package com.syty.netty;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.syty.entity.*;
import com.syty.service.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;
@Slf4j
@Component
@ConditionalOnProperty(name = "print.method", havingValue = "netty", matchIfMissing = true)
@RequiredArgsConstructor
public class PrintDataSender implements PrintSender {
    private final ShopService shopService;
    private final PlayerService playerService;
    private final RacketService racketService;
    private final StringInfoService stringInfoService;
    private final PrintTemplateService printTemplateService;
    @Value("${netty.server.port:18889}")
    private int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    public void sendPrintData(StringingOrder order) {
        try {
            Map<String, Object> printData = buildPrintData(order);
            String json = JSON.toJSONString(printData);
            log.info("发送打印数据: {}", json);
            sendByNettyClient(json);
        } catch (Exception e) {
            log.error("发送打印数据失", e);
        }
    }
    private void sendByNettyClient(String json) {
        new Thread(() -> {
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast(new StringEncoder(CharsetUtil.UTF_8));
                                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelActive(ChannelHandlerContext ctx) {
                                        ctx.writeAndFlush(json + "\n");
                                        log.info("Netty客户端已发送打印数据到C#程序");
                                        ctx.close();
                                    }
                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                        log.warn("Netty发送异常，C#打印程序可能未启动: {}", cause.getMessage());
                                        ctx.close();
                                    }
                                });
                            }
                        });
                int csPort = 18889;
                ChannelFuture future = bootstrap.connect("127.0.0.1", csPort).sync();
                future.channel().closeFuture().sync();
            } catch (Exception e) {
                log.warn("连接C#打印程序失败(端口{}), 请确认C#程序已启动: {}", 18889, e.getMessage());
            } finally {
                group.shutdownGracefully();
            }
        }).start();
    }
    private Map<String, Object> buildPrintData(StringingOrder order) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "PRINT_ORDER");
        data.put("orderNo", order.getOrderNo());
        data.put("id", order.getId());
        data.put("status", order.getStatus());
        data.put("mainTension", order.getMainTension());
        data.put("crossTension", order.getCrossTension());
        data.put("totalPrice", order.getTotalPrice());
        data.put("timestamp", System.currentTimeMillis());
        // 关联查询名称
        if (order.getShopId() != null) {
            Shop shop = shopService.getById(order.getShopId());
            data.put("shopName", shop != null ? shop.getName() : "");
        }
        if (order.getPlayerId() != null) {
            Player player = playerService.getById(order.getPlayerId());
            data.put("playerName", player != null ? player.getName() : "");
            data.put("playerPhone", order.getPlayerPhone() != null ? order.getPlayerPhone() :
                    (player != null ? player.getPhone() : ""));
        }
        if (order.getRacketId() != null) {
            Racket racket = racketService.getById(order.getRacketId());
            data.put("racket", racket != null ? racket.getBrand() + " " + racket.getModel() : "");
        }
        if (order.getMainStringId() != null) {
            StringInfo ms = stringInfoService.getById(order.getMainStringId());
            data.put("mainString", ms != null ? ms.getBrand() + " " + ms.getModel() : "");
        }
        if (order.getCrossStringId() != null) {
            StringInfo cs = stringInfoService.getById(order.getCrossStringId());
            data.put("crossString", cs != null ? cs.getBrand() + " " + cs.getModel() : "");
        }
        // 拼装打印HTML模板
        try {
            Long tenantId = order.getTenantId();
            if (tenantId != null) {
                PrintTemplate tpl = printTemplateService.getDefaultTemplate(tenantId);
                if (tpl != null) {
                    String html = renderHtml(tpl, data);
                    data.put("printHtml", html);
                    data.put("templateName", tpl.getName());
                }
            }
        } catch (Exception e) {
            log.warn("渲染打印模板失败: {}", e.getMessage());
        }
        return data;
    }
    private String renderHtml(PrintTemplate tpl, Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset='utf-8'><style>");
        sb.append("body{margin:0;padding:6px;font-family:'Microsoft YaHei',sans-serif;");
        sb.append("width:").append(tpl.getPaperWidth()).append("mm;");
        sb.append("min-height:").append(tpl.getPaperHeight()).append("mm;}");
        sb.append(".f{margin:2px 0;line-height:1.4;}");
        sb.append(".c{text-align:center;}");
        sb.append(".b{font-weight:bold;}");
        sb.append(".q{text-align:center;margin-top:4px;font-size:8px;color:#999;}");
        sb.append("hr{border:none;border-top:1px dashed #999;margin:4px 0;}");
        sb.append("</style></head><body>");
        try {
            JSONArray fields = JSON.parseArray(tpl.getFieldsJson());
            for (int i = 0; i < fields.size(); i++) {
                JSONObject f = fields.getJSONObject(i);
                String key = f.getString("key");
                String label = f.getString("label");
                int fs = f.getIntValue("fontSize", 10);
                boolean bold = f.getBooleanValue("bold");
                String align = f.getString("align");
                Object val = data.get(key);
                String text = val != null ? val.toString() : "";
                sb.append("<div class='f");
                if ("center".equals(align)) sb.append(" c");
                if (bold) sb.append(" b");
                sb.append("' style='font-size:").append(fs).append("px'>");
                sb.append(label).append(": ").append(text);
                sb.append("</div>");
            }
        } catch (Exception e) {
            sb.append("<div>模板渲染错误</div>");
        }
        if (tpl.getShowQrcode() == 1) {
            sb.append("<div class='q'>[二维码]</div>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }
}
