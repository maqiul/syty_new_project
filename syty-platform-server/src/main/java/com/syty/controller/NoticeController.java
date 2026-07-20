package com.syty.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.dto.ApiResult;
import com.syty.entity.PlatformNotice;
import com.syty.mapper.PlatformNoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/notices")
@RequiredArgsConstructor
public class NoticeController {
    private final PlatformNoticeMapper noticeMapper;

    @GetMapping
    public ApiResult<Page<PlatformNotice>> list(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return ApiResult.success(noticeMapper.selectPage(new Page<>(page, size), null));
    }

    @PostMapping
    public ApiResult<Void> add(@RequestBody PlatformNotice notice) {
        noticeMapper.insert(notice);
        return ApiResult.success();
    }

    @PutMapping("/{id}/publish")
    public ApiResult<Void> publish(@PathVariable Long id) {
        PlatformNotice n = new PlatformNotice();
        n.setId(id);
        n.setStatus("PUBLISHED");
        noticeMapper.updateById(n);
        return ApiResult.success();
    }
    
    // Tenant side endpoint logic is usually in tenant-server, 
    // but if sharing DB, can add here for simplicity. 
    // Let's add a public endpoint for tenants to fetch notices.
    @GetMapping("/tenant/{tenantId}")
    public ApiResult<List<PlatformNotice>> getTenantNotices(@PathVariable String tenantId) {
        return ApiResult.success(noticeMapper.selectVisibleNotices(tenantId));
    }
}
