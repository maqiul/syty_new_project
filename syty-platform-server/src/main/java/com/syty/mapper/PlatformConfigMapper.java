package com.syty.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.PlatformConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PlatformConfigMapper extends BaseMapper<PlatformConfig> {
    @Update("UPDATE platform_config SET config_version = config_version + 1, updated_at = NOW() WHERE id = #{id}")
    void incrementVersion(@Param("id") Long id);
}
