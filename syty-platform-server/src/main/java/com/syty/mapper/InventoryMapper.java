package com.syty.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.InventoryEntity;
import org.apache.ibatis.annotations.Mapper;
/**
 * 库存 Mapper
 */
@Mapper
public interface InventoryMapper extends BaseMapper<InventoryEntity> {
}
