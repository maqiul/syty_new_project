namespace SytyPrintClient.Models;

/// <summary>
/// 字符串扩展方法
/// </summary>
public static class StringExtensions
{
    public static string Truncate(this string value, int maxLength)
        => string.IsNullOrEmpty(value) ? value :
           value.Length <= maxLength ? value :
           value[..maxLength] + "...";
}
