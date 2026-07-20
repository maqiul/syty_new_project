package com.syty.netty;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * MQTT 状态监听器
 * 订阅 +/sender 通道，接收C# 客户端的 ONLINE/OFFLINE 状态消息
 * 并将店铺在线状态同步到 Redis
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "print.method", havingValue = "mqtt")
public class MqttStatusListener {
    private MqttClient mqttClient;

    @Value("${mqtt.broker:tcp://localhost:1883}")
    private String broker;

    @Value("${mqtt.client-id:syty-status-listener}")
    private String clientId;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /** Redis Key 前缀：店铺在线状态 */
    private static final String SHOP_ONLINE_KEY_PREFIX = "shop:online:";

    /** 店铺在线状态过期时间（秒）：5分钟无心跳自动下线 */
    private static final long SHOP_ONLINE_EXPIRE_SECONDS = 300;

    @PostConstruct
    public void init() {
        try {
            mqttClient = new MqttClient(broker, clientId, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);
            mqttClient.connect(options);
            log.info("📡 状态监听器 MQTT 连接成功: broker={}", broker);

            // 订阅通配符主题：匹配所有店铺的 sender 通道，例如101/sender
            mqttClient.subscribe("+/sender", 1, (topic, msg) -> {
                handleSenderMessage(topic, msg);
            });
            log.info("📡 已订阅状态主题: +/sender");
        } catch (MqttException e) {
            log.error("📡 状态监听器 MQTT 连接失败: {}", e.getMessage());
        }
    }

    private void handleSenderMessage(String topic, MqttMessage msg) {
        try {
            String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);

            // 解析 Topic 获取 shopId: "101/sender" -> 101
            String[] parts = topic.split("/");
            String shopId = parts.length > 0 ? parts[0] : "unknown";

            // 解析 Payload
            var jsonObj = JSON.parseObject(payload);
            String type = jsonObj.getString("type");

            if ("ONLINE".equalsIgnoreCase(type)) {
                log.info("🟢 店铺 [{}] 穿线客户端上线", shopId);
                // 更新 Redis 中的店铺在线状态
                updateShopOnlineStatus(shopId, true);
            } else if ("OFFLINE".equalsIgnoreCase(type)) {
                log.info("🔴 店铺 [{}] 穿线客户端下线", shopId);
                // 更新 Redis 中的店铺在线状态
                updateShopOnlineStatus(shopId, false);
            } else if ("HEARTBEAT".equalsIgnoreCase(type)) {
                // 心跳消息，刷新在线时间
                updateShopOnlineStatus(shopId, true);
                log.debug("💓 店铺 [{}] 心跳", shopId);
            } else {
                log.info("ℹ️ 收到店铺 [{}] 消息: {}", shopId, type);
            }
        } catch (Exception e) {
            log.warn("解析状态消息失败: {}", e.getMessage());
        }
    }

    /**
     * 更新店铺在线状态到 Redis
     *
     * @param shopId 店铺ID
     * @param online 是否在线
     */
    private void updateShopOnlineStatus(String shopId, boolean online) {
        try {
            String redisKey = SHOP_ONLINE_KEY_PREFIX + shopId;
            if (online) {
                // 在线：设置状态，并设置过期时间（5分钟无心跳自动下线）
                redisTemplate.opsForValue().set(redisKey, "1", SHOP_ONLINE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            } else {
                // 离线：删除状态
                redisTemplate.delete(redisKey);
            }
            log.debug("Redis 店铺状态更新: shopId={}, online={}", shopId, online);
        } catch (Exception e) {
            log.error("Redis 店铺状态更新失败: shopId={}, error={}", shopId, e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
            }
        } catch (MqttException e) {
            log.warn("状态监听器关闭异常: {}", e.getMessage());
        }
    }
}