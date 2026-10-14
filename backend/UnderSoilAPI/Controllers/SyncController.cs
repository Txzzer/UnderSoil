using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
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

        // POST: api/sync/sectors
        [HttpPost("sectors")]
        public async Task<IActionResult> SyncSectors([FromBody] List<Sector> incomingSectors)
        {
            if (incomingSectors == null || incomingSectors.Count == 0)
            {
                return Ok();
            }

            foreach (var incoming in incomingSectors)
            {
                var existing = await _context.Sectors.FirstOrDefaultAsync(s => s.Id == incoming.Id);
                if (existing != null)
                {
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

            foreach (var incoming in incomingGardenCrops)
            {
                var existing = await _context.GardenCrops.FirstOrDefaultAsync(g => g.Id == incoming.Id);
                if (existing != null)
                {
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

        // POST: api/sync/logs
        [HttpPost("logs")]
        public async Task<IActionResult> SyncScanLogs([FromBody] List<ScanLog> incomingLogs)
        {
            if (incomingLogs == null || incomingLogs.Count == 0)
            {
                return Ok();
            }

            foreach (var incoming in incomingLogs)
            {
                var existing = await _context.ScanLogs.FirstOrDefaultAsync(l => l.Id == incoming.Id);
                if (existing != null)
                {
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

            foreach (var incoming in incomingAlerts)
            {
                var existing = await _context.IotAlerts.FirstOrDefaultAsync(a => a.Id == incoming.Id);
                if (existing != null)
                {
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

            foreach (var incoming in incomingSchedules)
            {
                var existing = await _context.WateringSchedules.FirstOrDefaultAsync(s => s.Id == incoming.Id);
                if (existing != null)
                {
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
