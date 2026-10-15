using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;
using UnderSoilAPI.Models;
using UnderSoilAPI.Services;

namespace UnderSoilAPI.Controllers
{
    [Authorize]
    [ApiController]
    [Route("api/v1/[controller]")]
    public class ValveController : ControllerBase
    {
        private readonly UnderSoilContext _db;
        private readonly ValveCommandPublisher _valveCommands;

        public ValveController(UnderSoilContext db, ValveCommandPublisher valveCommands)
        {
            _db = db;
            _valveCommands = valveCommands;
        }

        [HttpGet("history")]
        public async Task<IActionResult> GetHistory()
        {
            var commands = await _db.ValveCommands
                .OrderByDescending(command => command.IssuedAt)
                .ThenByDescending(command => command.Id)
                .Take(50)
                .ToListAsync();

            return Ok(commands);
        }

        [HttpPost("toggle")]
        public async Task<IActionResult> ToggleValve([FromBody] ValveRequest request)
        {

            var userId = User.FindFirstValue(ClaimTypes.NameIdentifier) ?? User.FindFirstValue("sub");
            if (string.IsNullOrWhiteSpace(userId))
            {
                return Unauthorized();
            }

            await _valveCommands.PublishAsync(request.ValveIsOpen);

            var deviceId = await _db.TelemetryReadings
                .OrderByDescending(reading => reading.Timestamp)
                .Select(reading => reading.DeviceId)
                .FirstOrDefaultAsync();

            _db.ValveCommands.Add(new ValveCommand
            {
                UserId = userId,
                DeviceId = deviceId,
                ValveIsOpen = request.ValveIsOpen,
                DurationMinutes = request.DurationMinutes,
                Source = "App",
                IssuedAt = DateTime.UtcNow
            });
            await _db.SaveChangesAsync();

            return Ok();

        }
    }
}
