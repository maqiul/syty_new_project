using Newtonsoft.Json;

namespace SytyPrintClient.Models;

/// <summary>
/// 店铺信息响应（GET /api/auth/shop-info）
/// </summary>
public class ShopInfoResult
{
    [JsonProperty("shopId")]
    public int ShopId { get; set; }

    [JsonProperty("shopName")]
    public string ShopName { get; set; } = "";
}
