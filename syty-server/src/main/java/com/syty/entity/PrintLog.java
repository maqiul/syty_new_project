package com.syty.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("print_log")
public class PrintLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String printerName;
    private String templateName;
    private String status;
    private LocalDateTime printTime;
    private String errorMessage;
    private Long shopId;
    private Long tenantId;
    private LocalDateTime createdAt;
}
