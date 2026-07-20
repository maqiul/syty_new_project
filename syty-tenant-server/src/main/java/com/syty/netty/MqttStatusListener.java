package com.syty.netty;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
/**
 * MQTT 状态监听器
 * 订阅 +/sender 通道，接收C# 客户端的 ONLINE/OFFLINE 状态消息
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
            log.info("📡 已订阅状态主题: /sender");
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
                log.info("🟢 店铺 [{}] 穿线客户端上", shopId);
                // TODO: 未来可以在此处更新Redis 中的店铺在线状态
            } else {
                log.info("ℹ️ 收到店铺 [{}] 消息: {}", shopId, type);
            }
        } catch (Exception e) {
            log.warn("解析状态消息失败: {}", e.getMessage());
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