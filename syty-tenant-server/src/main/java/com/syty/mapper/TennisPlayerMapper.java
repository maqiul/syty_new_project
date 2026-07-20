package com.syty.mapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.TennisPlayer;
import org.apache.ibatis.annotations.Mapper;
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface TennisPlayerMapper extends BaseMapper<TennisPlayer> {}
