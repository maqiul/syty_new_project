using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

namespace SytyPrintClient.Services
{
    /// <summary>
    /// 打印机本地过滤服务
    /// 逻辑：读取 printers.txt，只上报文件中存在的打印机。
    /// 如果文件不存在，则自动生成包含当前所有打印机的模板。
    /// </summary>
    public class PrinterFilterService
    {
        private readonly string _configPath;

        public PrinterFilterService()
        {
            // 配置文件路径：程序运行目录下的 printers.txt
            _configPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "printers.txt");
        }

        /// <summary>
        /// 获取过滤后的打印机列表
        /// </summary>
        /// <param name="allPrinters">系统扫描到的所有打印机</param>
        /// <returns>白名单允许的打印机</returns>
        public List<string> GetFilteredPrinters(List<string> allPrinters)
        {
            // 1. 首次运行：文件不存在
            if (!File.Exists(_configPath))
            {
                try
                {
                    // 生成模板：把当前所有打印机写入文件（一行一个）
                    File.WriteAllLines(_configPath, allPrinters);
                    // 首次运行全量上报，方便用户初次配置
                    return allPrinters;
                }
                catch (Exception)
                {
                    // 写入失败（如权限问题），回退到全量上报
                    return allPrinters;
                }
            }

            // 2. 文件存在：加载白名单
            var whitelist = File.ReadAllLines(_configPath)
                                .Select(line => line.Trim())
                                .Where(line => !string.IsNullOrEmpty(line))
                                .ToList();

            // 3. 过滤：只保留在白名单里的打印机 (忽略大小写)
            var result = allPrinters
                .Where(p => whitelist.Contains(p, StringComparer.OrdinalIgnoreCase))
                .ToList();

            return result;
        }
    }
}
