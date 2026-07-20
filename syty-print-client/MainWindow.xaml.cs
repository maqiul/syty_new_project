using System;
using System.ComponentModel;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using Microsoft.Web.WebView2.Core;
using SytyPrintClient.Models;
using SytyPrintClient.Services;
using SytyPrintClient.Views;
using SytyPrintClient.Helpers; // 🆕 引入 Helpers 命名空间
using HandyControl.Controls;

namespace SytyPrintClient;

public partial class MainWindow
{
    private ApiService? _api;
    private readonly MqttService _mqtt;
    private AppConfig _config = new();
    private CoreWebView2? _webView2;
    private bool _tokenValid;
    private PrintOrderData? _lastOrder;
    private bool _isConnecting;  // 防止 StartConnectionAsync 重入
    private bool _isConnected;   // 连接成功标志
    private System.Threading.Timer? _heartbeatTimer; // 心跳定时器

    // 系统托盘
    private System.Windows.Forms.NotifyIcon? _notifyIcon;
    private bool _isExiting;

    public MainWindow()
    {
        InitializeComponent();

        // 1. 加载配置
        _config = AppConfig.Load();

        // 2. 创建 MQTT 服务
        _mqtt = new MqttService();

        // 3. 设置 UI
        if (LogListBox != null)
        {
            LogListBox.ItemsSource = LogService.Instance.Logs;
        }
        if (StatusBarTime != null)
        {
            var timer = new System.Windows.Threading.DispatcherTimer
            {
                Interval = TimeSpan.FromSeconds(1)
            };
            timer.Tick += (s, e) => { try { StatusBarTime.Text = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"); } catch { } };
            timer.Start();
        }

        UpdateShopDisplay();

        // MQTT 事件
        _mqtt.OnMessageReceived += OnPrintDataReceived;
        _mqtt.OnConnectionChanged += OnConnectionChanged;
        _mqtt.OnShopIdReceived += OnShopIdReceived;

        // 初始化 WebView2
        _ = InitWebView2Async();

        Loaded += async (s, e) =>
        {
            LogService.Instance.Info("系统", $"客户端已启动，后端: {_config.BackendUrl}");
            InitNotifyIcon();

            // 只有在勾选了"启动自动连接"且有保存的用户名时，才尝试自动登录和连接
            if (_config.AutoStart && !string.IsNullOrEmpty(_config.Username))
            {
                if (!string.IsNullOrEmpty(_config.Password))
                {
                    LogService.Instance.Info("自动登录", "正在使用已保存的凭证获取Token...");
                    await TryLoginAsync(_config.Username, _config.Password);
                }

                // 自动连接
                if (_tokenValid)
                {
                    LogService.Instance.Info("自动启动", "正在自动连接...");
                    _ = StartConnectionAsync();
                }
            }
        };

        Closing += OnClosing;
    }

    #region 系统托盘

    private void InitNotifyIcon()
    {
        try
        {
            System.Drawing.Icon? icon = null;
            try
            {
                var exePath = System.Reflection.Assembly.GetExecutingAssembly().Location;
                icon = System.Drawing.Icon.ExtractAssociatedIcon(exePath);
            }
            catch { }

            if (icon == null)
            {
                using var bmp = new System.Drawing.Bitmap(16, 16);
                using var g = System.Drawing.Graphics.FromImage(bmp);
                g.Clear(System.Drawing.Color.Transparent);
                using var brush = new System.Drawing.SolidBrush(System.Drawing.Color.FromArgb(100, 149, 237));
                g.FillEllipse(brush, 0, 0, 15, 15);
                icon = System.Drawing.Icon.FromHandle(bmp.GetHicon());
            }

            _notifyIcon = new System.Windows.Forms.NotifyIcon
            {
                Icon = icon,
                Text = "穿线助手",
                Visible = true
            };

            _notifyIcon.Click += (s, e) => ToggleWindowVisibility();
            _notifyIcon.MouseDoubleClick += (s, e) =>
            {
                if (e.Button == System.Windows.Forms.MouseButtons.Left)
                    RestoreWindow();
            };

            var contextMenu = new System.Windows.Forms.ContextMenuStrip();
            
            // 连接
            var connectItem = new System.Windows.Forms.ToolStripMenuItem("▶ 连接");
            connectItem.Click += (s, e) =>
            {
                _ = Dispatcher.InvokeAsync(async () =>
                {
                    if (_mqtt.IsConnected)
                    {
                        HandyControl.Controls.Growl.Info("已经处于连接状态");
                        return;
                    }

                    // 只要有账号密码，就尝试连接（StartConnectionAsync 内部包含登录逻辑）
                    // 不再强求 _tokenValid，因为那是连接后的结果
                    if (!string.IsNullOrEmpty(_config.Username) && !string.IsNullOrEmpty(_config.Password))
                    {
                        LogService.Instance.Info("托盘", "开始连接...");
                        await StartConnectionAsync();
                    }
                    else
                    {
                        HandyControl.Controls.Growl.Warning("请先输入账号密码！");
                        OpenSettingsDialog();
                    }
                });
            };
            contextMenu.Items.Add(connectItem);

            // 停止
            var stopItem = new System.Windows.Forms.ToolStripMenuItem("⏹ 停止");
            stopItem.Click += (s, e) =>
            {
                _ = Dispatcher.InvokeAsync(async () =>
                {
                    LogService.Instance.Info("托盘", "正在断开连接...");
                    await StopConnectionAsync();
                });
            };
            contextMenu.Items.Add(stopItem);

            // 增加一个状态显示 Label (放在菜单最顶部)
            var statusLabel = new System.Windows.Forms.ToolStripLabel("⚪ 状态: 初始化...")
            {
                Enabled = false, // 纯展示，不可点
                Font = new System.Drawing.Font("Microsoft YaHei UI", 9, System.Drawing.FontStyle.Bold)
            };
            // 插入到最前面
            contextMenu.Items.Insert(0, statusLabel);

            // 设置菜单打开事件：刷新状态
            contextMenu.Opening += (s, e) =>
            {
                // 1. 更新状态文字
                statusLabel.Text = _mqtt.IsConnected ? "🟢 状态: 已连接" : "⚪ 状态: 未连接";
                statusLabel.ForeColor = _mqtt.IsConnected ? System.Drawing.Color.Green : System.Drawing.Color.Gray;

                // 2. 按钮互斥
                connectItem.Enabled = !_mqtt.IsConnected;
                stopItem.Enabled = _mqtt.IsConnected;
            };

            // 设置
            var settingsItem = new System.Windows.Forms.ToolStripMenuItem("⚙️ 设置");
            settingsItem.Click += (s, e) => OpenSettingsDialog();
            contextMenu.Items.Add(settingsItem);

            // 分割线
            contextMenu.Items.Add(new System.Windows.Forms.ToolStripSeparator());

            // 显示主窗口
            var showItem = new System.Windows.Forms.ToolStripMenuItem("🔍 显示主窗口");
            showItem.Click += (s, e) => RestoreWindow();
            contextMenu.Items.Add(showItem);

            // 退出
            var exitItem = new System.Windows.Forms.ToolStripMenuItem("❌ 退出");
            exitItem.Click += (s, e) => ExitApplication();
            contextMenu.Items.Add(exitItem);

            _notifyIcon.ContextMenuStrip = contextMenu;
        }
        catch (Exception ex)
        {
            LogService.Instance.Error("托盘", $"托盘图标初始化失败: {ex.Message}");
        }
    }

    private void ToggleWindowVisibility()
    {
        if (Visibility == Visibility.Visible)
            Hide();
        else
            RestoreWindow();
    }

    private void RestoreWindow()
    {
        Show();
        WindowState = WindowState.Normal;
        Activate();
    }

    private void ExitApplication()
    {
        _isExiting = true;
        _notifyIcon?.Dispose();
        Close();
    }

    #endregion

    #region 自定义标题栏

    private void TitleBar_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        if (e.ClickCount == 2)
        {
            WindowState = WindowState == WindowState.Maximized ? WindowState.Normal : WindowState.Maximized;
        }
        else
        {
            try { this.DragMove(); } catch { }
        }
    }

    private void BtnMinimize_Click(object sender, MouseButtonEventArgs e) => WindowState = WindowState.Minimized;
    private void BtnClose_Click(object sender, MouseButtonEventArgs e) => Hide();

    #endregion

    #region WebView2

    private async Task InitWebView2Async()
    {
        try
        {
            var env = await CoreWebView2Environment.CreateAsync();
            var webView = new Microsoft.Web.WebView2.Wpf.WebView2();

            WebViewContainer.Children.Clear();
            WebViewContainer.Children.Add(webView);
            await webView.EnsureCoreWebView2Async();

            _webView2 = webView.CoreWebView2;
            _webView2.Settings.IsScriptEnabled = true;

            webView.NavigateToString(HtmlTemplates.WelcomePreviewHtml);
            LogService.Instance.Info("系统", "WebView2 初始化完成");
        }
        catch (Exception ex)
        {
            LogService.Instance.Error("系统", $"WebView2初始化失败: {ex.Message}");
            WebViewContainer.Children.Clear();
            WebViewContainer.Children.Add(new TextBlock
            {
                Text = $"WebView2未能加载: {ex.Message}\n请确保已安装 Microsoft Edge WebView2 Runtime",
                Foreground = System.Windows.Media.Brushes.Red,
                FontSize = 14,
                TextWrapping = TextWrapping.Wrap,
                Margin = new Thickness(10),
                VerticalAlignment = VerticalAlignment.Center
            });
        }
    }

    #endregion

    #region 登录

    private async Task TryLoginAsync(string username, string password)
    {
        try
        {
            var tenantCode = _config.TenantCode;
            if (string.IsNullOrEmpty(tenantCode))
            {
                LogService.Instance.Warn("自动登录", "未配置租户编码，跳过自动登录");
                return;
            }

            _api = new ApiService(_config.BackendUrl);
            var result = await _api.LoginAsync(tenantCode, username, password);
            if (result != null)
            {
                try
                {
                    var shopInfo = await _api.GetShopInfoAsync(result.Token);
                    _config.ShopId = shopInfo.ShopId.ToString();
                    _config.ShopName = shopInfo.ShopName;
                }
                catch (UnauthorizedShopException ex)
                {
                    LogService.Instance.Warn("店铺", "非店员账号，跳过自动登录");
                    _tokenValid = false;
                    HandyControl.Controls.Growl.Error($"非店员账号: {ex.Message}");
                    HandyControl.Controls.Growl.Warning("非店员账号，无法自动登录");
                    return;
                }
                catch (Exception ex)
                {
                    LogService.Instance.Warn("店铺", $"获取店铺信息失败: {ex.Message}");
                    _config.ShopId = result.User?.ShopId ?? _config.ShopId;
                }

                _tokenValid = true;
                _config.Username = username;
                _config.Password = password;
                _config.Token = result.Token;
                _config.Save();

                UpdateShopDisplay();

                var displayName = !string.IsNullOrEmpty(result.User?.RealName)
                    ? result.User.RealName
                    : result.User?.Username ?? username;
                var shopDisplay = !string.IsNullOrEmpty(_config.ShopName)
                    ? $" ({_config.ShopName})"
                    : (!string.IsNullOrEmpty(_config.ShopId) ? $" (店铺 {_config.ShopId})" : "");

                LogService.Instance.Success("登录", $"Token获取成功：{displayName}{shopDisplay}");
            }
        }
        catch (Exception ex)
        {
            _tokenValid = false;
            HandyControl.Controls.Growl.Error($"登录失败: {ex.Message}");
            LogService.Instance.Error("登录", ex.Message);
        }
    }

    #endregion

    #region 设置弹窗

    /// <summary>
    /// 打开连接配置弹窗（使用标准 WPF Window，避免 HandyControl Dialog 问题）
    /// </summary>
    private void OpenSettingsDialog()
    {
        var win = new SettingsWindow(
            shopId: _config.ShopId, 
            token: _config.Token
        )
        {
            Owner = this
        };
        win.ShowDialog(); // 改为阻塞弹窗，避免后台逻辑冲突
    }

    /// <summary>
    /// 顶栏设置按钮：打开连接配置弹窗
    /// </summary>
    private void BtnSettings_Click(object sender, MouseButtonEventArgs e)
    {
        OpenSettingsDialog();
    }

    #endregion

    #region 连接控制

    /// <summary>
    /// 启动连接：Login -> GetShopInfo -> Start MQTT -> Subscribe
    /// </summary>
    private async Task StartConnectionAsync()
    {
        if (_isConnecting || _isConnected) return;

        _config = AppConfig.Load();
        // 1. 前置校验：没账号密码直接报错，不改变状态
        if (string.IsNullOrEmpty(_config.Username) || string.IsNullOrEmpty(_config.Password))
        {
            HandyControl.Controls.Growl.Error("请先设置账号密码！");
            UpdateConnectionButton(false); // 确保按钮是绿色可点的
            return;
        }

        _isConnecting = true;
        UpdateConnectionButton(true); // 按钮变灰 (连接中)
        UpdateStatus("正在连接...", "#1890ff");

        try
        {
            LogService.Instance.Info("系统", "=== 开始连接流程 ===");

            // Step 1: HTTP Login
            LogService.Instance.Info("探针", "1. 准备登录...");
            _api = new ApiService(_config.BackendUrl);
            var loginResult = await _api.LoginAsync(_config.TenantCode, _config.Username, _config.Password);
            LogService.Instance.Info("探针", "1. 登录完成 ✅");
            if (loginResult?.Token == null) throw new Exception("登录失败，未获取 Token");

            _config.Token = loginResult.Token;
            _api.Token = loginResult.Token;
            LogService.Instance.Success("Auth", "登录成功");

            // Step 2: Shop Info
            LogService.Instance.Info("探针", "2. 准备获取店铺信息...");
            var shopInfo = await _api.GetShopInfoAsync(_config.Token);
            LogService.Instance.Info("探针", "2. 店铺获取完成 ✅");
            _config.ShopId = shopInfo.ShopId.ToString();
            _config.ShopName = shopInfo.ShopName;
            _config.Save(); // 保存新数据
            UpdateShopDisplay();
            LogService.Instance.Success("Shop", $"已绑定: {shopInfo.ShopName}");

            // Step 3: MQTT Connect
            LogService.Instance.Info("探针", $"3. 准备连 MQTT ({_config.MqttBroker}:{_config.MqttPort})...");
            await _mqtt.StartAsync(_config.MqttBroker, _config.MqttPort,
                clientId: $"shop_{_config.ShopId}",
                username: _config.Username,
                password: _config.Token);
            LogService.Instance.Info("探针", "3. MQTT 连接完成 ✅");

            // Step 4: Apply ShopId (订阅 + 发送 ONLINE)
            LogService.Instance.Info("探针", $"4. 准备订阅店铺 {_config.ShopId}...");
            await _mqtt.ApplyShopId(_config.ShopId);
            LogService.Instance.Info("探针", "4. 订阅完成 ✅");

            // Step 5: 上报本地打印机列表 (读取设置里的勾选状态)
            try
            {
                // 🆕 检查 ShopId 是否有效
                if (string.IsNullOrEmpty(_config.ShopId))
                {
                    LogService.Instance.Warning("[配置] 跳过上报：当前未绑定店铺信息");
                }
                else
                {
                    var machineId = Helpers.MachineHelper.GetMachineId();
                    
                    // 1. 获取用户在设置里勾选的名单 (白名单)
                    var savedConfig = AppConfig.Load();
                    var whiteList = savedConfig.RegisteredPrinters ?? new System.Collections.Generic.List<string>();
                    
                    // 2. 获取本机实际安装的打印机
                    var allLocalPrinters = new System.Collections.Generic.List<string>();
                    foreach (string printer in System.Drawing.Printing.PrinterSettings.InstalledPrinters)
                    {
                        allLocalPrinters.Add(printer);
                    }

                    // 3. 过滤逻辑：如果用户有勾选，只上报勾选且在本地存在的；如果没勾选，上报所有
                    System.Collections.Generic.List<string> toReport;
                    if (whiteList.Count > 0)
                    {
                        toReport = allLocalPrinters.Where(p => whiteList.Contains(p)).ToList();
                        LogService.Instance.Info("配置", $"已启用打印机勾选过滤，准备上报 {toReport.Count} 个");
                    }
                    else
                    {
                        toReport = allLocalPrinters;
                        LogService.Instance.Info("配置", $"未勾选过滤，准备上报所有打印机 ({toReport.Count} 个)");
                    }

                    // 4. 上报
                    if (toReport.Any())
                    {
                        LogService.Instance.Info("配置", "正在向服务器注册打印机...");
                        bool success = await _api.RegisterPrintersAsync(_config.ShopId, machineId, toReport);
                        if (success)
                        {
                            LogService.Instance.Success("配置", "打印机上报成功");
                        }
                        else
                        {
                            LogService.Instance.Warning("[配置] 打印机上报失败，请检查上方红色日志获取详情");
                        }
                    }
                    else
                    {
                        LogService.Instance.Warning("配置没有可上报的打印机 (请在设置中勾选或检查本机驱动)");
                    }
                }
            }
            catch (Exception ex)
            {
                LogService.Instance.Warning($"上报打印机异常: {ex.Message}");
            }

            // Step 6: 同步打印配置 (拉取云端规则)
            try
            {
                var machineId = Helpers.MachineHelper.GetMachineId();
                LogService.Instance.Info("配置", $"机器码: {machineId}");
                var printSetup = await _api.GetPrintSetupAsync(_config.ShopId, machineId);
                
                if (printSetup != null)
                {
                    LogService.Instance.Success("配置", "打印规则已同步");
                    // 缓存规则到全局或 AppConfig (此处先打印日志，后续实现图片下载)
                    var rules = printSetup.PrinterMappings.Count;
                    LogService.Instance.Info("配置", $"同步了 {rules} 条打印机映射");
                }
            }
            catch (Exception ex)
            {
                LogService.Instance.Warning($"同步配置失败: {ex.Message}");
            }

            _isConnected = true;
            StartHeartbeat(); // 🆕 启动心跳
            UpdateConnectionButton(true); // 成功后按钮变红 (停止)
            UpdateStatus($"已连接: {_config.ShopName}", "#52c41a");
            LogService.Instance.Info("探针", "5. 全部完成！✅");
            LogService.Instance.Success("System", "✅ 连接成功！");
        }
        catch (Exception ex)
        {
            HandyControl.Controls.Growl.Error("连接失败: " + ex.Message);
            LogService.Instance.Error("连接失败", ex.Message);
            UpdateStatus("连接失败", "#ff4d4f");
        }
        finally
        {
            // !!! 核心修复：无论成功失败，必须解锁 !!!
            _isConnecting = false;
            if (!_isConnected)
            {
                UpdateConnectionButton(false); // 失败则变回绿色 (连接)
            }
        }
    }

    /// <summary>
    /// 断开连接 - 暴力中断
    /// </summary>
    private async Task StopConnectionAsync()
    {
        // 如果已经在停止中，别重复调
        if (!_isConnected && !_isConnecting) return;

        try
        {
            LogService.Instance.Info("System", "用户请求断开...");
            UpdateConnectionButton(true); // 按钮变灰，禁止点击

            // !!! 核心修复：这里绝对不能阻塞 !!!
            // 必须直接 await，不要用 .Wait() 或 .Result
            await _mqtt.StopAsync(); 
            
            // 强制重置状态
            _isConnected = false;
            _isConnecting = false;
            
            UpdateConnectionButton(false); // 按钮变回绿色"连接"
            UpdateStatus("未连接", "#999");
            LogService.Instance.Info("System", "已断开");
        }
        catch (Exception ex)
        {
            LogService.Instance.Error("Stop Error", ex.Message);
            _isConnected = false;
            _isConnecting = false;
            UpdateConnectionButton(false);
        }
    }

    /// <summary>
    /// 连接/停止 切换按钮点击事件
    /// </summary>
    private async void BtnToggleConnection_Click(object sender, RoutedEventArgs e)
    {
        if (_mqtt.IsConnected)
        {
            await StopConnectionAsync();
        }
        else
        {
            await StartConnectionAsync();
        }
    }

    /// <summary>
    /// 更新连接按钮状态
    /// </summary>
    private void UpdateConnectionButton(bool isConnected)
    {
        if (BtnToggleConnection == null) return;

        Dispatcher.Invoke(() =>
        {
            if (isConnected)
            {
                BtnToggleConnection.Content = "⏹ 停止";
                BtnToggleConnection.IsEnabled = true;
                BtnToggleConnection.Background = new SolidColorBrush(Color.FromRgb(0xff, 0x4d, 0x4f));
            }
            else
            {
                BtnToggleConnection.Content = "▶ 连接";
                BtnToggleConnection.IsEnabled = true;
                BtnToggleConnection.Background = new SolidColorBrush(Color.FromRgb(0x52, 0xc4, 0x1a));
            }
        });
    }

    #endregion

    #region 消息处理

    private void OnConnectionChanged(bool isConnected)
    {
        Dispatcher.Invoke(() =>
        {
            if (isConnected)
            {
                if (ConnectionStatusDot != null) ConnectionStatusDot.Fill = new SolidColorBrush(Colors.Green);
                if (TxtConnectionStatus != null) TxtConnectionStatus.Text = "已连接";
                UpdateConnectionButton(true);
                UpdateStatus("MQTT 已连接", "#52c41a");
            }
            else
            {
                if (ConnectionStatusDot != null) ConnectionStatusDot.Fill = new SolidColorBrush(Color.FromRgb(255, 152, 0));
                if (TxtConnectionStatus != null) TxtConnectionStatus.Text = "已断开";
                UpdateConnectionButton(false);
                UpdateStatus("MQTT 已断开", "#ffa940");
            }
        });
    }

    private void OnPrintDataReceived(string payload)
    {
        try
        {
            var order = PrintService.ParsePrintData(payload);
            if (order == null) return;

            // 仅保存订单数据并记录日志，用于调试。
            // 不在收到消息时自动弹窗或打印，避免打断用户操作。
            _lastOrder = order;

            LogService.Instance.Info("调试", $"收到 MQTT 订单: {order.OrderNo} - {order.PlayerName}");
        }
        catch (Exception ex)
        {
            LogService.Instance.Error("打印", $"处理消息异常: {ex.Message}");
        }
    }

    private void OnShopIdReceived(string shopId)
    {
        // 修复：移除内部的 _mqtt.ApplyShopId(shopId) 调用，防止与 MqttService 内部调用形成死循环
        // ApplyShopId 内部会自动触发此事件，此处只需更新 UI 显示即可。
        Dispatcher.Invoke(() =>
        {
            LogService.Instance.Info("MQTT", $"店铺 ID 已确认: {shopId}");
            _config.ShopId = shopId;
            UpdateShopDisplay();
            _config.Save();
        });
    }

    #endregion

    #region 打印 & 预览

    private async Task UpdatePreviewAsync(PrintOrderData order)
    {
        if (_webView2 == null) return;

        try
        {
            var html = order.Html;
            if (string.IsNullOrEmpty(html))
            {
                var mainLbs = order.MainTension.HasValue ? $"{order.MainTension} lbs" : "-";
                var crossLbs = order.CrossTension.HasValue ? $"{order.CrossTension} lbs" : "-";
                var remarkRow = string.IsNullOrEmpty(order.Remark)
                    ? ""
                    : $"<div class='row'><span class='label'>备  注</span><span class='value' style='color:#ff6b6b'>{order.Remark}</span></div>";

                html = $@"<!DOCTYPE html>
<html><head><meta charset='UTF-8'><style>
body{{font-family:'Microsoft YaHei',sans-serif;padding:20px;}}
.card{{border:1px solid #ddd;border-radius:8px;padding:20px;max-width:500px;margin:0 auto;}}
h2{{color:#4361ee;border-bottom:2px solid #4361ee;padding-bottom:8px;}}
.row{{display:flex;padding:8px 0;border-bottom:1px dashed #eee;}}
.label{{width:100px;color:#999;}}
.value{{flex:1;color:#333;font-weight:bold;}}
</style></head><body><div class='card'>
<h2>&#x1F3F8; 穿线单</h2>
<div class='row'><span class='label'>订单号</span><span class='value'>{order.OrderNo}</span></div>
<div class='row'><span class='label'>球  馆</span><span class='value'>{order.ShopName}</span></div>
<div class='row'><span class='label'>球  员</span><span class='value'>{order.PlayerName}</span></div>
<div class='row'><span class='label'>电  话</span><span class='value'>{order.PlayerPhone}</span></div>
<div class='row'><span class='label'>球  拍</span><span class='value'>{order.RacketModel}</span></div>
<div class='row'><span class='label'>主  线</span><span class='value'>{order.MainStringName}</span></div>
<div class='row'><span class='label'>主线磅数</span><span class='value'>{mainLbs}</span></div>
<div class='row'><span class='label'>横  线</span><span class='value'>{order.CrossStringName}</span></div>
<div class='row'><span class='label'>横线磅数</span><span class='value'>{crossLbs}</span></div>
{remarkRow}
</div></body></html>";
            }

            _webView2.NavigateToString(html);
        }
        catch (Exception ex)
        {
            LogService.Instance.Warn("预览", $"WebView2更新失败: {ex.Message}");
        }
    }

    private void PrintOrder(PrintOrderData order)
    {
        try
        {
            LogService.Instance.Info("打印", $"正在打印: {order.OrderNo}");
            var success = PrintService.ExecutePrint(order);
            if (success)
            {
                LogService.Instance.Success("打印", $"打印成功: {order.OrderNo}");
                HandyControl.Controls.Growl.Success($"打印成功: {order.OrderNo}");
            }
            else
            {
                LogService.Instance.Error("打印", $"打印失败: {order.OrderNo}");
                HandyControl.Controls.Growl.Error($"打印失败: {order.OrderNo}");
            }
        }
        catch (Exception ex)
        {
            LogService.Instance.Error("打印", $"打印异常: {ex.Message}");
            HandyControl.Controls.Growl.Error($"打印异常: {ex.Message}");
        }
    }

    private async void BtnRefreshPreview_Click(object sender, RoutedEventArgs e)
    {
        if (_lastOrder != null)
            await UpdatePreviewAsync(_lastOrder);
        else
            HandyControl.Controls.Growl.Warning("暂无打印数据");
    }

    private void BtnTemplatePreview_Click(object sender, RoutedEventArgs e)
    {
        if (_webView2 == null) return;
        _webView2.NavigateToString(HtmlTemplates.SamplePreviewHtml);
    }

    #endregion

    #region 工具方法

    private void UpdateShopDisplay()
    {
        if (TxtShopName != null)
        {
            TxtShopName.Text = !string.IsNullOrEmpty(_config.ShopName)
                ? $"🏪 {_config.ShopName}"
                : "🏪 未绑定店铺";
        }
        if (TxtShopId != null)
        {
            TxtShopId.Text = !string.IsNullOrEmpty(_config.ShopId)
                ? $"ID: {_config.ShopId}"
                : "";
        }
    }

    /// <summary>
    /// 更新底部状态栏的显示（小圆点 + 文字）
    /// </summary>
    private void UpdateStatus(string text, string color)
    {
        Dispatcher.Invoke(() =>
        {
            if (BottomStatusDot != null)
            {
                try
                {
                    BottomStatusDot.Fill = new System.Windows.Media.SolidColorBrush(
                        (System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString(color));
                }
                catch { }
            }
            if (StatusBarText != null)
            {
                StatusBarText.Text = text;
            }
        });
    }

    private void BtnClearLog_Click(object sender, RoutedEventArgs e)
    {
        LogService.Instance.Logs.Clear();
        LogService.Instance.Info("系统", "日志已清空");
    }

    /// <summary>
    /// 启动心跳定时器 (30秒一次)
    /// </summary>
    private void StartHeartbeat()
    {
        _heartbeatTimer?.Dispose();
        _heartbeatTimer = new System.Threading.Timer(async _ => await SendHeartbeatAsync(), null, 0, 30000); // 立即执行一次，然后每30秒
    }

    /// <summary>
    /// 发送心跳包
    /// </summary>
    private async Task SendHeartbeatAsync()
    {
        if (!_isConnected || _api == null || string.IsNullOrEmpty(_config.ShopId)) return;

        try
        {
            var machineId = Helpers.MachineHelper.GetMachineId();
            var statusMap = new Dictionary<string, string>();

            // 获取本机所有打印机状态
            foreach (string printerName in System.Drawing.Printing.PrinterSettings.InstalledPrinters)
            {
                // 使用 WMI 或 PrintQueue 获取状态比较复杂，这里简单判断：
                // 如果能创建 PrinterSettings 对象，通常认为是在线/可用的。
                // 更严谨的做法是检查 PrinterStatus，但受限于权限和驱动，简单判定即可。
                // 这里统一标记为 ONLINE，如果有缺纸等硬件错误，Windows 通常会报 Error。
                // 我们只取名字，状态暂时由连通性决定。
                statusMap[printerName] = "ONLINE";
            }

            await _api.SendPrinterHeartbeatAsync(_config.ShopId, machineId, statusMap);
            // 🆕 增加 UI 日志显示
            LogService.Instance.Info("心跳", $"💓 状态已同步 ({statusMap.Count} 台打印机)");
        }
        catch (Exception ex)
        {
            // 心跳失败通常不弹窗，只记日志
            LogService.Instance.Warning($"[心跳] 发送失败: {ex.Message}");
        }
    }

    #endregion

    #region 窗口关闭

    private async void OnClosing(object? sender, CancelEventArgs e)
    {
        if (!_isExiting)
        {
            e.Cancel = true;
            Hide();
            return;
        }

        try
        {
            await _mqtt.StopAsync();
            _notifyIcon?.Dispose();
        }
        catch { }
    }

    #endregion
}
