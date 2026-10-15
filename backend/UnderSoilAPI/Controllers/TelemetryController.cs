using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
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

        public TelemetryController(UnderSoilContext db)
        {
            _db = db;
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

            return Ok(latestData);
        }

        // GET /api/telemetry/latest — most recent reading per device
        [HttpGet("latest-per-device")]
        public async Task<IActionResult> GetLatestPerDevice()
        {
            var latest = await _db.TelemetryReadings
                .GroupBy(t => t.DeviceId)
                .Select(g => g.OrderByDescending(t => t.Timestamp).First())
                .ToListAsync();

            return Ok(latest);
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
