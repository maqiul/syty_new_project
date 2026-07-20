package com.syty.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.StringInfo;
import com.syty.mapper.StringInfoMapper;
import com.syty.service.StringInfoService;
import org.springframework.stereotype.Service;
@Service
public class StringInfoServiceImpl extends ServiceImpl<StringInfoMapper, StringInfo> implements StringInfoService {
}
