package com.syty.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.StockLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
/**
 * 库存流水 Mapper
 */
@Mapper
public interface StockLogMapper extends BaseMapper<StockLog> {
    /**
     * 带行锁查询店铺线材库存（防止并发扣减字
     */
    @Select("SELECT * FROM shop_string WHERE shop_id = #{shopId} AND string_id = #{stringId} FOR UPDATE")
    com.syty.entity.ShopString selectForUpdate(@Param("shopId") Long shopId, @Param("stringId") Long stringId);
    /**
     * 更新库存（带条件防止超卖字
     */
    @Update("UPDATE shop_string SET stock = stock + #{quantity}, updated_at = NOW(), updated_by = #{updatedBy} " +
            "WHERE id = #{id} AND stock + #{quantity} >= 0")
    int updateStockWithCheck(@Param("id") Long id, @Param("quantity") Integer quantity, @Param("updatedBy") Long updatedBy);
}
