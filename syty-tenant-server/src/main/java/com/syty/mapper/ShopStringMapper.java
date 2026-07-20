package com.syty.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.ShopString;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
@Mapper
public interface ShopStringMapper extends BaseMapper<ShopString> {
    /**
     * 查询并加行锁（FOR UPDATE），防并发超卖
     */
    @Select("SELECT id, tenant_id, shop_id, string_id, price, stock, reserved_quantity, min_stock_alert, " +
            "updated_by, deleted, created_at, updated_at " +
            "FROM shop_string WHERE shop_id = #{shopId} AND string_id = #{stringId} AND deleted = 0 FOR UPDATE")
    ShopString selectForUpdate(@Param("shopId") Long shopId, @Param("stringId") Long stringId);
    /**
     * 带条件扣减库存（防止并发超卖）
     */
    @Update("UPDATE shop_string SET stock = stock + #{delta}, " +
            "updated_by = #{operatorId}, updated_at = NOW() " +
            "WHERE id = #{id} AND deleted = 0 AND stock + #{delta} >= 0")
    int updateStockWithCheck(@Param("id") Long id,
                              @Param("delta") int delta,
                              @Param("operatorId") Long operatorId);

    /**
     * 更新预留库存
     */
    @Update("UPDATE shop_string SET reserved_quantity = reserved_quantity + #{delta}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id} AND deleted = 0 AND reserved_quantity + #{delta} >= 0")
    int updateReservedStock(@Param("id") Long id, @Param("delta") int delta);
}