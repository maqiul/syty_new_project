using System.Windows;
using System.Windows.Input;
using System.Drawing.Printing;
using System.Linq;
using System.Collections.Generic;
using HandyControl.Controls;
using SytyPrintClient.Models;
using SytyPrintClient.Helpers;
using System.Threading.Tasks;
using System.Collections.ObjectModel;

namespace SytyPrintClient.Views;

/// <summary>
/// 连接配置弹窗窗口（标准 WPF Window，不依赖 HandyControl Dialog）
/// </summary>
public partial class SettingsWindow : System.Windows.Window
{
    private string? _currentShopId;
    private string? _currentToken;
    public ObservableCollection<PrinterOption> PrinterOptions { get; set; } = new();

    public SettingsWindow(string? shopId = null, string? token = null)
    {
        InitializeComponent();
        _currentShopId = shopId;
        _currentToken = token;
        DataContext = this; // 绑定数据上下文
        LoadConfig();
        LoadLocalPrinters();
    }

    private void LoadConfig()
    {
        var config = AppConfig.Load();
        TxtBackendUrl.Text = config.BackendUrl;
        TxtTenantCode.Text = config.TenantCode;
        TxtUsername.Text = config.Username;
        TxtPassword.Password = config.Password;
        CbAutoStart.IsChecked = config.AutoStart;
    }

    /// <summary>
    /// 加载本机已安装的打印机 (绑定模式)
    /// </summary>
    private void LoadLocalPrinters()
    {
        var printers = PrinterSettings.InstalledPrinters.Cast<string>().OrderBy(p => p).ToList();
        var config = AppConfig.Load();
        var registered = config.RegisteredPrinters ?? new List<string>();
        
        PrinterOptions.Clear();
        foreach (var printerName in printers)
        {
            PrinterOptions.Add(new PrinterOption 
            { 
                Name = printerName, 
                IsSelected = registered.Contains(printerName) 
            });
        }
    }

    /// <summary>
    /// 保存配置
    /// </summary>
    private async void BtnSave_Click(object sender, RoutedEventArgs e)
    {
        var backendUrl = TxtBackendUrl.Text.Trim();
        var tenantCode = TxtTenantCode.Text.Trim();
        var username = TxtUsername.Text.Trim();
        var password = TxtPassword.Password;

        if (string.IsNullOrEmpty(backendUrl) || string.IsNullOrEmpty(tenantCode) || 
            string.IsNullOrEmpty(username) || string.IsNullOrEmpty(password))
        {
            Growl.Error("请填写完整的连接信息");
            return;
        }

        // 构建配置对象
        var config = new AppConfig
        {
            BackendUrl = backendUrl,
            TenantCode = tenantCode,
            Username = username,
            Password = password
        };

        var oldConfig = AppConfig.Load();
        config.Token = oldConfig.Token;
        config.ShopId = oldConfig.ShopId;
        config.ShopName = oldConfig.ShopName;
        config.AutoStart = CbAutoStart.IsChecked == true;
        config.MqttBroker = oldConfig.MqttBroker;
        config.MqttPort = oldConfig.MqttPort;
        config.PrinterName = oldConfig.PrinterName;
        config.EnablePreview = oldConfig.EnablePreview;

        // 保存基础配置
        config.Save();

        // 1. 保存已选中的打印机列表 (从数据源读取)
        var selectedPrinters = PrinterOptions.Where(p => p.IsSelected).Select(p => p.Name).ToList();
        config.RegisteredPrinters = selectedPrinters;
        config.Save();

        // 2. 如果已登录且有 ShopId，尝试注册打印机
        if (!string.IsNullOrEmpty(_currentShopId) && selectedPrinters.Count > 0)
        {
            try
            {
                var machineId = MachineHelper.GetMachineId();
                var api = new Services.ApiService(backendUrl);
                api.Token = _currentToken; // 必须设置 Token 才能调用接口

                var success = await api.RegisterPrintersAsync(_currentShopId, machineId, selectedPrinters);
                if (success)
                {
                    HandyControl.Controls.Growl.Success("配置已保存，打印机注册成功！");
                }
                else
                {
                    HandyControl.Controls.Growl.Warning("配置已保存，但打印机注册失败（请检查网络）");
                }
            }
            catch (System.Exception ex)
            {
                HandyControl.Controls.Growl.Warning($"配置已保存，注册异常: {ex.Message}");
            }
        }
        else
        {
            HandyControl.Controls.Growl.Success("配置已保存");
        }
        
        this.Close();
    }

    /// <summary>
    /// 取消按钮
    /// </summary>
    private void BtnCancel_Click(object sender, RoutedEventArgs e)
    {
        this.Close();
    }

    /// <summary>
    /// 关闭按钮
    /// </summary>
    private void BtnClose_Click(object sender, RoutedEventArgs e)
    {
        this.Close();
    }

    /// <summary>
    /// 标题栏拖拽移动窗口
    /// </summary>
    private void TitleBar_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        this.DragMove();
    }
}
