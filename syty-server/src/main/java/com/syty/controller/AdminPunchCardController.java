package com.syty.controller;

import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.dto.PunchCardIssueRequest;
import com.syty.service.PunchCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;



/**
 * 管理端次卡接口
 */
@RestController
@RequestMapping("/api/v1/admin/punch-card")
@RequiredArgsConstructor
public class AdminPunchCardController {

    private final PunchCardService punchCardService;

    /**
     * 售卡 (发卡)
     */
    @PostMapping("/issue")
    public Result<Void> issueCard(@Valid @RequestBody PunchCardIssueRequest request) {
        // Ensure tenant ID is set from context
        request.setTenantId(TenantContext.getTenantId());
        punchCardService.issueCard(request);
        return Result.success();
    }
}
