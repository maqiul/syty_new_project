package com.syty.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("platform_config")
public class PlatformConfig {
    @TableId
    private Long id;
    
    // Brand
    private String platformName;
    private String logoUrl;
    private String faviconUrl;
    
    // Login / Registration
    private String loginBgUrl;
    private Boolean enableSelfRegister;
    private Integer defaultTrialDays;
    
    // Footer / Contact
    private String supportEmail;
    private String icpLicense;

    // Meta
    private Long configVersion;
    private LocalDateTime updatedAt;
}
