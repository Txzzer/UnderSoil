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
        public DbSet<ScanLog> ScanLogs { get; set; }
        public DbSet<Sector> Sectors { get; set; }
        public DbSet<GardenEntity> GardenCrops { get; set; }
        public DbSet<IotAlert> IotAlerts { get; set; }
        public DbSet<WateringSchedule> WateringSchedules { get; set; }
        public DbSet<Plant> Plants { get; set; }
        public DbSet<DiseaseTreatment> DiseaseTreatments { get; set; }
        public DbSet<ValveCommand> ValveCommands { get; set; }
    }
}