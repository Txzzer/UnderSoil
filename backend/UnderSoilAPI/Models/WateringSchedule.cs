namespace UnderSoilAPI.Models
{
    public class WateringSchedule
    {
        public const string StatusPending = "Pending";
        public const string StatusRunning = "Running";
        public const string StatusCompleted = "Completed";
        public const string StatusMissed = "Missed";

        public string Id { get; set; } = string.Empty;
        public string UserId { get; set; } = string.Empty;
        public string SectorId { get; set; } = string.Empty;

        // UTC epoch milliseconds (matches Android's System.currentTimeMillis())
        public long StartTime { get; set; }
        public int DurationMinutes { get; set; }
        public double ExpectedVolume { get; set; }
        public double ActualVolume { get; set; }
        public string Status { get; set; } = StatusPending;
        public bool IsSynced { get; set; } = true;

        public long EndTime => StartTime + DurationMinutes * 60_000L;
    }
}
