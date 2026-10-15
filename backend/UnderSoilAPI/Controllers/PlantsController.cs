using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using UnderSoilAPI.Models;

namespace UnderSoilAPI.Controllers
{
    [Authorize]
    [ApiController]
    [Route("api/[controller]")]
    public class PlantsController : ControllerBase
    {
        private readonly UnderSoilContext _context;

        public PlantsController(UnderSoilContext context)
        {
            _context = context;
        }

        [HttpGet]
        public async Task<IActionResult> GetPlants()
        {
            var plants = await _context.Plants.ToListAsync();
            return Ok(plants);
        }
    }
}
