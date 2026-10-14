using System.Text.Json;
using MQTTnet;
using MQTTnet.Client;

namespace UnderSoilAPI.Services
{
    /// <summary>
    /// Publishes valve commands to the ESP32 over MQTT. Shared by ValveController (manual toggles)
    /// and WateringSchedulerService (scheduled watering).
    /// </summary>
    public class ValveCommandPublisher
    {
        public const string CommandTopic = "undersoil/commands";

        private readonly IMqttClient _mqttClient;

        public ValveCommandPublisher(IMqttClient mqttClient)
        {
            _mqttClient = mqttClient;
        }

        /// <param name="durationMinutes">
        /// Optional run time, sent as "duration" for firmware that auto-closes. The current firmware ignores it,
        /// so callers that open the valve on a timer must still send the close command themselves.
        /// </param>
        public async Task PublishAsync(bool valveIsOpen, int? durationMinutes = null, CancellationToken cancellationToken = default)
        {
            // Format the JSON payload exactly how the ESP32 expects it
            string payload = durationMinutes.HasValue
                ? JsonSerializer.Serialize(new { valveIsOpen, duration = durationMinutes.Value })
                : JsonSerializer.Serialize(new { valveIsOpen });

            var message = new MqttApplicationMessageBuilder()
                .WithTopic(CommandTopic)
                .WithPayload(payload)
                .Build();

            // Publish to HiveMQ (which instantly pushes it to the ESP32)
            await _mqttClient.PublishAsync(message, cancellationToken);
        }
    }
}
