using System.ComponentModel.DataAnnotations;

namespace UnderSoilAPI.Models
{
    public class ScanLog
    {
        public string Id { get; set; } = string.Empty;
        public string UserId { get; set; } = string.Empty;
        public string CropName { get; set; } = string.Empty;
        public string Diagnosis { get; set; } = string.Empty;
        public float Confidence { get; set; }
        public string DateStr { get; set; } = string.Empty;
        public bool IsSynced { get; set; } = true;

        [MaxLength(50)]
        public string? Notes { get; set; }
    }
}
