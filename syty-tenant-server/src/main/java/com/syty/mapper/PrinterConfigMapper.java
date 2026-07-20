package com.syty.mapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.PrinterConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface PrinterConfigMapper extends BaseMapper<PrinterConfig> {

    @Select("SELECT " +
            "pc.id as pc_id, pc.shop_id, pc.machine_id, pc.printer_name, " +
            "pc.is_default, pc.status as pc_status, " +
            "pr.id as pr_id, pr.doc_type, pr.printer_name as rule_printer, " +
            "pr.template_path, pr.paper_size, pr.copies, pr.is_enabled as rule_enabled, " +
            "res.id as res_id, res.resource_key, res.resource_url, res.status as res_status " +
            "FROM printer_config pc " +
            "LEFT JOIN print_rule pr ON pr.shop_id = pc.shop_id " +
            "    AND pr.machine_id = pc.machine_id " +
            "    AND pr.tenant_id = #{tenantId} " +
            "    AND pr.is_enabled = 1 " +
            "LEFT JOIN print_resource res ON (res.shop_id = pc.shop_id OR res.shop_id = 0) " +
            "    AND res.tenant_id = #{tenantId} " +
            "    AND res.status = 1 " +
            "WHERE pc.shop_id = #{shopId} AND pc.machine_id = #{machineId} AND pc.status = 1 " +
            "ORDER BY pc.is_default DESC, pc.created_at ASC")
    List<Map<String, Object>> selectPrintSetupWithJoin(@Param("shopId") String shopId,
                                                        @Param("machineId") String machineId,
                                                        @Param("tenantId") Long tenantId);
}
