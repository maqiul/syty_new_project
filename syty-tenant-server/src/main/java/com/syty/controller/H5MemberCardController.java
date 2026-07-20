package com.syty.controller;

import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.dto.PunchCardIssueRequest;
import com.syty.dto.PunchCardResponse;
import com.syty.service.PunchCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * H5 端次卡接口
 */
@RestController
@RequestMapping("/api/v1/h5/member")
@RequiredArgsConstructor
public class H5MemberCardController {

    private final PunchCardService punchCardService;

    /**
     * 查询用户可用次卡
     */
    @GetMapping("/cards")
    public Result<List<PunchCardResponse>> getAvailableCards(@RequestParam String phone) {
        Long tenantId = TenantContext.getTenantId();
        // 如果 H5 没有租户上下文，可能需要从请求中获取或默认为 null (视架构而定)
        // 假设 H5 也有 tenantId 上下文
        if (tenantId == null) {
            // Fallback logic if needed, e.g., default tenant or error
        }
        return Result.success(punchCardService.getAvailableCards(phone, tenantId));
    }
}
