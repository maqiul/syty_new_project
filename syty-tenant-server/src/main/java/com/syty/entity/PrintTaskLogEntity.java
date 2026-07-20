package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
/**
 * 打印任务回执日志实体
 * 用于记录 C# 客户端上报的打印结果
 */
@Data
@TableName("print_task_log")
public class PrintTaskLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 订单号 (雪花算法生成) */
    private String orderId;
    /** 打印机标识 */
    private String printerId;
    /** 打印状态: SUCCESS-成功, FAILED-失败 */
    private String status;
    /** 错误信息 (失败时记录) */
    private String errorMsg;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
