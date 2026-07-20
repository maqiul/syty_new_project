package com.syty.util;
import lombok.extern.slf4j.Slf4j;
/**
 * 雪花算法 ID 生成器 (Snowflake)
 * <p>
 * 64位结构: [1位符号位] [41位时间戳] [10位机器ID] [12位序列号]
 * - 时间戳: 毫秒级, 可用约69年
 * - 机器ID: 5位数据中心 + 5位工作器 (支持1024节点)
 * - 序列号: 每毫秒最多4096个ID
 * </p>
 */
@Slf4j
public class SnowflakeIdGenerator {
    /** 起始时间戳 (2024-01-01 00:00:00 UTC) */
    private static final long EPOCH = 1704067200000L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    String.format("workerId 必须在 0 到 %d 之间", MAX_WORKER_ID));
        }
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException(
                    String.format("datacenterId 必须在 0 到 %d 之间", MAX_DATACENTER_ID));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        log.info("SnowflakeIdGenerator 初始化: workerId={}, datacenterId={}", workerId, datacenterId);
    }
    /**
     * 生成下一个唯一ID
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        // 时钟回拨检测
        if (timestamp < lastTimestamp) {
            long diff = lastTimestamp - timestamp;
            if (diff <= 5) {
                // 5ms以内回拨, 等待补偿
                try {
                    wait(diff * 2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("时钟回拨补偿被中断", e);
                }
                timestamp = System.currentTimeMillis();
                if (timestamp < lastTimestamp) {
                    throw new RuntimeException(
                            String.format("时钟回拨异常, 拒绝生成ID. 回拨: %dms", diff));
                }
            } else {
                throw new RuntimeException(
                        String.format("时钟回拨超过5ms, 拒绝生成ID. 回拨: %dms", diff));
            }
        }
        // 同一毫秒内, 序列号递增
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // 序列号耗尽, 等待下一毫秒
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        // 组装64位ID
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }
    /**
     * 等待下一毫秒
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
    /**
     * 生成订单号: "ORD" + 雪花ID
     * 例: ORD123456789012345678
     */
    public String nextOrderId() {
        return "ORD" + nextId();
    }
}
