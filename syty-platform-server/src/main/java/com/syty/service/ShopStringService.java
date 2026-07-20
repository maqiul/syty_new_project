package com.syty.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.syty.entity.ShopString;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface ShopStringService extends IService<ShopString> {

    /** 预留库存（下单时） */
    void reserveStock(Long shopStringId, int quantity);

    /** 释放预留（取消订单时） */
    void releaseStock(Long shopStringId, int quantity);

    /** 扣减库存（完成订单时） */
    void deductStock(Long shopStringId, int quantity);

    /** 分页查询库存（支持仅看预警） */
    IPage<ShopString> pageStock(Page<ShopString> page, boolean warningOnly, Long tenantId);

    /** 更新盘点后的库存 */
    void updateAfterCheck(Long shopStringId, int actualQuantity);
}
