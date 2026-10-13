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
    }
}
