using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using Newtonsoft.Json;
using SytyPrintClient.Models;

namespace SytyPrintClient.Services;

/// <summary>
/// 后端 API 服务 - 登录获取 Token、获取打印模板等
/// </summary>
public class ApiService
{
    private readonly HttpClient _httpClient;
    private string? _token;

    /// <summary>
    /// 当前 Token。设置时自动同步 Bearer 和 satoken 两种请求头，
    /// 兼容不同后端拦截器的实现方式。
    /// </summary>
    public string? Token
    {
        get => _token;
        set
        {
            _token = value;
            if (!string.IsNullOrEmpty(value))
            {
                // 标准 Bearer Token
                _httpClient.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", value);
                // Sa-Token 风格请求头（后端可能使用 satoken 作为 header 名）
                _httpClient.DefaultRequestHeaders.Remove("satoken");
                _httpClient.DefaultRequestHeaders.Add("satoken", value);
            }
            else
            {
                _httpClient.DefaultRequestHeaders.Authorization = null;
                _httpClient.DefaultRequestHeaders.Remove("satoken");
            }
        }
    }

    public ApiService(string baseUrl)
    {
        // BackendUrl 如 "http://127.0.0.1:8080"，需要附加 /api/ 前缀
        var apiBase = baseUrl.TrimEnd('/');
        if (!apiBase.EndsWith("/api", StringComparison.OrdinalIgnoreCase))
            apiBase += "/api";

        _httpClient = new HttpClient
        {
            BaseAddress = new Uri(apiBase + "/"),
            Timeout = TimeSpan.FromSeconds(15)
        };
    }

    /// <summary>
    /// 登录获取 Token（带租户编码）
    /// </summary>
    public async Task<LoginResult?> LoginAsync(string tenantCode, string username, string password)
    {
        var payload = new { tenantCode, username, password };
        var json = JsonConvert.SerializeObject(payload);
        var content = new StringContent(json, Encoding.UTF8, "application/json");

        var fullUrl = _httpClient.BaseAddress + "auth/login";
        LogService.Instance.Info("API", $"正在请求登录接口: {fullUrl}");

        var response = await _httpClient.PostAsync("auth/login", content);
        var body = await response.Content.ReadAsStringAsync();

        if (!response.IsSuccessStatusCode)
        {
            throw new Exception($"登录请求失败 ({(int)response.StatusCode}): {body}");
        }

        var result = JsonConvert.DeserializeObject<ApiResult<LoginResult>>(body);

        if (result?.IsSuccess == true && result.Data != null)
        {
            Token = result.Data.Token;
            LogService.Instance.Success("API", $"登录成功，Token 已获取");
            return result.Data;
        }

        throw new Exception(result?.Msg ?? "登录失败，未返回 Token");
    }

    /// <summary>
    /// 获取当前登录用户的店铺信息（仅店员账号可用）
    /// </summary>
    /// <param name="token">登录 Token</param>
    /// <returns>店铺 ID 和名称</returns>
    /// <exception cref="UnauthorizedShopException">非店员账号时抛出</exception>
    public async Task<ShopInfoResult> GetShopInfoAsync(string token)
    {
        // 确保 Token 已设置到请求头（同时支持 Bearer 和 satoken）
        Token = token;

        var fullUrl = _httpClient.BaseAddress + "auth/shop-info";
        LogService.Instance.Info("API", $"正在请求店铺信息接口: {fullUrl}");

        var response = await _httpClient.GetAsync("auth/shop-info");
        var body = await response.Content.ReadAsStringAsync();

        if (!response.IsSuccessStatusCode)
        {
            throw new Exception($"获取店铺信息失败 ({(int)response.StatusCode}): {body}");
        }

        var result = JsonConvert.DeserializeObject<ApiResult<ShopInfoResult>>(body);

        if (result == null || !result.IsSuccess || result.Data == null)
        {
            var errorMsg = result?.Msg ?? "获取店铺信息失败，未返回数据";

            // 检测是否为店员权限限制
            if (errorMsg.Contains("仅支持店员") || errorMsg.Contains("店员") || errorMsg.Contains("权限"))
            {
                throw new UnauthorizedShopException(errorMsg);
            }

            throw new Exception(errorMsg);
        }

        LogService.Instance.Success("API", $"获取店铺信息成功: {result.Data.ShopName} (ID: {result.Data.ShopId})");
        return result.Data;
    }

    /// <summary>
    /// 获取打印模板 HTML 预览
    /// </summary>
    public async Task<string?> GetPrintTemplatePreview(long shopId)
    {
        var response = await _httpClient.GetAsync($"print-template/preview?shopId={shopId}");
        var body = await response.Content.ReadAsStringAsync();
        var result = JsonConvert.DeserializeObject<ApiResult<string>>(body);
        return result?.IsSuccess == true ? result.Data : null;
    }

    /// <summary>
    /// 获取店铺列表（用于预览模板时选择店铺）
    /// </summary>
    public async Task<List<object>> GetShopListAsync()
    {
        var response = await _httpClient.GetAsync("shop/list");
        var body = await response.Content.ReadAsStringAsync();
        var result = JsonConvert.DeserializeObject<ApiResult<List<object>>>(body);
        return result?.Data ?? new List<object>();
    }

    /// <summary>
    /// 注册本地打印机到后台
    /// </summary>
    public async Task<bool> RegisterPrintersAsync(string shopId, string machineId, List<string> printerNames)
    {
        long shopIdLong;
        try
        {
            shopIdLong = long.Parse(shopId);
        }
        catch
        {
            LogService.Instance.Error("API", $"上报失败：ShopId 格式无效 ({shopId})");
            return false;
        }

        var payload = new { shopId = shopIdLong, machineId, printerNames };
        var json = JsonConvert.SerializeObject(payload);
        var content = new StringContent(json, Encoding.UTF8, "application/json");

        LogService.Instance.Info("API", $"正在注册打印机: ShopId={shopIdLong}");
        
        var response = await _httpClient.PostAsync("printer/register", content);
        var body = await response.Content.ReadAsStringAsync();

        // 🆕 只要 HTTP 状态是 200 就算成功 (避免后端返回 null 导致反序列化崩溃)
        if (response.IsSuccessStatusCode)
        {
            LogService.Instance.Info("API", "注册接口调用成功");
            return true;
        }

        // 失败则打印详情
        LogService.Instance.Error("API", $"注册失败 HTTP {(int)response.StatusCode}: {body}");
        return false;
    }

    /// <summary>
    /// 获取打印配置（包含规则、模板、静态图片等）
    /// </summary>
    public async Task<PrintSetupResult?> GetPrintSetupAsync(string shopId, string machineId)
    {
        var response = await _httpClient.GetAsync($"print-setup?shopId={shopId}&machineId={machineId}");
        var body = await response.Content.ReadAsStringAsync();
        var result = JsonConvert.DeserializeObject<ApiResult<PrintSetupResult>>(body);
        return result?.IsSuccess == true ? result.Data : null;
    }

    /// <summary>
    /// 发送打印机心跳包
    /// </summary>
    public async Task<bool> SendPrinterHeartbeatAsync(string shopId, string machineId, Dictionary<string, string> printerStatusMap)
    {
        long shopIdLong;
        try
        {
            shopIdLong = long.Parse(shopId);
        }
        catch
        {
            return false;
        }

        var payload = new { shopId = shopIdLong, machineId, printerStatusMap };
        var json = JsonConvert.SerializeObject(payload);
        var content = new StringContent(json, Encoding.UTF8, "application/json");

        var response = await _httpClient.PostAsync("printer/heartbeat", content);
        return response.IsSuccessStatusCode;
    }
}

/// <summary>
/// 非店员账号无法获取店铺信息的异常
/// </summary>
public class UnauthorizedShopException : Exception
{
    public UnauthorizedShopException(string message) : base(message) { }
}
