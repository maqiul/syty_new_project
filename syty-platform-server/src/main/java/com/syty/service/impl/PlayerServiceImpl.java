package com.syty.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.Player;
import com.syty.mapper.PlayerMapper;
import com.syty.service.PlayerService;
import org.springframework.stereotype.Service;
@Service
public class PlayerServiceImpl extends ServiceImpl<PlayerMapper, Player> implements PlayerService {
}
