package com.syty.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.StringerWorkLog;
import org.apache.ibatis.annotations.Select;
import com.syty.dto.StringerRankDTO;
import java.util.List;

public interface StringerWorkLogMapper extends BaseMapper<StringerWorkLog> {
    
    @Select("SELECT stringer_id AS stringerId, stringer_name AS stringerName, " +
            "COUNT(*) AS orderCount, SUM(fee) AS totalFee, AVG(duration) AS avgDuration " +
            "FROM stringer_work_log " +
            "WHERE tenant_id = #{tenantId} " +
            "GROUP BY stringer_id, stringer_name " +
            "ORDER BY orderCount DESC LIMIT #{limit}")
    List<StringerRankDTO> selectRankByTenant(Long tenantId, int limit);
}
