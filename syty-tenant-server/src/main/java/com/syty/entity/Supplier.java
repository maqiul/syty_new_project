package com.syty.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("supplier")
public class Supplier {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;           // 供应商名称
    private String contactPerson;  // 联系人
    private String phone;          // 电话
    private String email;          // 邮箱
    private String address;        // 地址
    private String sportType;      // 运动类型: BADMINTON/TENNIS/DUAL
    private String remark;         // 备注
    private Long tenantId;         // 所属租户
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
