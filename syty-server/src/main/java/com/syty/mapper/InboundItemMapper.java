package com.syty.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.InboundItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入库单明细 Mapper
 */
@Mapper
public interface InboundItemMapper extends BaseMapper<InboundItem> {
}
