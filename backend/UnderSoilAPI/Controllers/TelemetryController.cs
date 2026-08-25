using Microsoft.AspNetCore.Mvc;
using UnderSoilAPI.Models;
using System;
using System.Threading.Tasks;

namespace UnderSoilAPI.Controllers
{
    [ApiController]
    [Route("api/v1/[controller]")] // This makes the URL look like: http://<your-ip>:5000/api/v1/telemetry
    public class TelemetryController : ControllerBase
    {
        private readonly UnderSoilContext _db;

        public TelemetryController(UnderSoilContext db)
        {
            _db = db;
        }

        // ====================================================================
        // ENDPOINT: The Python Simulator posts data here every 5 seconds
        // URL: POST http://<your-ip>:5000/api/v1/telemetry/ingest
        // ====================================================================
        [HttpPost("ingest")]
        public async Task<IActionResult> IngestTelemetry([FromBody] TelemetryReading reading)
        {
            if (reading == null)
            {
                return BadRequest("I didn't receive any data.");
            }

            // For now, we are just printing it to the black terminal screen to prove it arrived.
            // Now also save to the database.
            Console.WriteLine("=========================================");
            Console.WriteLine($"🔔 NEW DATA RECEIVED FROM: {reading.DeviceId}");
            Console.WriteLine($"💧 Flow Rate: {reading.FlowRateLitersPerMin} L/min");
            Console.WriteLine($"⚙️ Valve Open: {reading.ValveIsOpen}");
            Console.WriteLine($"⚙️ Temperature: {reading.TemperatureCelsius}");
            Console.WriteLine($"⚙️ Humidity: {reading.HumidityPercentage}");
            Console.WriteLine($"⏰ Time: {reading.Timestamp}");
            Console.WriteLine("=========================================");

            await _db.TelemetryReadings.AddAsync(reading);
            await _db.SaveChangesAsync();

            // Tell the Python script that we got the data successfully.
            return Ok(new { status = "Success", message = "Data received loud and clear!" });
        }
    }
}
