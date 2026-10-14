using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using UnderSoilAPI.Models;

namespace UnderSoilAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize(Roles = "Admin")]
    public class SectorsController : ControllerBase
    {
        private readonly UnderSoilContext _context;

        public SectorsController(UnderSoilContext context)
        {
            _context = context;
        }

        // GET /api/sectors
        [HttpGet]
        public async Task<IActionResult> GetSectors()
        {
            var sectors = await _context.Sectors.ToListAsync();
            return Ok(sectors);
        }
    }
}