package com.syty.controller;

import com.syty.dto.ApiResult;
import com.syty.entity.PlatformConfig;
import com.syty.service.PlatformConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform/config")
@RequiredArgsConstructor
public class PlatformConfigController {
    private final PlatformConfigService configService;

    @GetMapping
    public ApiResult<PlatformConfig> getConfig() {
        return ApiResult.success(configService.getConfig());
    }

    @PutMapping
    public ApiResult<Void> updateConfig(@RequestBody PlatformConfig config) {
        configService.updateConfig(config);
        return ApiResult.success();
    }
}
