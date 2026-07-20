package com.syty.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syty.entity.PrinterConfig;
import com.syty.mapper.PrinterConfigMapper;
import com.syty.service.PrinterConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
/**
 * 打印机配字Service 实现
 */
@Slf4j
@Service
public class PrinterConfigServiceImpl extends ServiceImpl<PrinterConfigMapper, PrinterConfig> implements PrinterConfigService {
    @Override
    public List<PrinterConfig> listByMachineId(String machineId) {
        return lambdaQuery()
                .eq(PrinterConfig::getMachineId, machineId)
                .list();
    }
    @Override
    public void updateStatus(String machineId, String printerName, Integer status) {
        lambdaUpdate()
                .eq(PrinterConfig::getMachineId, machineId)
                .eq(PrinterConfig::getPrinterName, printerName)
                .set(PrinterConfig::getStatus, status)
                .set(PrinterConfig::getUpdatedAt, LocalDateTime.now())
                .update();
        log.info("更新打印机状态: machineId={}, printerName={}, status={}", machineId, printerName, status);
    }
}
