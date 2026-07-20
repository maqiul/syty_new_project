package com.syty.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.Shop;
import com.syty.mapper.ShopMapper;
import com.syty.service.ShopService;
import org.springframework.stereotype.Service;
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements ShopService {
}
