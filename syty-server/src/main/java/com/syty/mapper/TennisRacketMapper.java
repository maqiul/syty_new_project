package com.syty.mapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.TennisRacket;
import org.apache.ibatis.annotations.Mapper;
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface TennisRacketMapper extends BaseMapper<TennisRacket> {}
