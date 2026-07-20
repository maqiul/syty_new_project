using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using FastReport;
using FastReport.Data;
using FastReport.Export.Html;
using FastReport.Export.Image;
using FastReport.Utils;
using Newtonsoft.Json.Linq;
using SytyPrintClient.Models;

namespace SytyPrintClient.Services;

/// <summary>
/// 打印服务 - 基于 FastReport 的打印处理
///
/// FastReport.OpenSource 限制：
/// - 不支持 Designer（.frx 设计器）
/// - 不支持内置预览窗口（Show）
/// - 不支持直接打印（Print）
/// - 支持：加载 .frx 模板、RegisterData、Prepare、导出 HTML/图片
///
/// 本服务采用"导出HTML → 浏览器预览/打印"的工作流
/// </summary>
public static class PrintService
{
    /// <summary>打印机名称（预留）</summary>
    private static string? _printerName;

    /// <summary>.frx 模板文件路径（留空则使用代码生成的内置模板）</summary>
    private static string? _templatePath;

    /// <summary>临时 HTML 文件路径（用于预览）</summary>
    private static string? _lastExportedHtml;

    public static void SetPrinter(string? name) => _printerName = name;
    public static void SetTemplatePath(string? path) => _templatePath = path;

    /// <summary>
    /// 解析接收到的打印数据，返回订单对象（供 UI 弹窗确认）
    /// </summary>
    public static PrintOrderData? ParsePrintData(string jsonData)
    {
        try
        {
            var data = JObject.Parse(jsonData);
            var type = data["type"]?.ToString();

            if (type != "PRINT_ORDER")
            {
                LogService.Instance.Warn("打印", $"未知消息类型: {type}");
                return null;
            }

            var order = new PrintOrderData
            {
                OrderNo = data["orderNo"]?.ToString() ?? "未知",
                ShopName = data["shopName"]?.ToString() ?? "",
                PlayerName = data["playerName"]?.ToString() ?? "未知",
                PlayerPhone = data["playerPhone"]?.ToString() ?? "",
                RacketModel = data["racketModel"]?.ToString() ?? "未知",
                MainStringName = data["mainStringName"]?.ToString() ?? "",
                MainTension = data["mainTension"]?.ToObject<double?>(),
                CrossStringName = data["crossStringName"]?.ToString() ?? "",
                CrossTension = data["crossTension"]?.ToObject<double?>(),
                Remark = data["remark"]?.ToString() ?? "",
                TotalPrice = data["totalPrice"]?.ToObject<decimal?>(),
                Html = data["html"]?.ToString(),
                // V1.1 新增字段
                StringerName = data["stringerName"]?.ToString(),
                PayStatus = data["payStatus"]?.ToString()
            };

            LogService.Instance.Success("打印",
                $"订单: {order.OrderNo} | 球馆: {order.ShopName} | 球员: {order.PlayerName} | 球拍: {order.RacketModel} | 磅数: {order.MainTension}");

            return order;
        }
        catch (Exception ex)
        {
            LogService.Instance.Error("打印", $"解析打印数据失败: {ex.Message}");
            return null;
        }
    }

    #region FastReport 渲染与预览

    /// <summary>
    /// 预览订单：使用 FastReport 生成 HTML，在系统默认浏览器中打开
    /// </summary>
    public static void PreviewOrder(PrintOrderData order)
    {
        try
        {
            // 1. 构建并准备报告
            using var report = BuildReport(order);
            report.Prepare();

            // 2. 导出到临时 HTML 文件
            var tempFile = Path.Combine(
                Path.GetTempPath(),
                $"syty_preview_{order.OrderNo}_{DateTime.Now:yyyyMMdd_HHmmss}.html");
            _lastExportedHtml = tempFile;

            using var htmlExport = new HTMLExport
            {
                SinglePage = true,
                EmbedPictures = true,
                Navigator = false,
                Layers = false
            };

            report.Export(htmlExport, tempFile);

            // 3. 在默认浏览器中打开
            var psi = new ProcessStartInfo
            {
                FileName = tempFile,
                UseShellExecute = true
            };
            Process.Start(psi);

            LogService.Instance.Info("打印",
                $"已打开打印预览 [{order.OrderNo}]，请在浏览器中 Ctrl+P 打印");
        }
        catch (Exception ex)
        {
            LogService.Instance.Error("打印", $"预览失败: {ex.Message}");
        }
    }

    /// <summary>
    /// 执行打印：生成 HTML 并发送到默认打印机
    /// </summary>
    public static bool ExecutePrint(PrintOrderData order)
    {
        try
        {
            // 1. 构建并准备报告
            using var report = BuildReport(order);
            report.Prepare();

            // 2. 导出到临时 HTML
            var tempFile = Path.Combine(
                Path.GetTempPath(),
                $"syty_print_{order.OrderNo}_{DateTime.Now:yyyyMMdd_HHmmss}.html");

            using var htmlExport = new HTMLExport
            {
                SinglePage = true,
                EmbedPictures = true,
                Navigator = false
            };

            report.Export(htmlExport, tempFile);

            // 3. 通过系统默认方式打印 HTML
            var psi = new ProcessStartInfo
            {
                FileName = tempFile,
                Verb = "print",           // 触发打印操作
                UseShellExecute = true,
                CreateNoWindow = true
            };

            var process = Process.Start(psi);
            process?.WaitForExit(3000);   // 最多等待 3 秒

            LogService.Instance.Success("打印", $"订单 [{order.OrderNo}] 已发送到打印机");
            return true;
        }
        catch (Exception ex)
        {
            LogService.Instance.Error("打印", $"打印失败: {ex.Message}");
            return false;
        }
    }

    /// <summary>
    /// 导出为 HTML 文件（用于存档）
    /// </summary>
    public static string? ExportToHtml(PrintOrderData order, string? outputPath = null)
    {
        try
        {
            outputPath ??= Path.Combine(
                AppDomain.CurrentDomain.BaseDirectory,
                "PrintArchive",
                $"订单_{order.OrderNo}_{DateTime.Now:yyyyMMdd_HHmmss}.html");

            var dir = Path.GetDirectoryName(outputPath);
            if (!string.IsNullOrEmpty(dir) && !Directory.Exists(dir))
                Directory.CreateDirectory(dir);

            using var report = BuildReport(order);
            report.Prepare();

            using var htmlExport = new HTMLExport
            {
                SinglePage = true,
                EmbedPictures = true,
                Navigator = false
            };

            report.Export(htmlExport, outputPath);
            LogService.Instance.Info("打印", $"HTML已导出: {outputPath}");
            return outputPath;
        }
        catch (Exception ex)
        {
            LogService.Instance.Error("打印", $"导出HTML失败: {ex.Message}");
            return null;
        }
    }

    /// <summary>
    /// 导出为图片（PNG），用于存档或嵌入其他文档
    /// </summary>
    public static string? ExportToImage(PrintOrderData order, string? outputPath = null)
    {
        try
        {
            outputPath ??= Path.Combine(
                AppDomain.CurrentDomain.BaseDirectory,
                "PrintArchive",
                $"订单_{order.OrderNo}_{DateTime.Now:yyyyMMdd_HHmmss}.png");

            var dir = Path.GetDirectoryName(outputPath);
            if (!string.IsNullOrEmpty(dir) && !Directory.Exists(dir))
                Directory.CreateDirectory(dir);

            using var report = BuildReport(order);
            report.Prepare();

            using var imageExport = new ImageExport
            {
                ImageFormat = ImageExportFormat.Png,
                Resolution = 200
            };

            report.Export(imageExport, outputPath);
            LogService.Instance.Info("打印", $"图片已导出: {outputPath}");
            return outputPath;
        }
        catch (Exception ex)
        {
            LogService.Instance.Error("打印", $"导出图片失败: {ex.Message}");
            return null;
        }
    }

    #endregion

    #region 报告构建

    /// <summary>
    /// 构建 FastReport 报告对象
    /// 优先加载 .frx 模板文件，如果没有则使用代码生成的内置模板
    /// </summary>
    private static Report BuildReport(PrintOrderData order)
    {
        var report = new Report();

        // 尝试加载外部 .frx 模板
        if (!string.IsNullOrEmpty(_templatePath) && File.Exists(_templatePath))
        {
            try
            {
                report.Load(_templatePath);
                LogService.Instance.Info("打印", $"已加载模板: {_templatePath}");
            }
            catch (Exception ex)
            {
                LogService.Instance.Warn("打印", $"模板加载失败，使用内置模板: {ex.Message}");
                report = BuildBuiltInTemplate();
            }
        }
        else
        {
            // 使用代码生成的内置模板
            report = BuildBuiltInTemplate();
        }

        // 注册订单数据（数据源名称: "Order"）
        report.RegisterData(new[] { order }, "Order");
        // 启用数据源
        var ds = report.GetDataSource("Order");
        if (ds != null) ds.Enabled = true;

        return report;
    }

    /// <summary>
    /// 用代码生成一个简单的穿线单模板（当没有 .frx 文件时使用）
    /// 页面宽度约 80mm，适合小票/穿线单打印
    /// </summary>
    private static Report BuildBuiltInTemplate()
    {
        var report = new Report();

        // 创建页面（80mm 宽，160mm 高 - 适合小票打印）
        var page = new ReportPage
        {
            Name = "Page1",
            PaperWidth = 80,
            PaperHeight = 160,
            LeftMargin = 5,
            RightMargin = 5,
            TopMargin = 5,
            BottomMargin = 5
        };
        report.Pages.Add(page);

        // ========== 标题带 ==========
        var titleBand = new ReportTitleBand
        {
            Name = "TitleBand1",
            Top = 0,
            Height = (float)(Units.Millimeters * 18)
        };
        page.Bands.Add(titleBand);

        // 主标题
        var titleText = new TextObject
        {
            Name = "TitleText",
            Top = 0,
            Left = 0,
            Width = (float)(Units.Millimeters * 70),
            Height = (float)(Units.Millimeters * 8),
            Text = "三益穿线系统 - 穿线单",
            HorzAlign = HorzAlign.Center,
            Font = new Font("微软雅黑", 14f, FontStyle.Bold)
        };
        titleBand.Objects.Add(titleText);

        // 打印时间
        var timeText = new TextObject
        {
            Name = "TimeText",
            Top = (float)(Units.Millimeters * 9),
            Left = 0,
            Width = (float)(Units.Millimeters * 70),
            Height = (float)(Units.Millimeters * 5),
            Text = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"),
            HorzAlign = HorzAlign.Center,
            Font = new Font("微软雅黑", 8f),
            TextColor = Color.Gray
        };
        titleBand.Objects.Add(timeText);

        // ========== 分割线 ==========
        var headerBand = new PageHeaderBand
        {
            Name = "HeaderBand",
            Top = (float)(Units.Millimeters * 19),
            Height = (float)(Units.Millimeters * 5)
        };
        page.Bands.Add(headerBand);

        var lineText = new TextObject
        {
            Name = "LineText",
            Top = 0,
            Left = 0,
            Width = (float)(Units.Millimeters * 70),
            Height = (float)(Units.Millimeters * 4),
            Text = "═══════════════════════",
            HorzAlign = HorzAlign.Center,
            Font = new Font("Consolas", 10f)
        };
        headerBand.Objects.Add(lineText);

        // ========== 数据带 ==========
        var dataBand = new DataBand
        {
            Name = "DataBand1",
            Top = (float)(Units.Millimeters * 25),
            Height = (float)(Units.Millimeters * 85),
            DataSource = report.GetDataSource("Order")
        };
        page.Bands.Add(dataBand);

        // 订单字段定义：(标签文本, 字段表达式, 行号)
        var fields = new[]
        {
            ("订单号",  "[Order.OrderNo]",       0),
            ("球  馆",  "[Order.ShopName]",      1),
            ("球  员",  "[Order.PlayerName]",    2),
            ("电  话",  "[Order.PlayerPhone]",   3),
            ("球  拍",  "[Order.RacketModel]",   4),
            ("主  线",  "[Order.MainStringName]", 5),
            ("主线磅数", "[Order.MainTension] lbs", 6),
            ("横  线",  "[Order.CrossStringName]", 7),
            ("横线磅数", "[Order.CrossTension] lbs", 8),
            ("总  价",  "¥[Order.TotalPrice]",   9),
            ("备  注",  "[Order.Remark]",        10),
            // V1.1 新增
            ("穿线师",  "[Order.StringerName]",  11)
        };

        float rowHeight = 6.5f;
        foreach (var (label, expr, row) in fields)
        {
            // 标签
            var lbl = new TextObject
            {
                Name = $"Lbl_{row}",
                Top = (float)(Units.Millimeters * (row * rowHeight)),
                Left = (float)(Units.Millimeters * 2),
                Width = (float)(Units.Millimeters * 20),
                Height = (float)(Units.Millimeters * 5.5),
                Text = label + ":",
                Font = new Font("微软雅黑", 9f, FontStyle.Bold)
            };
            dataBand.Objects.Add(lbl);

            // 数据值（绑定到 Order 数据源）
            var val = new TextObject
            {
                Name = $"Val_{row}",
                Top = (float)(Units.Millimeters * (row * rowHeight)),
                Left = (float)(Units.Millimeters * 23),
                Width = (float)(Units.Millimeters * 45),
                Height = (float)(Units.Millimeters * 5.5),
                Text = expr,
                Font = new Font("微软雅黑", 9f)
            };
            dataBand.Objects.Add(val);
        }

        // ========== 穿线师信息（有则显示） ==========
        // 注意：FastReport.OpenSource 的 TextObject 不支持 Condition 属性
        // 改用 IIf 表达式在 Text 属性中做条件判断
        var stringerText = new TextObject
        {
            Name = "StringerText",
            Top = (float)(Units.Millimeters * (10 * rowHeight)),
            Left = 0,
            Width = (float)(Units.Millimeters * 70),
            Height = (float)(Units.Millimeters * 5),
            Text = "[IIf([Order.StringerName] != \"\" AND [Order.StringerName] != Null, \"穿线师: \" + [Order.StringerName], \"\")]",
            HorzAlign = HorzAlign.Center,
            Font = new Font("微软雅黑", 9f, FontStyle.Italic),
            TextColor = Color.FromArgb(255, 100, 100, 100)
        };
        dataBand.Objects.Add(stringerText);

        // ========== 挂账标记（有则显示） ==========
        var creditMark = new TextObject
        {
            Name = "CreditMark",
            Top = (float)(Units.Millimeters * (11 * rowHeight)),
            Left = 0,
            Width = (float)(Units.Millimeters * 70),
            Height = (float)(Units.Millimeters * 6),
            Text = "[IIf([Order.PayStatus] == \"CREDIT\", \"★ 【签单挂账】 ★\", \"\")]",
            HorzAlign = HorzAlign.Center,
            Font = new Font("微软雅黑", 12f, FontStyle.Bold),
            TextColor = Color.FromArgb(255, 220, 53, 69)  // 红色醒目
        };
        dataBand.Objects.Add(creditMark);

        // ========== 页脚 ==========
        var footerBand = new PageFooterBand
        {
            Name = "FooterBand1",
            Top = (float)(Units.Millimeters * 110),
            Height = (float)(Units.Millimeters * 20)
        };
        page.Bands.Add(footerBand);

        var footerLine = new TextObject
        {
            Name = "FooterLine",
            Top = 0,
            Left = 0,
            Width = (float)(Units.Millimeters * 70),
            Height = (float)(Units.Millimeters * 4),
            Text = "═══════════════════════",
            HorzAlign = HorzAlign.Center,
            Font = new Font("Consolas", 10f)
        };
        footerBand.Objects.Add(footerLine);

        // V1.1 新增：穿线师签名行（有穿线师时显示）
        var stringerFooterText = new TextObject
        {
            Name = "StringerFooterText",
            Top = (float)(Units.Millimeters * 4),
            Left = 0,
            Width = (float)(Units.Millimeters * 70),
            Height = (float)(Units.Millimeters * 4),
            Text = "[IIf([Order.StringerName] != \"\" AND [Order.StringerName] != Null, \"穿线师: \" + [Order.StringerName], \"--- 感谢您的使用 ---\")]",
            HorzAlign = HorzAlign.Center,
            Font = new Font("微软雅黑", 9f, FontStyle.Bold)
        };
        footerBand.Objects.Add(stringerFooterText);

        var footerText = new TextObject
        {
            Name = "FooterText",
            Top = (float)(Units.Millimeters * 8),
            Left = 0,
            Width = (float)(Units.Millimeters * 70),
            Height = (float)(Units.Millimeters * 4),
            Text = "--- 感谢您的使用 ---",
            HorzAlign = HorzAlign.Center,
            Font = new Font("微软雅黑", 8f, FontStyle.Italic),
            TextColor = Color.Gray
        };
        footerBand.Objects.Add(footerText);

        return report;
    }

    #endregion
}
