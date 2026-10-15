using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using UnderSoilAPI.Models;

namespace UnderSoilAPI.Controllers
{
    [Authorize]
    [ApiController]
    [Route("api/[controller]")]
    public class TreatmentsController : ControllerBase
    {
        private readonly UnderSoilContext _context;

        public TreatmentsController(UnderSoilContext context)
        {
            _context = context;
        }

        [HttpGet]
        public async Task<IActionResult> GetTreatments([FromQuery] string? label)
        {
            var query = _context.DiseaseTreatments.AsQueryable();
            if (!string.IsNullOrWhiteSpace(label))
            {
                query = query.Where(treatment => treatment.Label == label);
            }

            var treatments = await query.ToListAsync();
            return Ok(treatments);
        }
    }
}
