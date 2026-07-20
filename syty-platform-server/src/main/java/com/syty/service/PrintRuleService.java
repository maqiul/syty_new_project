package com.syty.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.syty.entity.PrintRule;
import java.util.List;
/**
 * 打印规则 Service
 */
public interface PrintRuleService extends IService<PrintRule> {
    /**
     * 根据店铺和单据类型查询匹配的打印规则
     */
    List<PrintRule> listByDocType(Long shopId, String docType);
}
