package com.syty.controller;

import com.syty.common.Result;
import com.syty.common.TenantContext;
import com.syty.entity.Customer;
import com.syty.entity.Player;
import com.syty.entity.StringingOrder;
import com.syty.mapper.CustomerMapper;
import com.syty.mapper.PlayerMapper;
import com.syty.mapper.StringingOrderMapper;
import com.syty.dto.H5OrderCreateRequest;
import com.syty.dto.H5OrderCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * H5 自助登记接口 (V1.6/V1.7)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/h5")
@RequiredArgsConstructor
public class H5V16Controller {

    private final StringingOrderMapper orderMapper;
    private final CustomerMapper customerMapper; // 新增
    private final PlayerMapper playerMapper;     // 保留用于回查历史

    /**
     * 创建订单 (V1.8 修正：完善 Customer 关联)
     */
    @PostMapping("/order")
    public Result<H5OrderCreateResponse> createOrder(@RequestBody H5OrderCreateRequest req) {
        Long tenantId = TenantContext.getTenantId(); 
        if (tenantId == null) tenantId = 1L; 

        // 1. 核心修正：查找或创建 Customer
        Customer customer = customerMapper.selectByPhone(req.getPhone());
        Long customerId = null;
        if (customer == null) {
            customer = new Customer();
            customer.setTenantId(tenantId);
            customer.setPhone(req.getPhone());
            customer.setNickname("H5自助客户");
            customer.setIsMember(0);
            customerMapper.insert(customer);
            log.info("[H5] 自动注册新客户: {}", req.getPhone());
        }
        customerId = customer.getId();

        // 2. 创建订单
        StringingOrder order = new StringingOrder();
        order.setTenantId(tenantId);
        order.setCustomerId(customerId); // 关联到 Customer
        order.setOrderNo("ORD-" + System.currentTimeMillis());
        order.setPlayerPhone(req.getPhone());
        order.setStatus(0); // PENDING
        order.setMainTension(req.getMainPounds());
        order.setCrossTension(req.getCrossPounds());
        
        // 3. 次卡标记
        if (Boolean.TRUE.equals(req.getUsePunchCard()) && req.getPunchCardId() != null) {
            order.setUsePunchCard(true);
            order.setPunchCardId(req.getPunchCardId());
        }

        orderMapper.insert(order);

        H5OrderCreateResponse resp = new H5OrderCreateResponse();
        resp.setOrderId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus("PENDING");
        resp.setWarning(null);

        return Result.success(resp);
    }
}
