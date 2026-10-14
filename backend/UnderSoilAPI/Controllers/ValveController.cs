using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using MQTTnet;
using MQTTnet.Client;
using System;
using System.Text.Json;
using System.Threading.Tasks;
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


        [HttpPost("toggle")]
        public async Task<IActionResult> ToggleValve([FromBody] ValveRequest request)
        {
            await _valveCommands.PublishAsync(request.ValveIsOpen);

            return Ok();

        }
    }
}
