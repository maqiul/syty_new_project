package com.syty.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.BizException;
import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.entity.*;
import com.syty.mapper.SysUserShopMapper;
import com.syty.netty.PrintSender;
import com.syty.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "订单管理")
@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class StringingOrderController {

    private final StringingOrderService orderService;
    private final ShopService shopService;
    private final PlayerService playerService;
    private final RacketService racketService;
    private final StringInfoService stringInfoService;
    private final StockService stockService;
    private final SysUserShopMapper sysUserShopMapper;
    private final PrintSender printSender;

    /** 检查当前用户是否有指定店铺的操作权限 */
    private void checkShopPermission(Long shopId) {
        if (TenantContext.isSuperAdmin()) return;
        Long userId = TenantContext.getUserId();
        Long count = sysUserShopMapper.selectCount(
                new LambdaQueryWrapper<SysUserShop>()
                        .eq(SysUserShop::getUserId, userId)
                        .eq(SysUserShop::getShopId, shopId));
        if (count == null || count == 0) {
            throw BizException.forbidden("您没有该店铺的操作权");
        }
    }

    private String getStringName(Long id) {
        StringInfo s = stringInfoService.getById(id);
        return s == null ? "未知" : s.getBrand() + " " + s.getModel();
    }

    @SaCheckPermission("order:create")
    @Operation(summary = "扫码创建订单")
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/scan")
    public Result<Object> scanCreate(@RequestBody Map<String, Object> params) {
        Long shopId = Long.valueOf(params.get("shopId").toString());
        Long playerId = Long.valueOf(params.get("playerId").toString());
        Long racketId = Long.valueOf(params.get("racketId").toString());
        checkShopPermission(shopId);
        if (shopService.getById(shopId) == null) return Result.error("店铺不存");
        if (playerService.getById(playerId) == null) return Result.error("球员不存");
        if (racketService.getById(racketId) == null) return Result.error("球拍不存");
        
        String orderNo = "SO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + new Random().nextInt(9000) + 1000;
        StringingOrder order = new StringingOrder();
        order.setOrderNo(orderNo);
        order.setShortOrderNo(generateShortOrderNo());
        order.setShopId(shopId);
        order.setPlayerId(playerId);
        order.setRacketId(racketId);
        order.setStatus(0);
        order.setTenantId(TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L);
        if (params.containsKey("mainStringId")) {
            order.setMainStringId(Long.valueOf(params.get("mainStringId").toString()));
            order.setMainTension(new BigDecimal(params.get("mainTension").toString()));
        }
        if (params.containsKey("crossStringId")) {
            order.setCrossStringId(Long.valueOf(params.get("crossStringId").toString()));
            order.setCrossTension(new BigDecimal(params.get("crossTension").toString()));
        }
        if (params.containsKey("totalPrice")) {
            order.setTotalPrice(new BigDecimal(params.get("totalPrice").toString()));
        }
        if (params.containsKey("remark")) {
            order.setRemark(params.get("remark").toString());
        }
        orderService.save(order);
        
        // 库存预留 (V1.3.1 修复)
        tryReserveStock(order);
        
        return Result.success(order);
    }

    @SaCheckPermission("order:create")
    @Operation(summary = "常规创建订单")
    @Transactional(rollbackFor = Exception.class)
    @PostMapping
    public Result<Object> create(@RequestBody StringingOrder order) {
        checkShopPermission(order.getShopId());
        if (order.getOrderNo() == null || order.getOrderNo().isBlank()) {
            order.setOrderNo("SO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + new Random().nextInt(9000) + 1000);
        }
        order.setShortOrderNo(generateShortOrderNo());
        order.setStatus(0);
        order.setTenantId(TenantContext.getTenantId() != null ? TenantContext.getTenantId() : 1L);
        orderService.save(order);
        
        // 库存预留 (V1.3.1 修复)
        tryReserveStock(order);
        
        return Result.success(order);
    }

    @Operation(summary = "客户自助登记订单（公开接口")
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/customer")
    public Result<Object> customerRegister(@RequestBody Map<String, Object> params) {
        // 参数提取
        String phone = params.get("phone") != null ? params.get("phone").toString() : null;
        String name = params.get("name") != null ? params.get("name").toString() : null;
        String racketModel = params.get("racketModel") != null ? params.get("racketModel").toString() : null;
        Long mainStringId = params.get("mainStringId") != null ? Long.valueOf(params.get("mainStringId").toString()) : null;
        BigDecimal mainTension = params.get("mainTension") != null ? new BigDecimal(params.get("mainTension").toString()) : null;
        Long shopId = params.get("shopId") != null ? Long.valueOf(params.get("shopId").toString()) : null;
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;
        Long playerId = params.get("playerId") != null ? Long.valueOf(params.get("playerId").toString()) : null;
        if (phone == null || name == null || racketModel == null || shopId == null) {
            return Result.error("手机号、姓名、拍子型号和店铺为必填项");
        }
        // 查找或创建球员
        if (playerId == null && phone != null) {
            Player existing = playerService.getOne(
                    new LambdaQueryWrapper<Player>().eq(Player::getPhone, phone).last("LIMIT 1"));
            if (existing != null) {
                playerId = existing.getId();
            } else {
                Player newPlayer = new Player();
                newPlayer.setName(StringUtils.hasText(name) ? name : "匿名客户");
                newPlayer.setPhone(phone);
                playerService.save(newPlayer);
                playerId = newPlayer.getId();
            }
        }
        // 查找或创建球员
        Long racketId = null;
        if (StringUtils.hasText(racketModel)) {
            Racket existing = racketService.getOne(
                    new LambdaQueryWrapper<Racket>().eq(Racket::getModel, racketModel).last("LIMIT 1"));
            if (existing != null) {
                racketId = existing.getId();
            } else {
                Racket newRacket = new Racket();
                String brand = racketModel.split(" ")[0];
                // 简单智能匹配品牌
                if (racketModel.toUpperCase().contains("YONEX") || racketModel.toUpperCase().startsWith("YY")) brand = "YONEX";
                else if (racketModel.contains("VICTOR") || racketModel.contains("胜利")) brand = "VICTOR";
                else if (racketModel.contains("LI-NING") || racketModel.contains("李宁")) brand = "LI-NING";
                newRacket.setBrand(brand);
                newRacket.setModel(racketModel);
                racketService.save(newRacket);
                racketId = newRacket.getId();
            }
        }
        // 创建订单
        String orderNo = "SO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + new Random().nextInt(9000) + 1000;
        StringingOrder order = new StringingOrder();
        order.setOrderNo(orderNo);
        order.setShortOrderNo(generateShortOrderNo());
        order.setShopId(shopId);
        order.setPlayerId(playerId);
        order.setRacketId(racketId);
        order.setMainStringId(mainStringId);
        order.setMainTension(mainTension);
        order.setPlayerPhone(phone);
        order.setRemark(remark);
        order.setStatus(0);
        order.setSource("customer_register");
        // 关键修复：根据店铺动态获取租户ID，防止数据串台
        Shop shop = shopService.getById(shopId);
        if (shop != null) {
            order.setTenantId(shop.getTenantId());
        } else {
            return Result.error("店铺不存");
        }
        orderService.save(order);
        
        // 库存预留 (V1.3.1 修复)
        tryReserveStock(order);
        
        // 推送打印通知（可选）
        try {
            printSender.sendPrintData(order);
        } catch (Exception e) {
            // 打印失败不影响订单创建
        }
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("id", order.getId());
        return Result.success(result);
    }

    @Operation(summary = "订单进度查询（公开接口-移动端用）")
    @GetMapping("/query")
    public Result<Object> query(@RequestParam(required = false) String orderNo,
                                 @RequestParam(required = false) String phone) {
        if (!StringUtils.hasText(orderNo) && !StringUtils.hasText(phone)) {
            return Result.error("请提供订单号或手机号");
        }
        LambdaQueryWrapper<StringingOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(orderNo)) {
            wrapper.eq(StringingOrder::getOrderNo, orderNo).or().eq(StringingOrder::getShortOrderNo, orderNo);
        }
        if (StringUtils.hasText(phone)) {
            wrapper.eq(StringingOrder::getPlayerPhone, phone);
        }
        wrapper.orderByDesc(StringingOrder::getCreatedAt).last("LIMIT 10");
        List<StringingOrder> orders = orderService.list(wrapper);
        // 补充关联名称
        for (StringingOrder o : orders) {
            if (o.getShopId() != null) {
                Shop s = shopService.getById(o.getShopId());
                if (s != null) o.setShopName(s.getName());
            }
            if (o.getPlayerId() != null) {
                Player p = playerService.getById(o.getPlayerId());
                if (p != null) o.setPlayerName(p.getName());
            }
        }
        if (StringUtils.hasText(orderNo) && !orders.isEmpty()) {
            return Result.success(orders.get(0)); // 按订单号返回单条
        }
        return Result.success(orders); // 按手机号返回列表
    }

    @Operation(summary = "分页查询订单")
    @GetMapping("/page")
    public Result<Page<StringingOrder>> page(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(required = false) Long shopId,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String startDate,
                                              @RequestParam(required = false) String endDate,
                                              @RequestParam(required = false) String sportType) {
        LambdaQueryWrapper<StringingOrder> wrapper = new LambdaQueryWrapper<>();
        if (!TenantContext.isSuperAdmin()) {
            wrapper.eq(StringingOrder::getTenantId, TenantContext.getTenantId());
        }
        if (StringUtils.hasText(sportType)) {
            wrapper.eq(StringingOrder::getSportType, sportType);
        }
        if (shopId != null) {
            wrapper.eq(StringingOrder::getShopId, shopId);
        }
        if (status != null) {
            wrapper.eq(StringingOrder::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(StringingOrder::getOrderNo, keyword);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(StringingOrder::getCreatedAt, startDate + " 00:00:00");
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(StringingOrder::getCreatedAt, endDate + " 23:59:59");
        }
        wrapper.orderByDesc(StringingOrder::getId);
        Page<StringingOrder> result = orderService.page(new Page<>(page, size), wrapper);
        // 补充关联名称
        for (StringingOrder o : result.getRecords()) {
            if (o.getShopId() != null) {
                Shop s = shopService.getById(o.getShopId());
                if (s != null) o.setShopName(s.getName());
            }
            if (o.getPlayerId() != null) {
                Player p = playerService.getById(o.getPlayerId());
                if (p != null) o.setPlayerName(p.getName());
            }
            if (o.getRacketId() != null) {
                Racket r = racketService.getById(o.getRacketId());
                if (r != null) o.setRacketName(r.getBrand() + " " + r.getModel());
            }
        }
        return Result.success(result);
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    public Result<StringingOrder> get(@PathVariable Long id) {
        StringingOrder o = orderService.getById(id);
        if (o != null) {
            if (o.getShopId() != null) {
                Shop s = shopService.getById(o.getShopId());
                if (s != null) o.setShopName(s.getName());
            }
            if (o.getPlayerId() != null) {
                Player p = playerService.getById(o.getPlayerId());
                if (p != null) o.setPlayerName(p.getName());
            }
            if (o.getRacketId() != null) {
                Racket r = racketService.getById(o.getRacketId());
                if (r != null) o.setRacketName(r.getBrand() + " " + r.getModel());
            }
            if (o.getMainStringId() != null) o.setMainStringName(getStringName(o.getMainStringId()));
            if (o.getCrossStringId() != null) o.setCrossStringName(getStringName(o.getCrossStringId()));
        }
        return Result.success(o);
    }

    @SaCheckPermission("order:edit")
    @Operation(summary = "编辑订单")
    @PutMapping
    public Result<Void> update(@RequestBody StringingOrder order) {
        checkShopPermission(order.getShopId());
        orderService.updateById(order);
        return Result.success();
    }

    @SaCheckPermission("order:delete")
    @Operation(summary = "删除订单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        StringingOrder order = orderService.getById(id);
        if (order != null) {
            checkShopPermission(order.getShopId());
        }
        orderService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "开始穿线 (状态流转: 待穿→穿线中)")
    @PutMapping("/{id}/start")
    public Result<Void> startStringing(@PathVariable Long id) {
        StringingOrder order = orderService.getById(id);
        if (order == null) {
            return Result.error("订单不存");
        }
        checkShopPermission(order.getShopId());
        if (order.getStatus() != 0) {
            return Result.error("只有待穿订单才能开始穿线，当前状态: " + order.getStatus());
        }
        StringingOrder update = new StringingOrder();
        update.setId(id);
        update.setStatus(1); // 1 = 穿线中
        // 如果未指定穿线师，自动绑定当前操作人
        if (order.getStringerId() == null) {
            update.setStringerId(TenantContext.getUserId());
        }
        orderService.updateById(update);
        log.info("开始穿线: orderId={}, orderNo={}", id, order.getOrderNo());
        return Result.success();
    }

    @SaCheckPermission("order:complete")
    @Operation(summary = "确认完成(结账) - 扣库存记录支付+算提成")
    @PostMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id,
                              @RequestBody(required = false) Map<String, Object> params) {
        StringingOrder order = orderService.getById(id);
        if (order == null) {
            return Result.error("订单不存");
        }
        checkShopPermission(order.getShopId());
        BigDecimal payAmount = null;
        String payMethod = null;
        if (params != null) {
            if (params.containsKey("payAmount")) {
                payAmount = new BigDecimal(params.get("payAmount").toString());
            }
            if (params.containsKey("payMethod")) {
                payMethod = params.get("payMethod").toString();
            }
        }
        try {
            orderService.completeOrder(id, payAmount, payMethod);
        } catch (BizException e) {
            log.error("订单完成失败: orderId={}, reason={}", id, e.getMessage());
            return Result.error(e.getMessage());
        }
        return Result.success();
    }

    @SaCheckPermission("order:print")
    @Operation(summary = "打印订单(发送到C#客户端)")
    @PostMapping("/{id}/print")
    public Result<Object> print(@PathVariable Long id) {
        StringingOrder order = orderService.getById(id);
        if (order == null) return Result.error("订单不存");
        checkShopPermission(order.getShopId());
        try {
            printSender.sendPrintData(order);
            return Result.success("打印任务已发");
        } catch (Exception e) {
            return Result.error("推送打印失败: " + e.getMessage());
        }
    }

    @Operation(summary = "转单 (修改穿线师)")
    @PutMapping("/{id}/assign")
    public Result<Void> reassignStringer(@PathVariable Long id, @RequestBody Map<String, Long> params) {
        StringingOrder order = orderService.getById(id);
        if (order == null) return Result.error("订单不存");
        checkShopPermission(order.getShopId());
        Long stringerId = params.get("stringerId");
        if (stringerId == null) return Result.error("未指定穿线师");
        
        order.setStringerId(stringerId);
        orderService.updateById(order);
        return Result.success();
    }

    @SaCheckPermission("order:export")
    @Operation(summary = "导出Excel")
    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) Long shopId,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) throws Exception {
        LambdaQueryWrapper<StringingOrder> wrapper = new LambdaQueryWrapper<>();
        if (!TenantContext.isSuperAdmin()) {
            wrapper.eq(StringingOrder::getTenantId, TenantContext.getTenantId());
        }
        if (shopId != null) wrapper.eq(StringingOrder::getShopId, shopId);
        if (status != null) wrapper.eq(StringingOrder::getStatus, status);
        if (StringUtils.hasText(keyword)) wrapper.like(StringingOrder::getOrderNo, keyword);
        if (StringUtils.hasText(startDate)) wrapper.ge(StringingOrder::getCreatedAt, startDate + " 00:00:00");
        if (StringUtils.hasText(endDate)) wrapper.le(StringingOrder::getCreatedAt, endDate + " 23:59:59");
        wrapper.orderByDesc(StringingOrder::getId);
        List<StringingOrder> list = orderService.list(wrapper);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = URLEncoder.encode("订单导出_" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        StringBuilder sb = new StringBuilder();
        sb.append("订单店铺,球员,球拍,主线,主线磅数,横线,横线磅数,总价,状态备注,创建时间\n");
        for (StringingOrder o : list) {
            String shopName = o.getShopId() != null ? shopService.getById(o.getShopId()).getName() : "";
            String playerName = o.getPlayerId() != null ? playerService.getById(o.getPlayerId()).getName() : "";
            String racketName = o.getRacketId() != null ? racketService.getById(o.getRacketId()).getBrand() + " " + racketService.getById(o.getRacketId()).getModel() : "";
            String mainString = o.getMainStringId() != null ? getStringName(o.getMainStringId()) : "";
            String crossString = o.getCrossStringId() != null ? getStringName(o.getCrossStringId()) : "";
            String statusText = o.getStatus() == 1 ? "已穿" : o.getStatus() == 2 ? "已取" : "待穿";
            sb.append(o.getOrderNo()).append(",")
                    .append(shopName).append(",")
                    .append(playerName).append(",")
                    .append(racketName).append(",")
                    .append(mainString).append(",")
                    .append(o.getMainTension()).append(",")
                    .append(crossString).append(",")
                    .append(o.getCrossTension() != null ? o.getCrossTension() : "").append(",")
                    .append(o.getTotalPrice()).append(",")
                    .append(statusText).append(",")
                    .append(o.getRemark() != null ? o.getRemark() : "").append(",")
                    .append(o.getCreatedAt()).append("\n");
        }
        response.getWriter().write(sb.toString());
    }
    
    /** 生成简短可读的展示单号 */
    private String generateShortOrderNo() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("MMdd")) + String.format("%04d", new Random().nextInt(10000));
    }
    
    /** V1.3.1 库存预留逻辑 (带容错) */
    private void tryReserveStock(StringingOrder order) {
        try {
            if (order.getMainStringId() != null) {
                stockService.reserveStock(order.getShopId(), order.getMainStringId(), 1, order.getId(), order.getOrderNo());
            }
            if (order.getCrossStringId() != null) {
                stockService.reserveStock(order.getShopId(), order.getCrossStringId(), 1, order.getId(), order.getOrderNo());
            }
        } catch (Exception e) {
            log.warn("库存预留失败 (不影响接单): orderNo={}, msg={}", order.getOrderNo(), e.getMessage());
            // 预留失败不阻断接单流程，但记录日志供后续排查
        }
    }
}
