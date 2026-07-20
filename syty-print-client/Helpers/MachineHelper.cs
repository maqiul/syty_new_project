using Microsoft.Win32;
using System;

namespace SytyPrintClient.Helpers;

public static class MachineHelper
{
    /// <summary>
    /// 获取 Windows 机器唯一标识 (MachineGuid)
    /// 来源：HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Cryptography
    /// </summary>
    public static string GetMachineId()
    {
        try
        {
            using var key = Registry.LocalMachine.OpenSubKey(@"SOFTWARE\Microsoft\Cryptography");
            return key?.GetValue("MachineGuid")?.ToString() ?? "UNKNOWN_MACHINE";
        }
        catch
        {
            return "ERROR_RETRIEVING_ID";
        }
    }
}
