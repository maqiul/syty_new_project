package com.syty.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.Racket;
import com.syty.mapper.RacketMapper;
import com.syty.service.RacketService;
import org.springframework.stereotype.Service;
@Service
public class RacketServiceImpl extends ServiceImpl<RacketMapper, Racket> implements RacketService {
}
