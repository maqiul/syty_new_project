package com.syty.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.PunchCard;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PunchCardMapper extends BaseMapper<PunchCard> {
    // BaseMapper provides most needed methods. 
    // Optimistic lock is handled by MyBatis Plus interceptor usually, 
    // but for manual SQL we can add methods here if needed.
}
