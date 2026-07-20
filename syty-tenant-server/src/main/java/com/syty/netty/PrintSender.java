package com.syty.netty;
import com.syty.entity.*;
/**
 * 打印发送器接口
 * 支持 Netty TCP 字MQTT 两种实现，通过配置切换
 */
public interface PrintSender {
    /** 发送打印数据（仅传订单，内部关联查询） */
    void sendPrintData(StringingOrder order);
    /** 发送打印数据（传入完整上下文，减少数据库查询） */
    default void send(StringingOrder order, Player player, Racket racket, Shop shop,
                      String mainStringName, String crossStringName) {
        // 默认实现：兼容旧调用，子类可按需重写
        sendPrintData(order);
    }
}
