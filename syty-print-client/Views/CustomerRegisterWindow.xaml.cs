using System.Collections.ObjectModel;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using SytyPrintClient.Models;
using SytyPrintClient.Services;

namespace SytyPrintClient.Views;

/// <summary>
/// 接单/登记窗口 - V1.1 新增
/// 用于登记新订单，选择穿线师，记录支付方式
/// </summary>
public partial class CustomerRegisterWindow : Window
{
    /// <summary>穿线师信息（模拟数据，后续可改为 API 获取）</summary>
    private readonly ObservableCollection<StringerItem> _stringers = new();

    /// <summary>选中的穿线师</summary>
    public StringerItem? SelectedStringer { get; private set; }

    public CustomerRegisterWindow()
    {
        InitializeComponent();
        LoadMockStringers();
    }

    /// <summary>
    /// 加载穿线师列表（当前为模拟数据，后续对接 /api/employee/list）
    /// </summary>
    private void LoadMockStringers()
    {
        _stringers.Clear();
        _stringers.Add(new StringerItem { Id = 1, Name = "张三", IsAvailable = true });
        _stringers.Add(new StringerItem { Id = 2, Name = "李四", IsAvailable = true });
        _stringers.Add(new StringerItem { Id = 3, Name = "王五", IsAvailable = false });

        CbStringer.ItemsSource = _stringers;
        CbStringer.SelectedIndex = 0; // 默认选中"请选择"
    }

    #region 拖拽标题栏

    private void TitleBar_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        if (e.ChangedButton == MouseButton.Left)
        {
            DragMove();
        }
    }

    #endregion

    #region 事件处理

    private void BtnClose_Click(object sender, RoutedEventArgs e)
    {
        Close();
    }

    private void BtnCancel_Click(object sender, RoutedEventArgs e)
    {
        Close();
    }

    private void CbStringer_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (CbStringer.SelectedItem is StringerItem item)
        {
            SelectedStringer = item;
        }
    }

    /// <summary>磅数输入框只允许数字和小数点</summary>
    private void Tension_PreviewTextInput(object sender, TextCompositionEventArgs e)
    {
        var textBox = sender as TextBox;
        string newText = textBox != null
            ? textBox.Text.Insert(textBox.SelectionStart, e.Text)
            : e.Text;

        // 只允许数字和最多一位小数
        e.Handled = !Regex.IsMatch(newText, @"^\d*\.?\d{0,1}$");
    }

    /// <summary>确认登记</summary>
    private async void BtnSubmit_Click(object sender, RoutedEventArgs e)
    {
        // 基础校验
        if (string.IsNullOrWhiteSpace(TxtPlayerName.Text))
        {
            HandyControl.Controls.Growl.Warning("请输入球员姓名");
            return;
        }

        if (string.IsNullOrWhiteSpace(TxtRacketModel.Text))
        {
            HandyControl.Controls.Growl.Warning("请输入球拍型号");
            return;
        }

        if (SelectedStringer == null)
        {
            HandyControl.Controls.Growl.Warning("请选择穿线师");
            return;
        }

        // 构建订单数据
        var order = new PrintOrderData
        {
            OrderNo = GenerateOrderNo(),
            PlayerName = TxtPlayerName.Text.Trim(),
            PlayerPhone = TxtPlayerPhone.Text.Trim(),
            RacketModel = TxtRacketModel.Text.Trim(),
            MainStringName = TxtMainString.Text.Trim(),
            MainTension = double.TryParse(TxtMainTension.Text, out var mt) ? mt : null,
            CrossStringName = TxtCrossString.Text.Trim(),
            CrossTension = double.TryParse(TxtCrossTension.Text, out var ct) ? ct : null,
            Remark = TxtRemark.Text.Trim(),
            ShopName = AppConfig.Load().ShopName,
            StringerName = SelectedStringer.Name,
            PayStatus = CbPayStatus.SelectedIndex switch
            {
                0 => "PAID",
                1 => "CREDIT",
                2 => "UNPAID",
                _ => "UNPAID"
            }
        };

        LogService.Instance.Info("登记",
            $"新订单: {order.OrderNo} | 球员: {order.PlayerName} | 穿线师: {order.StringerName} | 状态: {order.PayStatus}");

        // 如果有预览配置，打开预览
        var config = AppConfig.Load();
        if (config.EnablePreview)
        {
            PrintService.PreviewOrder(order);
        }

        HandyControl.Controls.Growl.Success($"订单 [{order.OrderNo}] 已登记成功");
        DialogResult = true;
        Close();
    }

    /// <summary>生成订单号</summary>
    private static string GenerateOrderNo()
    {
        return $"SY{DateTime.Now:yyyyMMdd}{new Random().Next(1000, 9999)}";
    }

    #endregion
}

/// <summary>
/// 穿线师信息
/// </summary>
public class StringerItem
{
    public int Id { get; set; }
    public string Name { get; set; } = "";
    public bool IsAvailable { get; set; } = true;

    public override string ToString() => IsAvailable ? Name : $"{Name} (忙碌)";
}
