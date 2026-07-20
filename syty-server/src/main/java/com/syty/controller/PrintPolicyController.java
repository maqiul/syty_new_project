package com.syty.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.dto.ApiResult;
import com.syty.dto.PageResult;
import com.syty.entity.PrintPolicyEntity;
import com.syty.mapper.PrintPolicyMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 打印策略管理 Controller (V1.4 新增)
 * <p>
 * 提供策略的分页查询能力，供前端管理页面使用
 * </p>
 *
 * @author 嘎嘎
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/print-policy")
@Tag(name = "打印策略管理", description = "打印策略的增删改查（分页查询）")
public class PrintPolicyController {

    private final PrintPolicyMapper printPolicyMapper;

    // ==================== API 1: 分页查询策略 ====================

    @GetMapping("/page")
    @SaCheckPermission("print:policy:view")
    @Operation(summary = "分页查询打印策略", description = "支持按门店ID、场景、运动类型、单据类型筛选。" +
            "指定shopId时: 优先返回该店铺专属策略，并附带公共策略供参考")
    public ApiResult<PageResult<PrintPolicyEntity>> page(
            @Parameter(description = "当前页码", required = true)
            @RequestParam(defaultValue = "1") int current,

            @Parameter(description = "每页条数", required = true)
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "门店ID（可选）。传入后同时返回该店铺专属+公共策略")
            @RequestParam(required = false) Long shopId,

            @Parameter(description = "场景: WALK_IN/TOURNAMENT/ALL（可选）")
            @RequestParam(required = false) String scene,

            @Parameter(description = "运动类型: BADMINTON/TENNIS/ALL（可选）")
            @RequestParam(required = false) String sportType,

            @Parameter(description = "单据类型: LABEL/RECEIPT（可选）")
            @RequestParam(required = false) String docType) {

        try {
            // 参数校验
            if (current < 1) {
                current = 1;
            }
            if (size < 1 || size > 100) {
                size = 10;
            }

            Page<PrintPolicyEntity> resultPage;

            if (shopId != null) {
                // V1.5: 店铺专属优先 + 公共降级策略
                // 查询条件: shop_id = #{shopId} OR shop_id IS NULL
                resultPage = queryWithShopId(current, size, shopId, scene, sportType, docType);
            } else {
                // 普通分页查询
                LambdaQueryWrapper<PrintPolicyEntity> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(shopId != null, PrintPolicyEntity::getShopId, shopId);
                wrapper.eq(StringUtils.hasText(scene), PrintPolicyEntity::getScene, scene);
                wrapper.eq(StringUtils.hasText(sportType), PrintPolicyEntity::getSportType, sportType);
                wrapper.eq(StringUtils.hasText(docType), PrintPolicyEntity::getDocType, docType);
                wrapper.orderByDesc(PrintPolicyEntity::getCreatedAt);
                resultPage = printPolicyMapper.selectPage(new Page<>(current, size), wrapper);
            }

            // 转换为 PageResult
            PageResult<PrintPolicyEntity> pageResult = PageResult.build(
                    resultPage.getCurrent(),
                    resultPage.getSize(),
                    resultPage.getTotal(),
                    resultPage.getPages(),
                    resultPage.getRecords()
            );

            return ApiResult.success(pageResult);

        } catch (Exception e) {
            log.error("分页查询策略异常", e);
            return ApiResult.error(500, "查询失败: " + e.getMessage());
        }
    }

    private Page<PrintPolicyEntity> queryWithShopId(int current, int size, Long shopId, String scene, String sportType, String docType) {
        LambdaQueryWrapper<PrintPolicyEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(PrintPolicyEntity::getShopId, shopId).or().isNull(PrintPolicyEntity::getShopId));
        wrapper.eq(StringUtils.hasText(scene), PrintPolicyEntity::getScene, scene);
        wrapper.eq(StringUtils.hasText(sportType), PrintPolicyEntity::getSportType, sportType);
        wrapper.eq(StringUtils.hasText(docType), PrintPolicyEntity::getDocType, docType);
        wrapper.orderByDesc(PrintPolicyEntity::getCreatedAt);
        return printPolicyMapper.selectPage(new Page<>(current, size), wrapper);
    }
}
