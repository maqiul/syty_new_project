package com.syty.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * 打印机配置实字
 */
@Data
@TableName("printer_config")
public class PrinterConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String shopId;
    private String machineId; // 机器字GUID
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String clientName; // 虚拟字段，返回给前端展示 (字MachineId)
    private String printerName; // 物理打印机名字
    private Integer status; // 1: 在线 0: 离线
    private Integer isDefault; // 1: 默认 0: 非默认
    private Long tenantId; // 租户ID
    private LocalDateTime lastHeartbeatAt; // 最后心跳时间
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
