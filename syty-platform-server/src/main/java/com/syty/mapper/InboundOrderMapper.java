package com.syty.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.InboundOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入库单主表 Mapper
 */
@Mapper
public interface InboundOrderMapper extends BaseMapper<InboundOrder> {
}
