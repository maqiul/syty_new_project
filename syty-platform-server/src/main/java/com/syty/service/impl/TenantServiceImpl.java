package com.syty.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.Tenant;
import com.syty.mapper.TenantMapper;
import com.syty.service.TenantService;
import org.springframework.stereotype.Service;
@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {
}
