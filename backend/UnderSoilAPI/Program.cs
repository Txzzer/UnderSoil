using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using MQTTnet;
using MQTTnet.Client;
using MQTTnet.Client.Options;
using System.Net;
using System.Text;
using System.Text.Json;
using UnderSoilAPI.Models;
using UnderSoilAPI.Services;

var builder = WebApplication.CreateBuilder(args);
var jwtKey = builder.Configuration["Jwt:Key"];

if (string.IsNullOrWhiteSpace(jwtKey) || jwtKey.Length < 32 || Encoding.UTF8.GetByteCount(jwtKey) < 32)
{
    throw new InvalidOperationException("Jwt:Key must be configured with at least 32 characters.");
}

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey)),
            ValidateIssuer = true,
            ValidIssuer = builder.Configuration["Jwt:Issuer"],
            ValidateAudience = true,
            ValidAudience = builder.Configuration["Jwt:Audience"],
            ValidateLifetime = true,
            ClockSkew = TimeSpan.Zero
        };
    });

// Add services to the container.
builder.Services.AddControllers();

// Register EF Core DbContext (SQL Server)
builder.Services.AddDbContext<UnderSoilContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("DefaultConnection")));

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(options =>
{
    options.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Description = "Enter your JWT access token."
    });

    options.AddSecurityRequirement(new OpenApiSecurityRequirement
       {
           {
               new OpenApiSecurityScheme
               {
                   Reference = new OpenApiReference
                   {
                       Type = ReferenceType.SecurityScheme,
                       Id = "Bearer"
                   }
               },
               Array.Empty<string>()
           }
       });
});

// Create MQTT client and options to connect to HiveMQ (or any broker)
var factory = new MqttFactory();
var mqttClient = factory.CreateMqttClient();
builder.Services.AddSingleton<IMqttClient>(mqttClient);
builder.Services.AddSingleton<ValveCommandPublisher>();

// Opens/closes the valve for synced watering schedules when their time arrives
builder.Services.AddHostedService<WateringSchedulerService>();


builder.Services.AddCors(options =>
{
    options.AddPolicy("UnderSoilDashboard", policy =>
    {
        policy
            .WithOrigins("http://127.0.0.1:3000")
            .AllowAnyHeader()
            .AllowAnyMethod();
    });
});


var app = builder.Build();


app.UseCors("UnderSoilDashboard");


using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<UnderSoilContext>();

    if (app.Environment.IsDevelopment())
    {
        db.Database.Migrate();
    }

    if (!db.Plants.Any())
    {
        db.Plants.AddRange(
            new Plant { Name = "Tomato", Description = "Tomatoes add elegance to indoor spaces and carry a positive symbolic message.", WaterNeeds = "250 ml", SunlightNeeds = "Normal", FertilizerNeeds = "70 ml", HumidityNeeds = "54%" },
            new Plant { Name = "Potato", Description = "Potatoes are versatile root vegetables that thrive in loose, well-drained soil.", WaterNeeds = "200 ml", SunlightNeeds = "High", FertilizerNeeds = "50 ml", HumidityNeeds = "40%" },
            new Plant { Name = "Apple", Description = "Apple trees require careful pruning and consistent watering to produce high yields.", WaterNeeds = "300 ml", SunlightNeeds = "High", FertilizerNeeds = "100 ml", HumidityNeeds = "50%" },
            new Plant { Name = "Corn", Description = "Corn is a staple cereal crop that requires significant space and full sun.", WaterNeeds = "400 ml", SunlightNeeds = "High", FertilizerNeeds = "80 ml", HumidityNeeds = "45%" },
            new Plant { Name = "Grape", Description = "Grapes grow on vigorous vines and need strong trellising for support.", WaterNeeds = "150 ml", SunlightNeeds = "High", FertilizerNeeds = "40 ml", HumidityNeeds = "60%" },
            new Plant { Name = "Cherry", Description = "Cherry trees produce sweet stone fruits and prefer well-drained soil.", WaterNeeds = "250 ml", SunlightNeeds = "High", FertilizerNeeds = "60 ml", HumidityNeeds = "50%" },
            new Plant { Name = "Peach", Description = "Peaches prefer temperate climates and require winter chilling hours.", WaterNeeds = "250 ml", SunlightNeeds = "High", FertilizerNeeds = "70 ml", HumidityNeeds = "50%" },
            new Plant { Name = "Pepper", Description = "Peppers thrive in warm weather and produce better yields with consistent moisture.", WaterNeeds = "200 ml", SunlightNeeds = "High", FertilizerNeeds = "60 ml", HumidityNeeds = "45%" },
            new Plant { Name = "Strawberry", Description = "Strawberries are low-growing perennials that multiply via runners.", WaterNeeds = "150 ml", SunlightNeeds = "Normal", FertilizerNeeds = "30 ml", HumidityNeeds = "60%" });
        db.SaveChanges();
    }

    if (!db.DiseaseTreatments.Any())
    {
        var recommendationsPath = Path.Combine(AppContext.BaseDirectory, "Data", "recommendations.json");
        using var recommendationsDocument = JsonDocument.Parse(File.ReadAllText(recommendationsPath));
        var treatments = recommendationsDocument.RootElement.EnumerateObject().Select(entry =>
        {
            var value = entry.Value;
            var recommendations = value.GetProperty("recommendations").EnumerateArray()
                .Select(recommendation => recommendation.GetString() ?? string.Empty);

            return new DiseaseTreatment
            {
                Label = entry.Name,
                CropName = value.GetProperty("crop").GetString() ?? string.Empty,
                DiseaseName = value.GetProperty("disease").GetString() ?? string.Empty,
                Cause = value.GetProperty("cause").GetString() ?? string.Empty,
                Recommendations = string.Join("\n", recommendations)
            };
        }).ToList();

        db.DiseaseTreatments.AddRange(treatments);
        db.SaveChanges();
    }
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
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();

await app.RunAsync();