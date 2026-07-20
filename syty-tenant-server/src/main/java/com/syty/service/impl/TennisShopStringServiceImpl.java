package com.syty.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.TennisShopString;
import com.syty.mapper.TennisShopStringMapper;
import com.syty.service.TennisShopStringService;
import org.springframework.stereotype.Service;
@Service
public class TennisShopStringServiceImpl extends ServiceImpl<TennisShopStringMapper, TennisShopString> implements TennisShopStringService {}
