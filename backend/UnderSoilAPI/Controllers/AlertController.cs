using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using UnderSoilAPI.Models;

namespace UnderSoilAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize(Roles = "Admin")]
    public class AlertsController : ControllerBase
    {
        private readonly UnderSoilContext _context;

        public AlertsController(UnderSoilContext context)
        {
            _context = context;
        }

        // GET /api/alerts
        [HttpGet]
        public async Task<IActionResult> GetAlerts()
        {
            var alerts = await _context.IotAlerts
                .OrderByDescending(a => a.DateTime)
                .ToListAsync();
            return Ok(alerts);
        }
    }
}