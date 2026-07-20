package com.syty.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syty.config.TenantContextHolder;
import com.syty.dto.PrintSetupResponse;
import com.syty.dto.PrinterRegisterRequest;
import com.syty.dto.PrintTaskReportRequest;
import com.syty.dto.StockReservationResult;
import com.syty.entity.PrintResource;
import com.syty.entity.PrintTaskLogEntity;
import com.syty.entity.PrinterConfig;
import com.syty.mapper.PrintResourceMapper;
import com.syty.mapper.PrintTaskLogMapper;
import com.syty.mapper.PrinterConfigMapper;
import com.syty.util.SnowflakeIdGenerator;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.*;
/**
 * 打印配置服务
 *
 * V1.2 变更:
 * 1. 订单号改为雪花算法 (SnowflakeIdGenerator)
 * 2. 新增打印回执上报 (reportPrintTask)
 * 3. getPrintSetup 改为 JOIN 联表查询
 * 4. 集成库存预留机制 (InventoryService)
 * 5. 【安全修复】全面启用租户隔离，print_rule/print_resource 不再共享
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrinterService {
    private final PrinterConfigMapper printerConfigMapper;
    private final PrintResourceMapper printResourceMapper;
    private final PrintTaskLogMapper printTaskLogMapper;
    private final MinioClient minioClient;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final InventoryService inventoryService;
    @Value("${minio.bucket-name}")
    private String bucketName;
    @Value("${minio.url-prefix}")
    private String urlPrefix;
    // ==================== API 1: 注册打印机 ====================
    /**
     * 注册/更新打印机配置 (幂等操作)
     *
     * V1.2 安全修复: 删除操作自动由租户拦截器追加 tenant_id 条件
     */
    @Transactional(rollbackFor = Exception.class)
    public void registerPrinter(PrinterRegisterRequest request) {
        String shopId = request.getShopId();
        String machineId = request.getMachineId();
        List<String> printerNames = request.getPrinterNames();
        String defaultPrinterName = request.getDefaultPrinterName();
        Long tenantId = TenantContextHolder.getTenantId();
        log.info("注册打印机: tenantId={}, shopId={}, machineId={}, printers={}",
                tenantId, shopId, machineId, printerNames);
        // Step 1: 删除该机器下所有旧的打印机配置 (实现幂等)
        // 租户拦截器自动追加: AND tenant_id = #{currentTenantId}
        LambdaQueryWrapper<PrinterConfig> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(PrinterConfig::getShopId, shopId)
                     .eq(PrinterConfig::getMachineId, machineId);
        printerConfigMapper.delete(deleteWrapper);
        // Step 2: 批量插入新的打印机配置
        // 租户拦截器自动注入: tenant_id 字段
        List<PrinterConfig> entities = new ArrayList<>();
        for (int i = 0; i < printerNames.size(); i++) {
            String name = printerNames.get(i);
            PrinterConfig entity = new PrinterConfig();
            entity.setTenantId(tenantId); // 显式设置，双重保险
            entity.setShopId(shopId);
            entity.setMachineId(machineId);
            entity.setPrinterName(name);
            boolean isDefault = (defaultPrinterName != null && defaultPrinterName.equals(name))
                    || (defaultPrinterName == null && i == 0);
            entity.setIsDefault(isDefault ? 1 : 0);
            entity.setStatus(1);
            entities.add(entity);
        }
        for (PrinterConfig entity : entities) {
            printerConfigMapper.insert(entity);
        }
        log.info("打印机注册成功: tenantId={}, shopId={}, machineId={}, 共{}台",
                tenantId, shopId, machineId, printerNames.size());
    }
    // ==================== API 2: 拉取配置 (JOIN 优化版) ====================
    /**
     * V1.2 优化: 使用 JOIN 联表查询替代原来的 N+1 独立查询
     * 原来: 3 次独立 SQL (printer_config + print_rule + print_resource)
     * 现在: 1 次 JOIN SQL, 一次性返回所有数据
     *
     * V1.2 安全修复: JOIN 查询显式传入 tenantId，防止跨租户数据泄露
     */
    public PrintSetupResponse getPrintSetup(String shopId, String machineId) {
        Long tenantId = TenantContextHolder.getTenantId();
        log.info("拉取打印配置 (JOIN): tenantId={}, shopId={}, machineId={}",
                tenantId, shopId, machineId);
        // 1. JOIN 联表查询 (一次搞定)
        // 显式传入 tenantId，确保 JOIN 条件正确过滤
        List<Map<String, Object>> joinResults = printerConfigMapper.selectPrintSetupWithJoin(shopId, machineId, tenantId);
        if (joinResults.isEmpty()) {
            log.warn("未找到打印配置: tenantId={}, shopId={}, machineId={}", tenantId, shopId, machineId);
            return PrintSetupResponse.builder()
                    .printers(Collections.emptyList())
                    .rules(Collections.emptyMap())
                    .resources(Collections.emptyMap())
                    .build();
        }
        // 2. 去重组装打印机列表 (LEFT JOIN 会产生多行)
        Set<String> seenPrinters = new LinkedHashSet<>();
        List<PrintSetupResponse.PrinterInfo> printers = new ArrayList<>();
        for (Map<String, Object> row : joinResults) {
            String printerName = (String) row.get("printerName");
            if (printerName != null && seenPrinters.add(printerName)) {
                Integer isDefault = (Integer) row.get("isDefault");
                printers.add(PrintSetupResponse.PrinterInfo.builder()
                        .printerName(printerName)
                        .isDefault(isDefault != null && isDefault == 1)
                        .build());
            }
        }
        // 3. 去重组装打印规则
        Map<String, PrintSetupResponse.PrintRuleInfo> ruleMap = new LinkedHashMap<>();
        for (Map<String, Object> row : joinResults) {
            String docType = (String) row.get("docType");
            if (docType != null && !ruleMap.containsKey(docType)) {
                ruleMap.put(docType, PrintSetupResponse.PrintRuleInfo.builder()
                        .docType(docType)
                        .printerName((String) row.get("rulePrinter"))
                        .templatePath((String) row.get("templatePath"))
                        .paperSize((String) row.get("paperSize"))
                        .copies(row.get("copies") != null ? ((Number) row.get("copies")).intValue() : 1)
                        .build());
            }
        }
        // 4. 去重组装资源 (门店级优先)
        Map<String, String> resourceMap = new LinkedHashMap<>();
        for (Map<String, Object> row : joinResults) {
            String resourceKey = (String) row.get("resourceKey");
            String resourceUrl = (String) row.get("resourceUrl");
            if (resourceKey != null && resourceUrl != null && !resourceMap.containsKey(resourceKey)) {
                resourceMap.put(resourceKey, resourceUrl);
            }
        }
        return PrintSetupResponse.builder()
                .printers(printers)
                .rules(ruleMap)
                .resources(resourceMap)
                .build();
    }
    // ==================== API 3: 生成订单号 (雪花算法) ====================
    public String generateOrderId() {
        String orderId = snowflakeIdGenerator.nextOrderId();
        log.info("生成订单号: {}", orderId);
        return orderId;
    }
    // ==================== API 4: 打印回执上报 ====================
    @Transactional(rollbackFor = Exception.class)
    public void reportPrintTask(PrintTaskReportRequest request) {
        log.info("收到打印回执上报: orderId={}, printerId={}, status={}",
                request.getOrderId(), request.getPrinterId(), request.getStatus());
        PrintTaskLogEntity logEntity = new PrintTaskLogEntity();
        logEntity.setOrderId(request.getOrderId());
        logEntity.setPrinterId(request.getPrinterId());
        logEntity.setStatus(request.getStatus());
        logEntity.setErrorMsg(request.getErrorMsg());
        printTaskLogMapper.insert(logEntity);
        log.info("打印回执已保存: id={}", logEntity.getId());
        if ("FAILED".equalsIgnoreCase(request.getStatus())) {
            log.warn("打印失败, 触发库存释放: orderId={}", request.getOrderId());
        }
    }
    // ==================== API 5: 接单 + 库存预留 ====================
    public StockReservationResult reserveStockForOrder(String skuCode, int amount) {
        log.info("接单预留库存: skuCode={}, amount={}", skuCode, amount);
        return inventoryService.reserveStock(skuCode, amount);
    }
    public void deductStockOnComplete(String skuCode, int amount) {
        log.info("完成订单实扣库存: skuCode={}, amount={}", skuCode, amount);
        inventoryService.deductStock(skuCode, amount);
    }
    // ==================== OSS 图片上传 ====================
    /**
     * 上传图片到 MinIO, 并记录到 print_resource 表
     *
     * V1.2 安全修复: 查询和插入自动由租户拦截器追加 tenant_id 条件
     */
    @Transactional(rollbackFor = Exception.class)
    public String uploadResource(String shopId, String resourceKey, MultipartFile file) {
        if (shopId == null) {
            shopId = "";
        }
        Long tenantId = TenantContextHolder.getTenantId();
        log.info("上传打印资源: tenantId={}, shopId={}, resourceKey={}, fileName={}",
                tenantId, shopId, resourceKey, file.getOriginalFilename());
        try {
            String objectName = "resources/" + shopId + "/" + resourceKey + "/"
                    + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }
            String fileUrl = urlPrefix + "/" + objectName;
            // 租户拦截器自动追加: AND tenant_id = #{currentTenantId}
            LambdaQueryWrapper<PrintResource> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PrintResource::getShopId, shopId)
                        .eq(PrintResource::getResourceKey, resourceKey);
            PrintResource existing = printResourceMapper.selectOne(queryWrapper);
            PrintResource resource = (existing != null) ? existing : new PrintResource();
            resource.setTenantId(tenantId); // 显式设置，双重保险
            resource.setShopId(shopId);
            resource.setResourceKey(resourceKey);
            resource.setResourceUrl(fileUrl);
            resource.setFileName(file.getOriginalFilename());
            resource.setMimeType(file.getContentType());
            resource.setFileSize(file.getSize());
            resource.setStatus(1);
            if (existing != null) {
                printResourceMapper.updateById(resource);
            } else {
                printResourceMapper.insert(resource);
            }
            log.info("资源上传成功: tenantId={}, url={}", tenantId, fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.error("上传资源失败: tenantId={}, shopId={}, resourceKey={}", tenantId, shopId, resourceKey, e);
            throw new RuntimeException("上传文件到 OSS 失败: " + e.getMessage(), e);
        }
    }
}
