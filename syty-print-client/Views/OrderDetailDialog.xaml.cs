using System.Windows;
using SytyPrintClient.Models;
using SytyPrintClient.Services;
using HandyControl.Controls;

namespace SytyPrintClient.Views;

/// <summary>
/// 订单详情弹窗 - 使用 HandyControl Dialog.Show() 展示
/// </summary>
public partial class OrderDetailDialog
{
    private PrintOrderData? _order;
    private Dialog? _dialogInstance;

    public OrderDetailDialog()
    {
        InitializeComponent();
    }

    /// <summary>设置 Dialog 实例引用（用于关闭）</summary>
    public void SetDialogInstance(Dialog dialog)
    {
        _dialogInstance = dialog;
    }

    /// <summary>绑定订单数据到 UI</summary>
    public void BindOrder(PrintOrderData order)
    {
        _order = order;

        TxtOrderNo.Text = order.OrderNo;
        TxtShopName.Text = string.IsNullOrEmpty(order.ShopName) ? "-" : order.ShopName;
        TxtPlayerName.Text = order.PlayerName;
        TxtPlayerPhone.Text = string.IsNullOrEmpty(order.PlayerPhone) ? "-" : order.PlayerPhone;
        TxtRacketModel.Text = order.RacketModel;
        TxtMainString.Text = string.IsNullOrEmpty(order.MainStringName) ? "-" : order.MainStringName;
        TxtMainTension.Text = order.MainTension.HasValue ? $"{order.MainTension} lbs" : "-";
        TxtRemark.Text = string.IsNullOrEmpty(order.Remark) ? "-" : order.Remark;
    }

    /// <summary>确认打印按钮点击</summary>
    private void BtnPrint_Click(object sender, RoutedEventArgs e)
    {
        if (_order == null) return;

        bool success = PrintService.ExecutePrint(_order);
        if (success)
        {
            LogService.Instance.Success("打印", $"订单 [{_order.OrderNo}] 打印确认成功");
        }
        else
        {
            LogService.Instance.Error("打印", $"订单 [{_order.OrderNo}] 打印失败");
        }

        // 关闭弹窗
        _dialogInstance?.Close();
    }
}
