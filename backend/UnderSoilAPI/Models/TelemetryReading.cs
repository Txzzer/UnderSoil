using System;
using System.ComponentModel.DataAnnotations;

namespace UnderSoilAPI.Models
{
    public class TelemetryReading
    {
        [Key]
        public int Id { get; set; }

        [Required]
        public string DeviceId { get; set; } = string.Empty;

        // This is the fake water flow data from the Python script
        public double FlowRateLitersPerMin { get; set; }

        // This represents the LED state (is the valve open or closed?)
        public bool ValveIsOpen { get; set; }

        public double TemperatureCelsius { get; set; }

        public double HumidityPercentage { get; set; }


        public DateTime Timestamp { get; set; } = DateTime.UtcNow;
    }
}