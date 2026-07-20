package com.syty.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.TennisPlayer;
import com.syty.mapper.TennisPlayerMapper;
import com.syty.service.TennisPlayerService;
import org.springframework.stereotype.Service;
@Service
public class TennisPlayerServiceImpl extends ServiceImpl<TennisPlayerMapper, TennisPlayer> implements TennisPlayerService {}
