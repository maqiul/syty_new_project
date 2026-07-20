package com.syty.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.StockLog;
import com.syty.mapper.StockLogMapper;
import com.syty.service.StockLogService;
import org.springframework.stereotype.Service;
@Service
public class StockLogServiceImpl extends ServiceImpl<StockLogMapper, StockLog> implements StockLogService {
}
