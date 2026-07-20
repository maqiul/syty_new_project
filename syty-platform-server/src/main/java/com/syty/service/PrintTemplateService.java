package com.syty.service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.PrintTemplate;
import com.syty.mapper.PrintTemplateMapper;
import org.springframework.stereotype.Service;
@Service
public class PrintTemplateService extends ServiceImpl<PrintTemplateMapper, PrintTemplate> {
    /**
     * 获取当前租户的默认模板，如果没有则创建默认配字
     */
    public PrintTemplate getDefaultTemplate(Long tenantId) {
        PrintTemplate tpl = lambdaQuery()
                .eq(PrintTemplate::getTenantId, tenantId)
                .eq(PrintTemplate::getIsDefault, 1)
                .one();
        if (tpl == null) {
            tpl = createDefault(tenantId);
        }
        return tpl;
    }
    private PrintTemplate createDefault(Long tenantId) {
        PrintTemplate tpl = new PrintTemplate();
        tpl.setTenantId(tenantId);
        tpl.setName("默认模板");
        tpl.setPaperWidth(80);
        tpl.setPaperHeight(50);
        tpl.setFontSize(10);
        tpl.setShowLogo(0);
        tpl.setShowQrcode(1);
        tpl.setIsDefault(1);
        String defaultFields = """
            [
                {"label":"店铺名称","key":"shopName","fontSize":14,"bold":true,"align":"center"},
                {"label":"订单","key":"orderNo","fontSize":12,"bold":true,"align":"left"},
                {"label":"球员","key":"playerName","fontSize":10,"bold":false,"align":"left"},
                {"label":"球拍","key":"racket","fontSize":10,"bold":false,"align":"left"},
                {"label":"主线","key":"mainString","fontSize":10,"bold":false,"align":"left"},
                {"label":"磅数","key":"tension","fontSize":10,"bold":false,"align":"left"},
                {"label":"总价","key":"totalPrice","fontSize":12,"bold":true,"align":"left"},
                {"label":"日期","key":"date","fontSize":9,"bold":false,"align":"left"}
            ]
            """.stripIndent();
        tpl.setFieldsJson(defaultFields);
        save(tpl);
        return tpl;
    }
}
