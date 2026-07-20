using System;
using System.Collections.Concurrent;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.IO;
using System.Runtime.CompilerServices;
using System.Threading.Tasks;
using System.Threading;

namespace SytyPrintClient.Services;

/// <summary>
/// 日志级别
/// </summary>
public enum LogLevel
{
    Info,
    Warning,
    Error,
    Success,
    Debug
}

/// <summary>
/// 日志条目
/// </summary>
public class LogEntry : INotifyPropertyChanged
{
    private string _message = "";
    private LogLevel _level = LogLevel.Info;
    private DateTime _time = DateTime.Now;

    public DateTime Time
    {
        get => _time;
        set { _time = value; OnPropertyChanged(); }
    }

    public LogLevel Level
    {
        get => _level;
        set { _level = value; OnPropertyChanged(); }
    }

    public string Message
    {
        get => _message;
        set { _message = value; OnPropertyChanged(); }
    }

    public string TimeFormatted => Time.ToString("HH:mm:ss.fff");

    public event PropertyChangedEventHandler? PropertyChanged;
    protected void OnPropertyChanged([CallerMemberName] string? name = null)
        => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}

/// <summary>
/// 日志服务（单例、线程安全、支持本地文件持久化）
/// </summary>
public class LogService : INotifyPropertyChanged
{
    private static readonly Lazy<LogService> _instance = new(() => new LogService());
    public static LogService Instance => _instance.Value;

    private readonly ObservableCollection<LogEntry> _logs = new();
    private readonly int _maxLogs = 1000;
    private readonly object _fileLock = new object();
    private readonly string _logFilePath;

    public ObservableCollection<LogEntry> Logs => _logs;

    private LogService() 
    {
        // 初始化日志文件路径：程序运行目录下的 logs 文件夹
        string logDir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "logs");
        if (!Directory.Exists(logDir))
        {
            try { Directory.CreateDirectory(logDir); } catch { /* 忽略权限错误 */ }
        }
        // 按天分文件，方便管理
        _logFilePath = Path.Combine(logDir, $"syty-print-{DateTime.Now:yyyyMMdd}.log");
    }

    public void Info(string message) => AddLog(message, LogLevel.Info);
    public void Info(string source, string message) => AddLog($"[{source}] {message}", LogLevel.Info);
    public void Warning(string message) => AddLog(message, LogLevel.Warning);
    public void Warn(string source, string message) => AddLog($"[{source}] {message}", LogLevel.Warning);
    public void Error(string message) => AddLog(message, LogLevel.Error);
    public void Error(string source, string message) => AddLog($"[{source}] {message}", LogLevel.Error);
    public void Success(string message) => AddLog(message, LogLevel.Success);
    public void Success(string source, string message) => AddLog($"[{source}] {message}", LogLevel.Success);
    public void Debug(string message) => AddLog(message, LogLevel.Debug);
    public void Debug(string source, string message) => AddLog($"[{source}] {message}", LogLevel.Debug);

    private void AddLog(string message, LogLevel level)
    {
        App.Current?.Dispatcher.Invoke(() =>
        {
            _logs.Add(new LogEntry
            {
                Message = message,
                Level = level,
                Time = DateTime.Now
            });

            // 限制日志数量
            while (_logs.Count > _maxLogs)
                _logs.RemoveAt(0);
        });

        // 写入本地文件 (后台线程执行)
        Task.Run(() =>
        {
            string logLine = $"{DateTime.Now:yyyy-MM-dd HH:mm:ss.fff} [{level}] {message}";
            try
            {
                lock (_fileLock)
                {
                    // AppendAllText 是线程安全的原子操作，但在 lock 中更稳妥防止并发写入冲突
                    File.AppendAllText(_logFilePath, logLine + Environment.NewLine);
                }
            }
            catch (Exception ex)
            {
                // 文件写入失败不应当影响主流程，仅在控制台输出
                Console.WriteLine($"Failed to write log to file: {ex.Message}");
            }
        });
    }

    public void Clear()
    {
        App.Current?.Dispatcher.Invoke(() => _logs.Clear());
    }

    public event PropertyChangedEventHandler? PropertyChanged;

    protected void OnPropertyChanged([CallerMemberName] string? name = null)
        => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
