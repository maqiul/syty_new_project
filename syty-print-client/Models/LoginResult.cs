using Newtonsoft.Json;

namespace SytyPrintClient.Models;

/// <summary>
/// 登录响应
/// </summary>
public class LoginResult
{
    [JsonProperty("token")]
    public string Token { get; set; } = "";

    [JsonProperty("user")]
    public UserInfo? User { get; set; }
}

public class UserInfo
{
    [JsonProperty("id")]
    public long Id { get; set; }

    [JsonProperty("username")]
    public string Username { get; set; } = "";

    [JsonProperty("realName")]
    public string RealName { get; set; } = "";

    [JsonProperty("tenantId")]
    public long TenantId { get; set; }

    /// <summary>店铺ID（登录后返回）</summary>
    [JsonProperty("shopId")]
    public string? ShopId { get; set; }
}

/// <summary>
/// 通用 API 响应
/// </summary>
public class ApiResult<T>
{
    [JsonProperty("code")]
    public int Code { get; set; }

    [JsonProperty("msg")]
    public string Msg { get; set; } = "";

    [JsonProperty("data")]
    public T? Data { get; set; }

    public bool IsSuccess => Code == 200;
}

/// <summary>
/// 打印订单数据（从后端推送过来）
/// </summary>
public class PrintOrderData
{
    [JsonProperty("orderNo")]
    public string OrderNo { get; set; } = "";

    [JsonProperty("playerName")]
    public string PlayerName { get; set; } = "";

    [JsonProperty("playerPhone")]
    public string PlayerPhone { get; set; } = "";

    [JsonProperty("racketModel")]
    public string RacketModel { get; set; } = "";

    [JsonProperty("mainStringName")]
    public string MainStringName { get; set; } = "";

    [JsonProperty("mainTension")]
    public double? MainTension { get; set; }

    [JsonProperty("crossStringName")]
    public string CrossStringName { get; set; } = "";

    [JsonProperty("crossTension")]
    public double? CrossTension { get; set; }

    [JsonProperty("totalPrice")]
    public decimal? TotalPrice { get; set; }

    [JsonProperty("remark")]
    public string Remark { get; set; } = "";

    [JsonProperty("shopName")]
    public string ShopName { get; set; } = "";

    [JsonProperty("status")]
    public int Status { get; set; }

    [JsonProperty("html")]
    public string? Html { get; set; }

    // ---- V1.1 新增字段 ----

    /// <summary>穿线师姓名</summary>
    [JsonProperty("stringerName")]
    public string? StringerName { get; set; }

    /// <summary>支付状态: PAID=已付, CREDIT=挂账, UNPAID=未付</summary>
    [JsonProperty("payStatus")]
    public string? PayStatus { get; set; }
}
