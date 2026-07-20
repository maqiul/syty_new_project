package com.syty.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.PrintResource;
import com.syty.mapper.PrintResourceMapper;
import com.syty.service.PrintResourceService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 打印资源 Service 实现
 */
@Service
public class PrintResourceServiceImpl extends ServiceImpl<PrintResourceMapper, PrintResource> implements PrintResourceService {
    @Override
    public List<PrintResource> listAvailableResources(Long shopId) {
        // 获取全局资源 (shopId=0) + 店铺专属资源
        return lambdaQuery()
                .and(wrapper -> wrapper
                        .eq(PrintResource::getShopId, 0)
                        .or()
                        .eq(PrintResource::getShopId, shopId))
                .list();
    }
    @Override
    public Map<String, String> getResourceMap(Long shopId) {
        return listAvailableResources(shopId).stream()
                .collect(Collectors.toMap(
                        PrintResource::getResourceKey,
                        PrintResource::getResourceUrl,
                        (existing, replacement) -> replacement // 店铺资源覆盖全局
                ));
    }
}
