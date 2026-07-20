package com.syty.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syty.entity.PrintPolicyEntity;
import com.syty.entity.PrintTemplate;
import com.syty.mapper.PrintPolicyMapper;
import com.syty.mapper.PrintTemplateMapper;
import com.syty.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 店铺模板/策略同步服务 (V1.5 新增)
 * <p>
 * 核心职责:
 * 1. 新店铺创建时, 自动从公共模板/策略复制一份到该店铺
 * 2. 保证每个店铺开箱即有默认打印配置
 * 3. 店铺可以独立修改自己的模板/策略, 不影响其他店
 * </p>
 *
 * @author 嘎嘎
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopTemplateSyncService {

    private final PrintTemplateMapper printTemplateMapper;
    private final PrintPolicyMapper printPolicyMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 新店铺创建后, 自动同步公共模板和策略
     * <p>
     * 事务保证: 模板复制 + 策略复制要么全部成功, 要么全部回滚.
     * 复制规则:
     * - 查询所有 shop_id IS NULL 的公共模板
     * - 复制每条记录, 将 shopId 设为新店铺ID
     * - 模板ID使用雪花算法重新生成 (避免冲突)
     * - 策略记录中的 templateId 指向新复制的模板ID
     * </p>
     *
     * @param newShopId 新店铺的ID
     * @return 同步结果 (复制的模板数, 策略数)
     */
    @Transactional(rollbackFor = Exception.class)
    public SyncResult syncPublicToShop(Long newShopId) {
        if (newShopId == null) {
            throw new IllegalArgumentException("店铺ID不能为空");
        }

        log.info("【店铺模板同步】开始为新店铺 {} 同步公共配置", newShopId);

        int templateCount = 0;
        int policyCount = 0;

        try {
            // Step 1: 查询所有公共模板 (shop_id IS NULL)
            LambdaQueryWrapper<PrintTemplate> templateWrapper = new LambdaQueryWrapper<>();
            templateWrapper.isNull(PrintTemplate::getShopId);
            templateWrapper.eq(PrintTemplate::getStatus, 1);
            List<PrintTemplate> publicTemplates = printTemplateMapper.selectList(templateWrapper);

            if (publicTemplates.isEmpty()) {
                log.warn("【店铺模板同步】未找到公共模板, 跳过同步. shopId={}", newShopId);
                return new SyncResult(0, 0);
            }

            // 建立旧模板ID → 新模板ID 的映射关系 (策略复制时需要)
            java.util.Map<Long, Long> templateIdMapping = new java.util.HashMap<>();

            // Step 2: 复制模板
            LocalDateTime now = LocalDateTime.now();
            for (PrintTemplate publicTemplate : publicTemplates) {
                Long oldTemplateId = publicTemplate.getId();
                Long newTemplateId = generateSnowflakeId();

                PrintTemplate newTemplate = new PrintTemplate();
                newTemplate.setId(newTemplateId);
                newTemplate.setName(publicTemplate.getName());
                newTemplate.setShopId(newShopId);
                newTemplate.setContent(publicTemplate.getContent());
                // newTemplate.setVersion(1); // 移除，因为 PrintTemplate 实体里可能没有 version 字段
                newTemplate.setStatus(publicTemplate.getStatus());
                newTemplate.setCreatedAt(now);
                newTemplate.setUpdatedAt(now);

                printTemplateMapper.insert(newTemplate);
                templateIdMapping.put(oldTemplateId, newTemplateId);
                templateCount++;

                log.debug("【店铺模板同步】模板已复制: {} -> {}, name={}",
                        oldTemplateId, newTemplateId, publicTemplate.getName());
            }

            // Step 3: 查询所有公共策略 (shop_id IS NULL)
            LambdaQueryWrapper<PrintPolicyEntity> policyWrapper = new LambdaQueryWrapper<>();
            policyWrapper.isNull(PrintPolicyEntity::getShopId);
            policyWrapper.eq(PrintPolicyEntity::getStatus, 1);
            List<PrintPolicyEntity> publicPolicies = printPolicyMapper.selectList(policyWrapper);

            // Step 4: 复制策略
            for (PrintPolicyEntity publicPolicy : publicPolicies) {
                PrintPolicyEntity newPolicy = new PrintPolicyEntity();
                newPolicy.setId(generateSnowflakeId());
                newPolicy.setShopId(newShopId);
                newPolicy.setScene(publicPolicy.getScene());
                newPolicy.setSportType(publicPolicy.getSportType());
                newPolicy.setDocType(publicPolicy.getDocType());

                // 关键: 将 templateId 映射到新复制的模板
                Long oldTemplateId = publicPolicy.getTemplateId();
                Long newTemplateId = templateIdMapping.get(oldTemplateId);
                if (newTemplateId != null) {
                    newPolicy.setTemplateId(newTemplateId);
                } else {
                    // 如果公共策略关联的模板没有被复制 (可能是已停用的模板), 保持原ID
                    log.warn("【店铺模板同步】策略关联的模板未找到映射: policyId={}, templateId={}",
                            publicPolicy.getId(), oldTemplateId);
                    newPolicy.setTemplateId(oldTemplateId);
                }

                newPolicy.setTargetRole(publicPolicy.getTargetRole());
                newPolicy.setPriority(publicPolicy.getPriority());
                newPolicy.setStatus(publicPolicy.getStatus());
                newPolicy.setCreatedAt(now);
                newPolicy.setUpdatedAt(now);

                printPolicyMapper.insert(newPolicy);
                policyCount++;

                log.debug("【店铺模板同步】策略已复制: policyId={}, scene={}, docType={}",
                        newPolicy.getId(), newPolicy.getScene(), newPolicy.getDocType());
            }

            log.info("【店铺模板同步】完成! shopId={}, 模板数={}, 策略数={}",
                    newShopId, templateCount, policyCount);

            return new SyncResult(templateCount, policyCount);

        } catch (Exception e) {
            log.error("【店铺模板同步】失败! shopId={}, 已复制模板={}, 已复制策略={}",
                    newShopId, templateCount, policyCount, e);
            throw new RuntimeException("店铺模板同步失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用项目统一的雪花算法生成 ID
     */
    private Long generateSnowflakeId() {
        return snowflakeIdGenerator.nextId();
    }

    /**
     * 同步结果
     */
    public record SyncResult(int templateCount, int policyCount) {
        @Override
        public String toString() {
            return String.format("SyncResult{模板=%d, 策略=%d}", templateCount, policyCount);
        }
    }
}
