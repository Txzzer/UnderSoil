using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

using UnderSoilAPI.Models;

namespace UnderSoilAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize(Roles = "Admin")]
    public class GardenController : ControllerBase
    {
        private readonly UnderSoilContext _context;

        public GardenController(UnderSoilContext context)
        {
            _context = context;
        }

        // GET /api/garden
        [HttpGet]
        public async Task<IActionResult> GetGarden()
        {
            var garden = await _context.GardenCrops.ToListAsync();
            return Ok(garden);
        }
    }
}