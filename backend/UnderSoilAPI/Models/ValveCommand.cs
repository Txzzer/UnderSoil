using System.ComponentModel.DataAnnotations;

namespace UnderSoilAPI.Models
{
    public class ValveCommand
    {
        [Key]
        public int Id { get; set; }

        public string UserId { get; set; } = string.Empty;
        public string? DeviceId { get; set; }
        public bool ValveIsOpen { get; set; }
        public int? DurationMinutes { get; set; }
        public string Source { get; set; } = "App";
        public DateTime IssuedAt { get; set; }
    }
}
