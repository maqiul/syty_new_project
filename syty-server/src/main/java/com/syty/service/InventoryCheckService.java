package com.syty.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syty.entity.InventoryCheck;
import com.syty.entity.InventoryCheckItem;

import java.util.List;

public interface InventoryCheckService extends IService<InventoryCheck> {

    /** 创建盘点单 */
    InventoryCheck createCheck(Long tenantId, Long shopId);

    /** 批量录入实盘数 */
    void submitItems(Long checkId, List<InventoryCheckItem> items);

    /** 确认盘点并更新库存 */
    void confirmCheck(Long checkId);

    /** 分页查询 */
    IPage<InventoryCheck> pageCheck(Page<InventoryCheck> page, Long tenantId);
}
