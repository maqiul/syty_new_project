package com.syty.netty;
import com.alibaba.fastjson2.JSON;
import com.syty.entity.StringingOrder;
import com.syty.service.ShopService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
/**
 * Netty服务字- 等待C#打印客户端连字
 * C#程序作为客户端连接到本服务端后，服务端将打印数据推送给C#客户字
 * 此方式适合C#程序主动连接并保持长连接
 */
@Slf4j
@Component
public class NettyPrintServer {
    @Value("${netty.server.port:18889}")
    private int port;
    private final ShopService shopService;
    // 已连接的C#客户端列字
    private final CopyOnWriteArrayList<Channel> clientChannels = new CopyOnWriteArrayList<>();
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    public NettyPrintServer(ShopService shopService) {
        this.shopService = shopService;
    }
    @PostConstruct
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        // 使用非阻塞方式启动，避免影响Spring启动流程
        new Thread(() -> {
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                                pipeline.addLast(new PrintServerHandler());
                            }
                        });
                ChannelFuture future = bootstrap.bind(port).sync();
                log.info("Netty打印服务端已启动，监听端口: {}", port);
                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                log.error("Netty服务端启动被中断", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Netty服务端启动失字端口{}可能被占字", port, e);
            }
        }, "netty-print-server").start();
    }
    @PreDestroy
    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("Netty打印服务端已关闭");
    }
    /**
     * 向所有已连接的C#客户端广播打印数字
     */
    public void broadcastPrintData(StringingOrder order) {
        Map<String, Object> data = buildPrintData(order);
        String json = JSON.toJSONString(data);
        json = json + "\n";
        log.info("广播打印数据到{}个C#客户端: {}", clientChannels.size(), json);
        for (Channel channel : clientChannels) {
            if (channel.isActive()) {
                channel.writeAndFlush(Unpooled.copiedBuffer(json, CharsetUtil.UTF_8));
            } else {
                clientChannels.remove(channel);
            }
        }
    }
    private Map<String, Object> buildPrintData(StringingOrder order) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "PRINT_ORDER");
        data.put("orderNo", order.getOrderNo());
        data.put("id", order.getId());
        data.put("mainTension", order.getMainTension());
        data.put("crossTension", order.getCrossTension());
        data.put("totalPrice", order.getTotalPrice());
        data.put("timestamp", System.currentTimeMillis());
        if (order.getShopId() != null) {
            var shop = shopService.getById(order.getShopId());
            data.put("shopName", shop != null ? shop.getName() : "");
        }
        return data;
    }
    /**
     * 打印服务端处理器
     */
    private class PrintServerHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            clientChannels.add(ctx.channel());
            log.info("C#打印客户端已连接: {}", ctx.channel().remoteAddress());
        }
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            clientChannels.remove(ctx.channel());
            log.info("C#打印客户端已断开: {}", ctx.channel().remoteAddress());
        }
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.warn("C#客户端异常: {}", cause.getMessage());
            clientChannels.remove(ctx.channel());
            ctx.close();
        }
    }
}
