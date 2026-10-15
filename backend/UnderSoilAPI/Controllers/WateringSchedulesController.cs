using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using UnderSoilAPI.Models;

namespace UnderSoilAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize(Roles = "Admin")]
    public class WateringSchedulesController : ControllerBase
    {
        private readonly UnderSoilContext _context;

        public WateringSchedulesController(UnderSoilContext context)
        {
            _context = context;
        }

        // GET /api/wateringschedules
        [HttpGet]
        public async Task<IActionResult> GetWateringSchedules()
        {
            var schedules = await _context.WateringSchedules
                .OrderByDescending(w => w.StartTime)
                .ToListAsync();

            return Ok(schedules);
        }
    }
}