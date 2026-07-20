package com.syty.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.PrintPolicyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 打印策略 Mapper (V1.4 新增)
 */
@Mapper
public interface PrintPolicyMapper extends BaseMapper<PrintPolicyEntity> {

    /**
     * 按优先级查询最佳匹配策略 (V1.5 改造: 支持店铺专属 + 公共降级)
     */
    @Select("SELECT * FROM sys_print_policy " +
            "WHERE status = 1 " +
            "AND (shop_id = #{shopId} OR shop_id IS NULL) " +
            "AND (scene = #{scene} OR scene = 'ALL') " +
            "AND (sport_type = #{sportType} OR sport_type = 'ALL') " +
            "AND doc_type = #{docType} " +
            "ORDER BY " +
            "  (CASE WHEN shop_id = #{shopId} THEN 0 ELSE 1 END), " +
            "  (CASE WHEN scene = #{scene} THEN 1 ELSE 0 END) + " +
            "  (CASE WHEN sport_type = #{sportType} THEN 1 ELSE 0 END) DESC, " +
            "  priority DESC " +
            "LIMIT 1")
    PrintPolicyEntity findBestMatch(
            @Param("shopId") Long shopId,
            @Param("scene") String scene,
            @Param("sportType") String sportType,
            @Param("docType") String docType
    );
}
