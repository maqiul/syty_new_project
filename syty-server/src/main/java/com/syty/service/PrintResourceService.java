package com.syty.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.syty.entity.PrintResource;
import java.util.List;
import java.util.Map;
/**
 * 打印资源 Service
 */
public interface PrintResourceService extends IService<PrintResource> {
    /**
     * 获取店铺可用的所有资源（含全局资源字
     */
    List<PrintResource> listAvailableResources(Long shopId);
    /**
     * 字key-value 形式返回资源映射
     */
    Map<String, String> getResourceMap(Long shopId);
}
