namespace UnderSoilAPI.Models
{
    public class GardenEntity
    {
        public string Id { get; set; } = string.Empty;
        public int PlantId { get; set; }
        public string UserId { get; set; } = string.Empty;
        public bool IsSynced { get; set; } = true;
    }
}
