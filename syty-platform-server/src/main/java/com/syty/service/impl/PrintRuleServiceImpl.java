package com.syty.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.PrintRule;
import com.syty.mapper.PrintRuleMapper;
import com.syty.service.PrintRuleService;
import org.springframework.stereotype.Service;
import java.util.List;
/**
 * 打印规则 Service 实现
 */
@Service
public class PrintRuleServiceImpl extends ServiceImpl<PrintRuleMapper, PrintRule> implements PrintRuleService {
    @Override
    public List<PrintRule> listByDocType(Long shopId, String docType) {
        return lambdaQuery()
                .eq(PrintRule::getShopId, shopId)
                .eq(PrintRule::getDocType, docType)
                .eq(PrintRule::getIsEnabled, true)
                .orderByAsc(PrintRule::getSortOrder)
                .list();
    }
}
