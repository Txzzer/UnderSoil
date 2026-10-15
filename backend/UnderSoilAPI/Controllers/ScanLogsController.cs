using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using UnderSoilAPI.Models;

namespace UnderSoilAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize(Roles = "Admin")]
    public class ScanLogsController : ControllerBase
    {
        private readonly UnderSoilContext _context;

        public ScanLogsController(UnderSoilContext context)
        {
            _context = context;
        }

        // GET /api/scanlogs
        [HttpGet]
        public async Task<IActionResult> GetScanLogs()
        {
            var logs = await _context.ScanLogs
                .OrderByDescending(s => s.DateStr)
                .ToListAsync();

            return Ok(logs);
        }
    }
}