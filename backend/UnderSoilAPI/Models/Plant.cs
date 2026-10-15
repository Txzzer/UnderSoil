using System.ComponentModel.DataAnnotations;

namespace UnderSoilAPI.Models
{
    public class Plant
    {
        [Key]
        public int Id { get; set; }

        public string Name { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public string WaterNeeds { get; set; } = string.Empty;
        public string SunlightNeeds { get; set; } = string.Empty;
        public string FertilizerNeeds { get; set; } = string.Empty;
        public string HumidityNeeds { get; set; } = string.Empty;
    }
}
