using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MQTTnet;
using MQTTnet.Client;
using System;
using System.Text.Json;
using System.Threading.Tasks;
using UnderSoilAPI.Models;

namespace UnderSoilAPI.Controllers
{
    [Authorize]
    [ApiController]
    [Route("api/v1/[controller]")]
    public class ValveController : ControllerBase
    {
        private readonly UnderSoilContext _db;
        private readonly IMqttClient _mqttClient;

        public ValveController(UnderSoilContext db, IMqttClient mqttClient)
        {
            _db = db;
            _mqttClient = mqttClient;
        }


        [HttpPost("toggle")]
        public async Task<IActionResult> ToggleValve([FromBody] ValveRequest request)
        {
            // 1. Format the JSON payload exactly how the ESP32 expects it
            string payload = JsonSerializer.Serialize(new { valveIsOpen = request.ValveIsOpen });

            // 2. Build the MQTT message
            var message = new MqttApplicationMessageBuilder()
                .WithTopic("undersoil/commands")
                .WithPayload(payload)
                .Build();

            // 3. Publish to HiveMQ (which instantly pushes it to the ESP32)
            await _mqttClient.PublishAsync(message);

            return Ok();

        }
    }
}
