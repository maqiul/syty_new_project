using System;
using System.IO;
using Newtonsoft.Json;

namespace SytyPrintClient.Models;

/// <summary>
/// 应用配置
/// </summary>
public class AppConfig
{
    /// <summary>后端API基础地址（HTTP）</summary>
    public string BackendUrl { get; set; } = "http://127.0.0.1:8080";

    /// <summary>租户编码（店铺编码）</summary>
    public string TenantCode { get; set; } = "";

    /// <summary>用户名(用于后端登录)</summary>
    public string Username { get; set; } = "";

    /// <summary>密码(用于后端登录)</summary>
    public string Password { get; set; } = "";

    /// <summary>登录后获取的 Token</summary>
    public string Token { get; set; } = "";

    /// <summary>登录后获取的 ShopId</summary>
    public string ShopId { get; set; } = "";

    /// <summary>登录后获取的店铺名称</summary>
    public string ShopName { get; set; } = "";

    /// <summary>是否自动启动</summary>
    public bool AutoStart { get; set; } = false;

    /// <summary>MQTT Broker地址</summary>
    public string MqttBroker { get; set; } = "127.0.0.1";

    /// <summary>MQTT端口</summary>
    public int MqttPort { get; set; } = 1883;

    /// <summary>打印机名称(留空用默认)</summary>
    public string PrinterName { get; set; } = "";

    /// <summary>已注册到后台的打印机名称列表</summary>
    public List<string> RegisteredPrinters { get; set; } = new List<string>();

    /// <summary>是否启用预览弹窗(收到打印消息时是否弹出预览)</summary>
    public bool EnablePreview { get; set; } = true;

    /// <summary>保存到文件</summary>
    public void Save(string path = "appconfig.json")
    {
        var json = JsonConvert.SerializeObject(this, Formatting.Indented);
        File.WriteAllText(path, json);
    }

    /// <summary>从文件加载</summary>
    public static AppConfig Load(string path = "appconfig.json")
    {
        if (File.Exists(path))
        {
            try
            {
                var json = File.ReadAllText(path);
                return JsonConvert.DeserializeObject<AppConfig>(json) ?? new AppConfig();
            }
            catch { }
        }
        return new AppConfig();
    }
}
