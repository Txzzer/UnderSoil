
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using UnderSoilAPI.Models;

namespace UnderSoilAPI.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    [Authorize(Roles = "Admin")]
    public class UsersController : ControllerBase
    {
        private readonly UnderSoilContext _context;

        public UsersController(UnderSoilContext context)
        {
            _context = context;
        }

        // GET: api/Users
        [HttpGet]
        public async Task<IActionResult> GetUsers()
        {
            var users = await _context.Users
                .AsNoTracking()
                .Select(u => new
                {
                    u.Id,
                    u.Name,
                    u.Email,
                    u.Role
                })
                .ToListAsync();

            return Ok(users);
        }

        // POST: api/Users
        [HttpPost]
        public async Task<IActionResult> CreateUser(
            [FromBody] CreateUserRequest request)
        {
            if (string.IsNullOrWhiteSpace(request.Name) ||
                string.IsNullOrWhiteSpace(request.Email) ||
                string.IsNullOrWhiteSpace(request.Password))
            {
                return BadRequest(
                    "Name, email, and password are required.");
            }

            var name = request.Name.Trim();
            var email = request.Email.Trim();

            if (request.Password.Length < 8)
            {
                return BadRequest(
                    "Password must be at least 8 characters.");
            }

            if (request.Role != "Farmer" &&
                request.Role != "Admin")
            {
                return BadRequest(
                    "Role must be Farmer or Admin.");
            }

            var emailExists = await _context.Users
                .AnyAsync(u => u.Email == email);

            if (emailExists)
            {
                return Conflict(
                    "That email address is already registered.");
            }

            var user = new User
            {
                Name = name,
                Email = email,
                Role = request.Role,
                PasswordHash = BCrypt.Net.BCrypt.HashPassword(
                    request.Password)
            };

            _context.Users.Add(user);
            await _context.SaveChangesAsync();

            return StatusCode(201, new
            {
                user.Id,
                user.Name,
                user.Email,
                user.Role
            });
        }

        // PUT: api/Users/5
        [HttpPut("{id:guid}")]
        public async Task<IActionResult> UpdateUser(
            Guid id,
            [FromBody] UpdateUserRequest request)
        {
            var user = await _context.Users.FindAsync(id);

            if (user == null)
            {
                return NotFound("User not found.");
            }

            if (string.IsNullOrWhiteSpace(request.Name) ||
                string.IsNullOrWhiteSpace(request.Email))
            {
                return BadRequest(
                    "Name and email are required.");
            }

            var name = request.Name.Trim();
            var email = request.Email.Trim();

            if (request.Role != "Farmer" &&
                request.Role != "Admin")
            {
                return BadRequest(
                    "Role must be Farmer or Admin.");
            }

            var emailExists = await _context.Users
                .AnyAsync(u => u.Email == email && u.Id != id);

            if (emailExists)
            {
                return Conflict(
                    "That email address is already registered.");
            }
            // Don't let the last admin get demoted — would lock everyone out of the dashboard.
            if (user.Role == "Admin" && request.Role != "Admin")
            {
                var adminCount = await _context.Users.CountAsync(u => u.Role == "Admin");
                if (adminCount <= 1)
                    return BadRequest("Can't change the role of the last admin account.");
            }

            user.Name = name;
            user.Email = email;
            user.Role = request.Role;

            // Only update the password if a new one was provided.
            if (!string.IsNullOrWhiteSpace(request.Password))
            {
                if (request.Password.Length < 8)
                {
                    return BadRequest(
                        "Password must be at least 8 characters.");
                }

                user.PasswordHash =
                    BCrypt.Net.BCrypt.HashPassword(
                        request.Password);
            }

            await _context.SaveChangesAsync();

            return Ok(new
            {
                user.Id,
                user.Name,
                user.Email,
                user.Role
            });
        }

        // DELETE: api/Users/5
        [HttpDelete("{id:guid}")]
        public async Task<IActionResult> DeleteUser(Guid id)
        {
            var user = await _context.Users.FindAsync(id);

            if (user == null)
            {
                return NotFound("User not found.");
            }

            // Don't let the last admin get deleted — would lock everyone out of the dashboard.
            if (user.Role == "Admin")
            {
                var adminCount = await _context.Users.CountAsync(u => u.Role == "Admin");
                if (adminCount <= 1)
                    return BadRequest("Can't remove the last admin account.");
            }

            _context.Users.Remove(user);
            await _context.SaveChangesAsync();

            return NoContent();
        }
    }

    public class CreateUserRequest
    {
        public string Name { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string Password { get; set; } = string.Empty;
        public string Role { get; set; } = "Farmer";
    }

    public class UpdateUserRequest
    {
        public string Name { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string? Password { get; set; }
        public string Role { get; set; } = "Farmer";
    }
}
