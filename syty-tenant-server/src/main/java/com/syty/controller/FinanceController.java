package com.syty.controller;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.common.Result;
import com.syty.entity.PaymentRecord;
import com.syty.entity.StringingOrder;
import com.syty.mapper.PaymentRecordMapper;
import com.syty.mapper.StringingOrderMapper;
import com.syty.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "财务管理")
@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {
    private final StringingOrderMapper orderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final PaymentService paymentService;

    @Operation(summary = "财务统计")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> result = new HashMap<>();
        LambdaQueryWrapper<StringingOrder> base = new LambdaQueryWrapper<>();
        base.eq(StringingOrder::getPayStatus, "PAID");
        List<StringingOrder> paidOrders = orderMapper.selectList(base);
        BigDecimal todayRev = paidOrders.stream()
            .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().equals(java.time.LocalDate.now()))
            .map(o -> o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthRev = paidOrders.stream()
            .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().getMonth() == java.time.LocalDate.now().getMonth())
            .map(o -> o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<StringingOrder> creditOrders = orderMapper.selectList(
            new LambdaQueryWrapper<StringingOrder>().eq(StringingOrder::getPayStatus, "CREDIT"));
        BigDecimal onAccount = creditOrders.stream()
            .map(o -> {
                BigDecimal total = o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO;
                BigDecimal paid = o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO;
                return total.subtract(paid);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long debtCount = creditOrders.stream().map(StringingOrder::getCustomerId).distinct().count();
        result.put("todayRevenue", todayRev);
        result.put("monthRevenue", monthRev);
        result.put("onAccountTotal", onAccount);
        result.put("debtPlayerCount", debtCount);
        return Result.success(result);
    }

    @Operation(summary = "客户欠款汇总")
    @GetMapping("/debt/summary")
    public Result<Map<String, Object>> debtSummary(@RequestParam(required = false) String keyword,
                                                    @RequestParam(defaultValue = "1") int current,
                                                    @RequestParam(defaultValue = "10") int size) {
        LambdaQueryWrapper<StringingOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(StringingOrder::getPayStatus, "CREDIT", "UNPAID", "PARTIAL");
        List<StringingOrder> debtOrders = orderMapper.selectList(wrapper);
        if (keyword != null && !keyword.isEmpty()) {
            debtOrders = debtOrders.stream()
                .filter(o -> (o.getPlayerName() != null && o.getPlayerName().contains(keyword))
                          || (o.getPlayerPhone() != null && o.getPlayerPhone().contains(keyword)))
                .collect(Collectors.toList());
        }
        Map<Long, Map<String, Object>> playerMap = new LinkedHashMap<>();
        for (StringingOrder o : debtOrders) {
            Long pid = o.getCustomerId() != null ? o.getCustomerId() : 0L;
            BigDecimal total = o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO;
            BigDecimal paid = o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal debt = total.subtract(paid);
            if (debt.compareTo(BigDecimal.ZERO) <= 0) continue;
            playerMap.computeIfAbsent(pid, k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("playerId", pid);
                m.put("playerName", o.getPlayerName() != null ? o.getPlayerName() : "");
                m.put("playerPhone", o.getPlayerPhone() != null ? o.getPlayerPhone() : "");
                m.put("totalDebt", BigDecimal.ZERO);
                m.put("debtOrderCount", 0);
                return m;
            });
            Map<String, Object> m = playerMap.get(pid);
            m.put("totalDebt", ((BigDecimal) m.get("totalDebt")).add(debt));
            m.put("debtOrderCount", ((Integer) m.get("debtOrderCount")) + 1);
        }
        List<Map<String, Object>> records = new ArrayList<>(playerMap.values());
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", records.size());
        return Result.success(result);
    }

    @Operation(summary = "客户欠款明细")
    @GetMapping("/debt")
    public Result<Map<String, Object>> debtDetail(@RequestParam Long playerId) {
        LambdaQueryWrapper<StringingOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringingOrder::getCustomerId, playerId)
               .in(StringingOrder::getPayStatus, "CREDIT", "UNPAID", "PARTIAL");
        List<StringingOrder> orders = orderMapper.selectList(wrapper);
        List<Map<String, Object>> debtOrders = new ArrayList<>();
        for (StringingOrder o : orders) {
            BigDecimal total = o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO;
            BigDecimal paid = o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal debt = total.subtract(paid);
            if (debt.compareTo(BigDecimal.ZERO) <= 0) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("orderId", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("totalPrice", total);
            m.put("paidAmount", paid);
            m.put("debtAmount", debt);
            m.put("createdAt", o.getCreatedAt());
            debtOrders.add(m);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("playerId", playerId);
        result.put("debtOrders", debtOrders);
        return Result.success(result);
    }

    @Operation(summary = "客户还款")
    @PostMapping("/repay")
    public Result<Void> repay(@RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(body.get("orderId").toString());
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String payMethod = body.getOrDefault("payMethod", "CASH").toString();
        paymentService.recordPayment(orderId, amount, payMethod);
        return Result.success();
    }

    @Operation(summary = "支付记录查询")
    @GetMapping("/payment-record")
    public Result<List<PaymentRecord>> paymentRecords(@RequestParam Long orderId) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRecord::getOrderId, orderId).orderByDesc(PaymentRecord::getCreatedAt);
        return Result.success(paymentRecordMapper.selectList(wrapper));
    }
}
