using System;

namespace UnderSoilAPI.Dtos
{
    // API response only: a TelemetryReading plus whether its device is still reporting.
    // Not an EF entity and not stored in the database.
    public class TelemetryStatusDto
    {
        public int Id { get; set; }

        public string DeviceId { get; set; } = string.Empty;

        public double FlowRateLitersPerMin { get; set; }

        public bool ValveIsOpen { get; set; }

        public double TemperatureCelsius { get; set; }

        public double HumidityPercentage { get; set; }

        // UTC time the reading was stored
        public DateTime Timestamp { get; set; }

        // True if the newest reading is no older than Telemetry:StaleAfterSeconds
        public bool IsOnline { get; set; }

        public double SecondsSinceLastReading { get; set; }
    }
}
