namespace UnderSoilAPI.Models
{
    public class Sector
    {
        public string Id { get; set; } = string.Empty;
        public string UserId { get; set; } = string.Empty;
        public string Name { get; set; } = string.Empty;
        public string CropNames { get; set; } = string.Empty;
        public bool IsSynced { get; set; } = true;
    }
}
