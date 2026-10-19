using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using System;
using System.Linq;
using System.Threading.Tasks;
using UnderSoilAPI.Models;

namespace UnderSoilAPI.Controllers
{
    [Authorize]
    [ApiController]
    [Route("api/v1/[controller]")]
    public class TelemetryController : ControllerBase
    {
        private readonly UnderSoilContext _db;
        private readonly double _staleAfterSeconds;

        public TelemetryController(UnderSoilContext db, IConfiguration config)
        {
            _db = db;
            _staleAfterSeconds = config.GetValue("Telemetry:StaleAfterSeconds", 15.0);
        }

        // Endpoint: GET http://<IP>:5254/api/v1/telemetry/latest
        [HttpGet("latest")]
        public async Task<IActionResult> GetLatestReading()
        {
            // Get the most recent row based on the Timestamp
            var latestData = await _db.TelemetryReadings
                                      .OrderByDescending(t => t.Timestamp)
                                      .FirstOrDefaultAsync();

            if (latestData == null)
            {
                return NotFound(new { message = "No sensor data found yet." });
            }

            return Ok(WithStatus(latestData));
        }

        // GET /api/telemetry/latest — most recent reading per device
        [HttpGet("latest-per-device")]
        public async Task<IActionResult> GetLatestPerDevice()
        {
            var latest = await _db.TelemetryReadings
                .GroupBy(t => t.DeviceId)
                .Select(g => g.OrderByDescending(t => t.Timestamp).First())
                .ToListAsync();

            return Ok(latest.Select(WithStatus));
        }

        // Timestamp is the server receive time (see the MQTT handler in Program.cs), so its age tells
        // whether the device is still reporting. Computed per request only; nothing is stored.
        private object WithStatus(TelemetryReading reading)
        {
            // SQLite drops DateTimeKind; mark it UTC so it serializes with a "Z"
            var timestamp = DateTime.SpecifyKind(reading.Timestamp, DateTimeKind.Utc);
            var secondsSinceLastReading = Math.Max(0, (DateTime.UtcNow - timestamp).TotalSeconds);

            return new
            {
                reading.Id,
                reading.DeviceId,
                reading.FlowRateLitersPerMin,
                reading.ValveIsOpen,
                reading.TemperatureCelsius,
                reading.HumidityPercentage,
                Timestamp = timestamp,
                IsOnline = secondsSinceLastReading <= _staleAfterSeconds,
                SecondsSinceLastReading = Math.Round(secondsSinceLastReading)
            };
        }

        // GET /api/telemetry?deviceId=ESP-A1 — full history for one device (used by Reports later if needed)
        [HttpGet]
        public async Task<IActionResult> GetTelemetry([FromQuery] string? deviceId)
        {
            var query = _db.TelemetryReadings.AsQueryable();
            if (!string.IsNullOrEmpty(deviceId))
                query = query.Where(t => t.DeviceId == deviceId);

            var readings = await query
                .OrderByDescending(t => t.Timestamp)
                .Take(200)
                .ToListAsync();

            return Ok(readings);
        }
    }
}
