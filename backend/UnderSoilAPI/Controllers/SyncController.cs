using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;
using UnderSoilAPI.Models;

namespace UnderSoilAPI.Controllers
{
    [Authorize]
    [ApiController]
    [Route("api/sync")]
    public class SyncController : ControllerBase
    {
        private readonly UnderSoilContext _context;

        public SyncController(UnderSoilContext context)
        {
            _context = context;
        }

        // The caller's user id comes from their JWT, never from the request body
        private string? CurrentUserId() =>
            User.FindFirstValue(ClaimTypes.NameIdentifier) ?? User.FindFirstValue("sub");

        // GET: api/sync/all
        // Everything that belongs to the logged-in farmer, so a new device or reinstall can restore it.
        [HttpGet("all")]
        public async Task<IActionResult> GetAllForUser()
        {
            var userId = CurrentUserId();
            if (string.IsNullOrEmpty(userId)) return Unauthorized();

            return Ok(new
            {
                sectors = await _context.Sectors.AsNoTracking().Where(s => s.UserId == userId).ToListAsync(),
                garden = await _context.GardenCrops.AsNoTracking().Where(g => g.UserId == userId).ToListAsync(),
                scanLogs = await _context.ScanLogs.AsNoTracking().Where(l => l.UserId == userId).ToListAsync(),
                alerts = await _context.IotAlerts.AsNoTracking().Where(a => a.UserId == userId).ToListAsync(),
                schedules = await _context.WateringSchedules.AsNoTracking().Where(s => s.UserId == userId).ToListAsync()
            });
        }

        // POST: api/sync/sectors
        [HttpPost("sectors")]
        public async Task<IActionResult> SyncSectors([FromBody] List<Sector> incomingSectors)
        {
            if (incomingSectors == null || incomingSectors.Count == 0)
            {
                return Ok();
            }

            var userId = CurrentUserId();
            if (string.IsNullOrEmpty(userId)) return Unauthorized();

            foreach (var incoming in incomingSectors)
            {
                incoming.UserId = userId;
                var existing = await _context.Sectors.FirstOrDefaultAsync(s => s.Id == incoming.Id);
                if (existing != null)
                {
                    // Ids are global: never let one user overwrite another user's row
                    if (existing.UserId != userId) continue;

                    existing.UserId = incoming.UserId;
                    existing.Name = incoming.Name;
                    existing.CropNames = incoming.CropNames;
                    existing.IsSynced = true;
                    _context.Sectors.Update(existing);
                }
                else
                {
                    incoming.IsSynced = true;
                    await _context.Sectors.AddAsync(incoming);
                }
            }

            await _context.SaveChangesAsync();
            return Ok();
        }

        // POST: api/sync/garden
        [HttpPost("garden")]
        public async Task<IActionResult> SyncGarden([FromBody] List<GardenEntity> incomingGardenCrops)
        {
            if (incomingGardenCrops == null || incomingGardenCrops.Count == 0)
            {
                return Ok();
            }

            var userId = CurrentUserId();
            if (string.IsNullOrEmpty(userId)) return Unauthorized();

            foreach (var incoming in incomingGardenCrops)
            {
                incoming.UserId = userId;
                var existing = await _context.GardenCrops.FirstOrDefaultAsync(g => g.Id == incoming.Id);
                if (existing != null)
                {
                    if (existing.UserId != userId) continue;

                    existing.PlantId = incoming.PlantId;
                    existing.UserId = incoming.UserId;
                    existing.IsSynced = true;
                    _context.GardenCrops.Update(existing);
                }
                else
                {
                    incoming.IsSynced = true;
                    await _context.GardenCrops.AddAsync(incoming);
                }
            }

            await _context.SaveChangesAsync();
            return Ok();
        }

        // POST: api/sync/garden/delete
        // Plant ids the farmer removed from My Garden on the phone (possibly while offline).
        [HttpPost("garden/delete")]
        public async Task<IActionResult> DeleteGardenPlants([FromBody] List<int> plantIds)
        {
            if (plantIds == null || plantIds.Count == 0) return Ok();

            var userId = CurrentUserId();
            if (string.IsNullOrEmpty(userId)) return Unauthorized();

            var rows = await _context.GardenCrops
                .Where(g => g.UserId == userId && plantIds.Contains(g.PlantId))
                .ToListAsync();
            _context.GardenCrops.RemoveRange(rows);

            await _context.SaveChangesAsync();
            return Ok();
        }

        // POST: api/sync/logs
        [HttpPost("logs")]
        public async Task<IActionResult> SyncScanLogs([FromBody] List<ScanLog> incomingLogs)
        {
            if (incomingLogs == null || incomingLogs.Count == 0)
            {
                return Ok();
            }

            var userId = CurrentUserId();
            if (string.IsNullOrEmpty(userId)) return Unauthorized();

            foreach (var incoming in incomingLogs)
            {
                incoming.UserId = userId;
                var existing = await _context.ScanLogs.FirstOrDefaultAsync(l => l.Id == incoming.Id);
                if (existing != null)
                {
                    if (existing.UserId != userId) continue;

                    existing.UserId = incoming.UserId;
                    existing.CropName = incoming.CropName;
                    existing.Diagnosis = incoming.Diagnosis;
                    existing.Confidence = incoming.Confidence;
                    existing.DateStr = incoming.DateStr;
                    existing.Notes = incoming.Notes;
                    existing.IsSynced = true;
                    _context.ScanLogs.Update(existing);
                }
                else
                {
                    incoming.IsSynced = true;
                    await _context.ScanLogs.AddAsync(incoming);
                }
            }

            await _context.SaveChangesAsync();
            return Ok();
        }
        [HttpPost("alerts")]
        public async Task<IActionResult> SyncIotAlerts([FromBody] List<IotAlert> incomingAlerts)
        {
            if (incomingAlerts == null || incomingAlerts.Count == 0) return Ok();

            var userId = CurrentUserId();
            if (string.IsNullOrEmpty(userId)) return Unauthorized();

            foreach (var incoming in incomingAlerts)
            {
                incoming.UserId = userId;
                var existing = await _context.IotAlerts.FirstOrDefaultAsync(a => a.Id == incoming.Id);
                if (existing != null)
                {
                    if (existing.UserId != userId) continue;

                    existing.DeviceId = incoming.DeviceId;
                    existing.UserId = incoming.UserId;
                    existing.AlertDesc = incoming.AlertDesc;
                    existing.DateTime = incoming.DateTime;
                    existing.IsSynced = true;
                    _context.IotAlerts.Update(existing);
                }
                else
                {
                    incoming.IsSynced = true;
                    await _context.IotAlerts.AddAsync(incoming);
                }
            }

            await _context.SaveChangesAsync();
            return Ok();
        }

        // POST: api/sync/schedules
        [HttpPost("schedules")]
        public async Task<IActionResult> SyncWateringSchedules([FromBody] List<WateringSchedule> incomingSchedules)
        {
            if (incomingSchedules == null || incomingSchedules.Count == 0) return Ok();

            var userId = CurrentUserId();
            if (string.IsNullOrEmpty(userId)) return Unauthorized();

            foreach (var incoming in incomingSchedules)
            {
                incoming.UserId = userId;
                var existing = await _context.WateringSchedules.FirstOrDefaultAsync(s => s.Id == incoming.Id);
                if (existing != null)
                {
                    if (existing.UserId != userId) continue;

                    // Status and ActualVolume are owned by the server; once a schedule has started,
                    // a re-upload from the phone must not reset it back to Pending.
                    if (existing.Status == WateringSchedule.StatusPending)
                    {
                        existing.UserId = incoming.UserId;
                        existing.SectorId = incoming.SectorId;
                        existing.StartTime = incoming.StartTime;
                        existing.DurationMinutes = incoming.DurationMinutes;
                        existing.ExpectedVolume = incoming.ExpectedVolume;
                    }
                    existing.IsSynced = true;
                    _context.WateringSchedules.Update(existing);
                }
                else
                {
                    incoming.Status = WateringSchedule.StatusPending;
                    incoming.ActualVolume = 0;
                    incoming.IsSynced = true;
                    await _context.WateringSchedules.AddAsync(incoming);
                }
            }

            await _context.SaveChangesAsync();
            return Ok();
        }
    }
}
