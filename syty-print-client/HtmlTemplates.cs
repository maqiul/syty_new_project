namespace SytyPrintClient;

/// <summary>
/// HTML 模板常量
/// </summary>
internal static class HtmlTemplates
{
    public const string WelcomePreviewHtml =
        "<!DOCTYPE html>" +
        "<html lang=\"zh-CN\">" +
        "<head>" +
        "    <meta charset=\"UTF-8\">" +
        "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
        "    <title>三益穿线系统 - 打印预览</title>" +
        "    <style>" +
        "        body { font-family: \"Microsoft YaHei\", sans-serif; background: #f5f5f5; margin: 0; padding: 20px; }" +
        "        .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 30px; }" +
        "        .header { text-align: center; border-bottom: 2px solid #4361ee; padding-bottom: 16px; margin-bottom: 20px; }" +
        "        .header h1 { margin: 0; color: #1a1a2e; font-size: 24px; }" +
        "        .header p { margin: 4px 0 0; color: #999; font-size: 13px; }" +
        "        .info-row { display: flex; padding: 8px 0; border-bottom: 1px dashed #eee; }" +
        "        .info-label { width: 100px; color: #999; font-size: 14px; }" +
        "        .info-value { flex: 1; color: #333; font-size: 14px; font-weight: bold; }" +
        "        .footer { text-align: center; margin-top: 24px; padding-top: 16px; border-top: 2px solid #4361ee; color: #999; font-size: 12px; }" +
        "    </style>" +
        "</head>" +
        "<body>" +
        "    <div class=\"container\">" +
        "        <div class=\"header\">" +
        "            <h1>&#x1F3F8; 三益穿线系统</h1>" +
        "            <p>穿线单 - 打印预览</p>" +
        "        </div>" +
        "        <div class=\"info-row\"><span class=\"info-label\">订单号</span><span class=\"info-value\">SY20260430001</span></div>" +
        "        <div class=\"info-row\"><span class=\"info-label\">球  馆</span><span class=\"info-value\">测试球馆</span></div>" +
        "        <div class=\"info-row\"><span class=\"info-label\">球  员</span><span class=\"info-value\">张三</span></div>" +
        "        <div class=\"info-row\"><span class=\"info-label\">电  话</span><span class=\"info-value\">138****1234</span></div>" +
        "        <div class=\"info-row\"><span class=\"info-label\">球  拍</span><span class=\"info-value\">YY100ZZ</span></div>" +
        "        <div class=\"info-row\"><span class=\"info-label\">主  线</span><span class=\"info-value\">BG65</span></div>" +
        "        <div class=\"info-row\"><span class=\"info-label\">主线磅数</span><span class=\"info-value\">28 lbs</span></div>" +
        "        <div class=\"info-row\"><span class=\"info-label\">横  线</span><span class=\"info-value\">BG65</span></div>" +
        "        <div class=\"info-row\"><span class=\"info-label\">横线磅数</span><span class=\"info-value\">26 lbs</span></div>" +
        "        <div class=\"info-row\"><span class=\"info-label\">备  注</span><span class=\"info-value\" style=\"color:#ff6b6b\">加急</span></div>" +
        "        <div class=\"footer\">" +
        "            <p>下单时间：2026-04-30 14:30:00</p>" +
        "            <p>请仔细核对信息后再打印</p>" +
        "        </div>" +
        "    </div>" +
        "</body>" +
        "</html>";

    public const string SamplePreviewHtml =
        "<!DOCTYPE html>" +
        "<html lang=\"zh-CN\">" +
        "<head>" +
        "    <meta charset=\"UTF-8\">" +
        "    <title>模板预览</title>" +
        "    <style>" +
        "        body { font-family: \"Microsoft YaHei\", sans-serif; padding: 20px; }" +
        "        .card { border: 1px solid #ddd; border-radius: 8px; padding: 20px; max-width: 400px; margin: 0 auto; }" +
        "        h2 { color: #4361ee; border-bottom: 2px solid #4361ee; padding-bottom: 8px; }" +
        "    </style>" +
        "</head>" +
        "<body>" +
        "    <div class=\"card\">" +
        "        <h2>&#x1F4CB; 穿线单</h2>" +
        "        <p>从后端加载的打印模板将在这里预览。</p>" +
        "        <p>请先登录并点击\"预览打印模板\"按钮。</p>" +
        "    </div>" +
        "</body>" +
        "</html>";
}
