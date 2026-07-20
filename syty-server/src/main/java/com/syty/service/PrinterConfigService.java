package com.syty.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.syty.entity.PrinterConfig;
import java.util.List;
/**
 * 打印机配字Service
 */
public interface PrinterConfigService extends IService<PrinterConfig> {
    /**
     * 根据机器码查询该机器所有打印机配置
     */
    List<PrinterConfig> listByMachineId(String machineId);
    /**
     * 更新打印机状态（在线/离线字
     */
    void updateStatus(String machineId, String printerName, Integer status);
}
