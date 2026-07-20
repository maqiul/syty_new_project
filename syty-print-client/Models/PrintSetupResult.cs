using System.Collections.Generic;

namespace SytyPrintClient.Models;

/// <summary>
/// 后台下发的打印配置包
/// </summary>
public class PrintSetupResult
{
    /// <summary>打印机规则映射：逻辑名 -> 物理打印机名</summary>
    public Dictionary<string, string> PrinterMappings { get; set; } = new();

    /// <summary>模板路径映射：任务类型 -> 模板文件名/URL</summary>
    public Dictionary<string, string> TemplatePaths { get; set; } = new();

    /// <summary>静态图片资源（Base64 编码）：如收款码、小程序码</summary>
    public Dictionary<string, string> Images { get; set; } = new();
}
