using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.IdentityModel.Tokens;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using UnderSoilAPI.Models;

namespace UnderSoilAPI.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class AuthController : ControllerBase
    {
        private const int TokenLifetimeDays = 7;

        private readonly UnderSoilContext _context;
        private readonly IConfiguration _configuration;

        public AuthController(UnderSoilContext context, IConfiguration configuration)
        {
            _context = context;
            _configuration = configuration;
        }

        [HttpPost("register")]
        [AllowAnonymous]
        public IActionResult Register([FromBody] RegisterRequest request)
        {
            // 1. Check if email already exists
            if (_context.Users.Any(u => u.Email == request.Email))
            {
                return BadRequest("Email already in use.");
            }

            // 2. Hash the password using BCrypt
            var passwordHash = BCrypt.Net.BCrypt.HashPassword(request.Password);

            // 3. Create the new user
            var newUser = new User
            {
                Name = request.Name,
                Email = request.Email,
                PasswordHash = passwordHash,
                Role = "Farmer"
            };

            _context.Users.Add(newUser);
            _context.SaveChanges();

            var (token, expiresAt) = GenerateJwtToken(newUser);

            return Ok(new
            {
                Message = "User registered successfully",
                Token = token,
                UserId = newUser.Id,
                Name = newUser.Name,
                ExpiresAt = expiresAt.ToUniversalTime().ToString("O")
            });
        }

        [HttpPost("login")]
        [AllowAnonymous]
        public IActionResult Login([FromBody] LoginRequest request)
        {
            // 1. Find the user by email
            var user = _context.Users.FirstOrDefault(u => u.Email == request.Email);
            if (user == null || !BCrypt.Net.BCrypt.Verify(request.Password, user.PasswordHash))
            {
                return Unauthorized("Invalid email or password.");
            }

            // 2. Generate the JWT String
            var (token, expiresAt) = GenerateJwtToken(user);

            // 3. Return the token to the Android app
            return Ok(new
            {
                Token = token,
                UserId = user.Id,
                Name = user.Name,
                ExpiresAt = expiresAt.ToUniversalTime().ToString("O")
            });
        }

        private (string Token, DateTime ExpiresAt) GenerateJwtToken(User user)
        {
            var jwtKey = _configuration["Jwt:Key"] ?? throw new InvalidOperationException("Jwt:Key is not configured.");
            var securityKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey));
            var credentials = new SigningCredentials(securityKey, SecurityAlgorithms.HmacSha256);

            var claims = new[]
            {
                new Claim(JwtRegisteredClaimNames.Sub, user.Id.ToString()),
                new Claim(JwtRegisteredClaimNames.Email, user.Email),
                new Claim("name", user.Name),
                new Claim("role", user.Role)
            };

            var expiresAt = DateTime.UtcNow.AddMinutes(2);
            expiresAt = DateTimeOffset.FromUnixTimeSeconds(new DateTimeOffset(expiresAt).ToUnixTimeSeconds()).UtcDateTime;

            var token = new JwtSecurityToken(
                issuer: _configuration["Jwt:Issuer"],
                audience: _configuration["Jwt:Audience"],
                claims: claims,
                expires: expiresAt,
                signingCredentials: credentials);

            return (new JwtSecurityTokenHandler().WriteToken(token), expiresAt);
        }
    }

    // These classes match the JSON data sent by your Android app
    public class RegisterRequest
    {
        public string Name { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string Password { get; set; } = string.Empty;
    }

    public class LoginRequest
    {
        public string Email { get; set; } = string.Empty;
        public string Password { get; set; } = string.Empty;
    }
}