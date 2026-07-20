package com.syty.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.TennisOrder;
import com.syty.mapper.TennisOrderMapper;
import com.syty.service.TennisOrderService;
import org.springframework.stereotype.Service;
@Service
public class TennisOrderServiceImpl extends ServiceImpl<TennisOrderMapper, TennisOrder> implements TennisOrderService {}
