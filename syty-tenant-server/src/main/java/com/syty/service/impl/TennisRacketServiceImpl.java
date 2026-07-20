package com.syty.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.TennisRacket;
import com.syty.mapper.TennisRacketMapper;
import com.syty.service.TennisRacketService;
import org.springframework.stereotype.Service;
@Service
public class TennisRacketServiceImpl extends ServiceImpl<TennisRacketMapper, TennisRacket> implements TennisRacketService {}
