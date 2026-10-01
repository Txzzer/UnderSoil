using System.Net;
using System.Text;
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using MQTTnet;
using MQTTnet.Client;
using MQTTnet.Client.Options;
using UnderSoilAPI.Models;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddControllers();

// Register EF Core DbContext (SQL Server)
builder.Services.AddDbContext<UnderSoilContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("DefaultConnection")));

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Create MQTT client and options to connect to HiveMQ (or any broker)
var factory = new MqttFactory();
var mqttClient = factory.CreateMqttClient();

builder.Services.AddSingleton<IMqttClient>(mqttClient);

var app = builder.Build();

// Ensure database exists
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<UnderSoilContext>();
    db.Database.EnsureCreated();
}

// Read MQTT client settings from configuration (appsettings.json / environment)
var mqttSection = builder.Configuration.GetSection("Mqtt");
var broker = mqttSection.GetValue<string>("Broker") ?? "broker.hivemq.com";
var port = mqttSection.GetValue<int?>("Port") ?? 1883;
var useTls = mqttSection.GetValue<bool?>("UseTls") ?? false;
var mqttUser = mqttSection.GetValue<string>("Username");
var mqttPass = mqttSection.GetValue<string>("Password");
var topic = mqttSection.GetValue<string>("Topic") ?? "undersoil/telemetry";
var clientId = mqttSection.GetValue<string>("ClientId") ?? $"UnderSoilClient-{Dns.GetHostName()}";



var optionsBuilder = new MqttClientOptionsBuilder()
    .WithClientId(clientId)
    .WithTcpServer(broker, port);

if (!string.IsNullOrEmpty(mqttUser))
    optionsBuilder = optionsBuilder.WithCredentials(mqttUser, mqttPass ?? string.Empty);
if (useTls)
    optionsBuilder = optionsBuilder.WithTls();

var mqttOptions = optionsBuilder.Build();

// Handlers
mqttClient.UseConnectedHandler(async e =>
{
    Console.WriteLine($"MQTT -> Connected to broker {broker}:{port} as {clientId}.");
    // Subscribe to topic(s) from HiveMQ clients
    await mqttClient.SubscribeAsync(new MqttTopicFilterBuilder()
        .WithTopic(topic)
        .WithAtLeastOnceQoS()
        .Build());
    Console.WriteLine($"MQTT -> Subscribed to '{topic}'.");
});

mqttClient.UseDisconnectedHandler(async e =>
{
    Console.WriteLine($"MQTT -> Disconnected (Reason: {e.Reason}). Reconnecting in 5s...");
    await Task.Delay(TimeSpan.FromSeconds(5));
    try
    {
        await mqttClient.ConnectAsync(mqttOptions);
    }
    catch (Exception ex)
    {
        Console.WriteLine($"MQTT -> Reconnect failed: {ex.Message}");
    }
});

mqttClient.UseApplicationMessageReceivedHandler(async e =>
{
    try
    {
        var payload = e.ApplicationMessage?.Payload == null ? null : Encoding.UTF8.GetString(e.ApplicationMessage.Payload);
        Console.WriteLine($"MQTT -> Message from '{e.ClientId}' topic '{e.ApplicationMessage?.Topic}': {payload}");
        if (string.IsNullOrWhiteSpace(payload)) return;

        var reading = JsonSerializer.Deserialize<TelemetryReading>(payload, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
        if (reading == null) { Console.WriteLine("MQTT -> Failed to deserialize payload."); return; }
        if (reading.Timestamp == default) reading.Timestamp = DateTime.UtcNow;

        using var scope = app.Services.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<UnderSoilContext>();
        await db.TelemetryReadings.AddAsync(reading);
        await db.SaveChangesAsync();

        Console.WriteLine($"MQTT -> Saved telemetry from {reading.DeviceId} at {reading.Timestamp}.");
    }
    catch (Exception ex)
    {
        Console.WriteLine($"MQTT -> Error handling message: {ex}");
    }
});

// Connect the client to HiveMQ (or configured broker)
try
{
    Console.WriteLine($"MQTT -> Connecting to broker {broker}:{port} ...");
    await mqttClient.ConnectAsync(mqttOptions);
}
catch (Exception ex)
{
    Console.WriteLine($"MQTT -> Initial connect failed: {ex.Message}");
}

// Graceful shutdown: disconnect MQTT client
app.Lifetime.ApplicationStopping.Register(() =>
{
    mqttClient.DisconnectAsync().GetAwaiter().GetResult();
});

Console.WriteLine($"Broker settings: {broker}:{port}, topic='{topic}', clientId='{clientId}'");

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();

app.UseAuthorization();

app.MapControllers();

await app.RunAsync();
    
