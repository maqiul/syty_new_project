using System.IO;
using Newtonsoft.Json;
using SytyPrintClient.Models;

namespace SytyPrintClient.Services;

/// <summary>
/// 配置管理服务（单例）
/// </summary>
public class ConfigService
{
    private static readonly Lazy<ConfigService> _instance = new(() => new ConfigService());
    public static ConfigService Instance => _instance.Value;

    private const string ConfigFileName = "config.json";

    public AppConfig Config { get; private set; }

    private ConfigService()
    {
        Config = LoadConfig();
    }

    private AppConfig LoadConfig()
    {
        var filePath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, ConfigFileName);
        if (!File.Exists(filePath))
        {
            LogService.Instance.Info("配置文件不存在，使用默认配置");
            return new AppConfig();
        }

        try
        {
            var json = File.ReadAllText(filePath);
            var config = JsonConvert.DeserializeObject<AppConfig>(json);
            if (config != null)
            {
                LogService.Instance.Info("配置文件加载成功");
                return config;
            }
        }
        catch (Exception ex)
        {
            LogService.Instance.Error($"配置文件加载失败: {ex.Message}");
        }

        return new AppConfig();
    }

    public void SaveConfig()
    {
        var filePath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, ConfigFileName);
        try
        {
            var json = JsonConvert.SerializeObject(Config, Formatting.Indented);
            File.WriteAllText(filePath, json);
            LogService.Instance.Success("配置已保存");
        }
        catch (Exception ex)
        {
            LogService.Instance.Error($"配置保存失败: {ex.Message}");
        }
    }
}
