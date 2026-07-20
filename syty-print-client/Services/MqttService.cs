using System;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using MQTTnet;
using MQTTnet.Client;
using MQTTnet.Protocol;

namespace SytyPrintClient.Services;

/// <summary>
/// MQTT订阅服务 (MQTTnet 4.x) - 动态 Topic 分配
/// </summary>
public class MqttService : IDisposable
{
    private IMqttClient? _client;
    private CancellationTokenSource? _cts;
    private bool _isRunning;
    private bool _isStopping;  // 是否正在主动停止（用于 DisconnectedAsync 判断是否重连）
    private string? _lastHost;
    private int _lastPort;
    private string? _lastClientId;
    private string? _lastUsername;
    private string? _lastPassword;
    private string? _currentShopId;
    private string? _currentSubscribeTopic;

    /// <summary>当前已绑定的店铺ID</summary>
    public string? CurrentShopId => _currentShopId;

    public bool IsConnected => _isRunning;

    /// <summary>收到消息时触发</summary>
    public event Action<string>? OnMessageReceived;

    /// <summary>连接状态变化时触发</summary>
    public event Action<bool>? OnConnectionChanged;

    /// <summary>收到shopId时触发</summary>
    public event Action<string>? OnShopIdReceived;

    /// <summary>
    /// 启动连接（不订阅 Topic，等待 ApplyShopId）
    /// </summary>
    public async Task StartAsync(string host, int port, string? clientId = null, string? username = null, string? password = null)
    {
        _isStopping = false;  // 重置停止标志
        await StopAsync();
        _cts = new CancellationTokenSource();

        _lastHost = host;
        _lastPort = port;
        _lastClientId = clientId;
        _lastUsername = username;
        _lastPassword = password;

        try
        {
            var factory = new MqttFactory();
            _client = factory.CreateMqttClient();

            var builder = new MqttClientOptionsBuilder()
                .WithTcpServer(host, port)
                .WithCleanSession();

            if (!string.IsNullOrEmpty(clientId))
                builder.WithClientId(clientId);
            if (!string.IsNullOrEmpty(username))
                builder.WithCredentials(username, password ?? "");

            var options = builder.Build();

            // 连接成功事件
            _client.ConnectedAsync += async (args) =>
            {
                _isRunning = true;
                LogService.Instance.Success("MQTT", $"已连接到 Broker {host}:{port}");
                OnConnectionChanged?.Invoke(true);

                // 如果已经有 shopId，自动重新订阅
                if (!string.IsNullOrEmpty(_currentShopId))
                {
                    await SubscribeTopicAsync(_currentShopId);
                }
            };

            // 断开事件 - 不再自动重连，由用户手动触发
            _client.DisconnectedAsync += (args) =>
            {
                _isRunning = false;
                LogService.Instance.Warn("MQTT", "连接已断开");
                OnConnectionChanged?.Invoke(false);
                return Task.CompletedTask;
            };

            // 消息接收事件
            _client.ApplicationMessageReceivedAsync += ApplicationMessageReceivedAsync;

            // 连接
            await _client.ConnectAsync(options, _cts.Token);
            LogService.Instance.Info("MQTT", "已连接，等待 shopId 分配...");
        }
        catch (Exception ex)
        {
            LogService.Instance.Error("MQTT", $"连接失败: {ex.Message}");
            _isRunning = false;
            throw;
        }
    }

    /// <summary>
    /// 应用 shopId：取消旧订阅 → 订阅新主题 → 发送 ONLINE 消息
    /// </summary>
    public async Task ApplyShopId(string shopId)
    {
        if (string.IsNullOrWhiteSpace(shopId))
        {
            LogService.Instance.Warn("MQTT", "ApplyShopId 收到空 shopId，忽略");
            return;
        }

        _currentShopId = shopId;

        // 如果未连接，仅记录，等连接后自动订阅
        if (_client == null || !_isRunning)
        {
            LogService.Instance.Info("MQTT", $"shopId 已记录: {shopId}，将在连接后自动订阅");
            OnShopIdReceived?.Invoke(shopId);
            return;
        }

        await SubscribeTopicAsync(shopId);

        // 发送 ONLINE 消息到 sender 通道
        await SendOnlineMessageAsync(shopId);

        // 通知外部
        OnShopIdReceived?.Invoke(shopId);
    }

    /// <summary>
    /// 订阅指定 shopId 的主题
    /// </summary>
    private async Task SubscribeTopicAsync(string shopId)
    {
        var newTopic = $"{shopId}/printer";

        // 取消旧订阅
        if (!string.IsNullOrEmpty(_currentSubscribeTopic) && _currentSubscribeTopic != newTopic)
        {
            try
            {
                var unsubscribeOptions = new MqttClientUnsubscribeOptionsBuilder()
                    .WithTopicFilter(_currentSubscribeTopic)
                    .Build();
                await _client!.UnsubscribeAsync(unsubscribeOptions, _cts!.Token);
                LogService.Instance.Info("MQTT", $"已取消订阅: {_currentSubscribeTopic}");
            }
            catch (Exception ex)
            {
                LogService.Instance.Warn("MQTT", $"取消订阅旧主题失败: {ex.Message}");
            }
        }

        // 订阅新主题
        try
        {
            var subscribeOptions = new MqttClientSubscribeOptionsBuilder()
                .WithTopicFilter(newTopic, MqttQualityOfServiceLevel.AtLeastOnce)
                .Build();

            await _client!.SubscribeAsync(subscribeOptions, _cts!.Token);
            _currentSubscribeTopic = newTopic;
            LogService.Instance.Success("MQTT", $"已绑定店铺 {shopId}，订阅主题: {newTopic}");
        }
        catch (Exception ex)
        {
            LogService.Instance.Error("MQTT", $"订阅主题 {newTopic} 失败: {ex.Message}");
        }
    }

    /// <summary>
    /// 向 {shopId}/sender 发送 ONLINE 消息
    /// </summary>
    private async Task SendOnlineMessageAsync(string shopId)
    {
        try
        {
            var onlineMsg = new { type = "ONLINE", status = "ready" };
            var json = JsonSerializer.Serialize(onlineMsg);
            var payload = Encoding.UTF8.GetBytes(json);

            var message = new MqttApplicationMessageBuilder()
                .WithTopic($"{shopId}/sender")
                .WithPayload(payload)
                .WithQualityOfServiceLevel(MqttQualityOfServiceLevel.AtMostOnce)  // QoS 0: 发完就走，不等 PUBACK
                .Build();

            // 加 3 秒超时作为双重保险
            using var timeoutCts = new CancellationTokenSource(TimeSpan.FromSeconds(3));
            await _client!.PublishAsync(message, timeoutCts.Token);
            LogService.Instance.Info("MQTT", $"已发送 ONLINE 消息到 {shopId}/sender");
        }
        catch (OperationCanceledException)
        {
            LogService.Instance.Warn("MQTT", "发送 ONLINE 消息超时 (3s)，QoS 0 模式下仍超时说明 Broker 无响应");
        }
        catch (Exception ex)
        {
            LogService.Instance.Warn("MQTT", $"发送 ONLINE 消息失败: {ex.Message}");
        }
    }

    /// <summary>
    /// 消息接收回调
    /// </summary>
    private Task ApplicationMessageReceivedAsync(MqttApplicationMessageReceivedEventArgs args)
    {
        try
        {
            var payload = Encoding.UTF8.GetString(args.ApplicationMessage.PayloadSegment);
            LogService.Instance.Debug("MQTT", $"收到消息: {payload}");

            // 修复：移除从普通消息中自动提取 shopId 并调用 ApplyShopId 的逻辑
            // 原因：后端下发的打印指令中通常包含 shopId，会导致 ApplyShopId -> OnShopIdReceived -> ApplyShopId 的死循环
            // shopId 应该在初始化阶段由 ApplyShopId 显式设置。

            // 始终触发消息事件（打印指令等走原有流程）
            OnMessageReceived?.Invoke(payload);
        }
        catch (Exception ex)
        {
            LogService.Instance.Error("MQTT", $"处理消息失败: {ex.Message}");
        }

        return Task.CompletedTask;
    }

    /// <summary>
    /// 尝试重新连接
    /// </summary>
    private async Task TryReconnectAsync()
    {
        // ★★★ 重连前最后确认 ★★★
        if (_isStopping || _cts == null || _cts.IsCancellationRequested)
        {
            return;
        }

        try
        {
            if (_client == null || _lastHost == null) return;

            var builder = new MqttClientOptionsBuilder()
                .WithTcpServer(_lastHost, _lastPort)
                .WithCleanSession();

            if (!string.IsNullOrEmpty(_lastClientId))
                builder.WithClientId(_lastClientId);
            if (!string.IsNullOrEmpty(_lastUsername))
                builder.WithCredentials(_lastUsername, _lastPassword ?? "");

            var options = builder.Build();
            await _client.ConnectAsync(options, _cts.Token);

            LogService.Instance.Success("MQTT", "自动重连成功");
        }
        catch (OperationCanceledException)
        {
            // 连接过程中被取消，正常现象，不报 warn
        }
        catch (Exception ex)
        {
            LogService.Instance.Warn("MQTT", $"自动重连失败: {ex.Message}");
        }
    }

    /// <summary>
    /// 停止服务 - 带超时，不卡死 UI
    /// </summary>
    public async Task StopAsync()
    {
        // 1. 立即取消所有正在进行的任务
        _cts?.Cancel();

        // 2. 尝试优雅断开，但设置 2 秒超时 (防止卡死 UI 线程)
        try
        {
            if (_client != null && _client.IsConnected)
            {
                var disconnectTask = _client.DisconnectAsync();
                var timeoutTask = Task.Delay(TimeSpan.FromSeconds(2));
                await Task.WhenAny(disconnectTask, timeoutTask);
            }
        }
        catch (Exception)
        {
            // 忽略断开时的任何错误
        }
        finally
        {
            // 3. 彻底清理
            _client?.Dispose();
            _client = null;
            _isRunning = false;
        }
    }

    public void Dispose()
    {
        _ = StopAsync();
        _cts?.Dispose();
        _client?.Dispose();
    }
}
