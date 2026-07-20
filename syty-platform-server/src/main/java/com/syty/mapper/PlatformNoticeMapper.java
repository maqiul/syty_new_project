package com.syty.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.PlatformNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface PlatformNoticeMapper extends BaseMapper<PlatformNotice> {
    // 获取租户可见的公告 (public schema query)
    @Select("SELECT * FROM public.platform_notice " +
            "WHERE status = 'PUBLISHED' " +
            "AND (target_type = 'ALL' OR target_ids::jsonb ? #{tenantId}) " +
            "ORDER BY created_at DESC")
    List<PlatformNotice> selectVisibleNotices(@Param("tenantId") String tenantId);
}
