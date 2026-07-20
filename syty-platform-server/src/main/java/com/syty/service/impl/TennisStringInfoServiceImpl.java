package com.syty.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.TennisStringInfo;
import com.syty.mapper.TennisStringInfoMapper;
import com.syty.service.TennisStringInfoService;
import org.springframework.stereotype.Service;
@Service
public class TennisStringInfoServiceImpl extends ServiceImpl<TennisStringInfoMapper, TennisStringInfo> implements TennisStringInfoService {}
