using System;
using System.Globalization;
using System.Windows.Data;
using System.Windows.Media;
using SytyPrintClient.Services;

namespace SytyPrintClient.Converters;

/// <summary>
/// 日志级别 → 颜色转换器
/// </summary>
public class LogLevelToColorConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        if (value is LogLevel level)
        {
            return level switch
            {
                LogLevel.Info => new SolidColorBrush(Color.FromRgb(30, 144, 255)),       // 蓝色
                LogLevel.Success => new SolidColorBrush(Color.FromRgb(34, 197, 94)),     // 绿色
                LogLevel.Warning => new SolidColorBrush(Color.FromRgb(255, 165, 0)),     // 橙色
                LogLevel.Error => new SolidColorBrush(Color.FromRgb(239, 68, 68)),      // 红色
                LogLevel.Debug => new SolidColorBrush(Color.FromRgb(156, 163, 175)),    // 灰色
                _ => new SolidColorBrush(Colors.Black)
            };
        }
        return new SolidColorBrush(Colors.Black);
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        => throw new NotImplementedException();
}
