package com.syty.netty;
import com.alibaba.fastjson2.JSON;
import com.syty.entity.StringingOrder;
import com.syty.service.*;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
/**
 * MQTT 打印发送器
 * 当配置print.method=mqtt 时启用
 * C#客户端也订阅同一主题，接收到消息后打印
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "print.method", havingValue = "mqtt")
public class MqttPrintSender implements PrintSender {
    private final ShopService shopService;
    private final PlayerService playerService;
    private final RacketService racketService;
    private final StringInfoService stringInfoService;
    private final PrintTemplateService printTemplateService;
    private MqttClient mqttClient;
    public MqttPrintSender(ShopService shopService,
                           PlayerService playerService,
                           RacketService racketService,
                           StringInfoService stringInfoService,
                           PrintTemplateService printTemplateService,
                           @Value("${print.mqtt.broker:tcp://127.0.0.1:1883}") String broker,
                           @Value("${print.mqtt.client-id:syty-server}") String clientId) {
        this.shopService = shopService;
        this.playerService = playerService;
        this.racketService = racketService;
        this.stringInfoService = stringInfoService;
        this.printTemplateService = printTemplateService;
        try {
            mqttClient = new MqttClient(broker, clientId, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);
            mqttClient.connect(options);
            log.info("MQTT 打印发送器连接成功: broker={}", broker);
        } catch (MqttException e) {
            log.warn("MQTT 打印发送器连接失败, 请确认Broker 已启动 {}", e.getMessage());
        }
    }
    @Override
    public void sendPrintData(StringingOrder order) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            log.warn("MQTT未连接，跳过打印推");
            return;
        }
        try {
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
                var shop = shopService.getById(order.getShopId());
                data.put("shopName", shop != null ? shop.getName() : "");
            }
            if (order.getPlayerId() != null) {
                var player = playerService.getById(order.getPlayerId());
                data.put("playerName", player != null ? player.getName() : "");
                data.put("playerPhone", order.getPlayerPhone() != null ? order.getPlayerPhone() :
                        (player != null ? player.getPhone() : ""));
            }
            if (order.getRacketId() != null) {
                var racket = racketService.getById(order.getRacketId());
                data.put("racket", racket != null ? racket.getBrand() + " " + racket.getModel() : "");
            }
            if (order.getMainStringId() != null) {
                var ms = stringInfoService.getById(order.getMainStringId());
                data.put("mainString", ms != null ? ms.getBrand() + " " + ms.getModel() : "");
            }
            if (order.getCrossStringId() != null) {
                var cs = stringInfoService.getById(order.getCrossStringId());
                data.put("crossString", cs != null ? cs.getBrand() + " " + cs.getModel() : "");
            }
            // 渲染打印HTML模板
            try {
                Long tenantId = order.getTenantId();
                if (tenantId != null) {
                    var tpl = printTemplateService.getDefaultTemplate(tenantId);
                    if (tpl != null) {
                        data.put("templateName", tpl.getName());
                    }
                }
            } catch (Exception e) {
                log.warn("获取打印模板失败: {}", e.getMessage());
            }
            String json = JSON.toJSONString(data);
            MqttMessage message = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            // 动态主题：根据店铺 ID 路由到对应的穿线机
            String shopId = order.getShopId() != null ? order.getShopId().toString() : "unknown";
            String topic = shopId + "/printer";
            mqttClient.publish(topic, message);
            log.info("MQTT已发布打印指令-> topic={}, orderNo={}", topic, order.getOrderNo());
        } catch (MqttException e) {
            log.error("MQTT发布打印数据失败: {}", e.getMessage());
        }
    }
    @PreDestroy
    public void destroy() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
                log.info("MQTT客户端已关闭");
            }
        } catch (MqttException e) {
            log.warn("MQTT关闭异常: {}", e.getMessage());
        }
    }
}
