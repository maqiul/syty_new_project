package com.syty.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("platform_notice")
public class PlatformNotice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String noticeType; // ANNOUNCEMENT, MAINTENANCE
    private String targetType; // ALL, SPECIFIC
    private String targetIds;  // JSON
    private String status;     // DRAFT, PUBLISHED
    private LocalDateTime createdAt;
}
