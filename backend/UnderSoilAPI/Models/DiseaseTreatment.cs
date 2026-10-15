using System.ComponentModel.DataAnnotations;

namespace UnderSoilAPI.Models
{
    public class DiseaseTreatment
    {
        [Key]
        public int Id { get; set; }

        public string Label { get; set; } = string.Empty;
        public string CropName { get; set; } = string.Empty;
        public string DiseaseName { get; set; } = string.Empty;
        public string Cause { get; set; } = string.Empty;
        public string Recommendations { get; set; } = string.Empty;
    }
}
