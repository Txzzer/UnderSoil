using Microsoft.EntityFrameworkCore;

namespace UnderSoilAPI.Models
{
    public class UnderSoilContext : DbContext
    {
        public UnderSoilContext(DbContextOptions<UnderSoilContext> options)
            : base(options)
        {
        }
        public DbSet<User> Users { get; set; }

        public DbSet<TelemetryReading> TelemetryReadings { get; set; } = null!;

    }
}