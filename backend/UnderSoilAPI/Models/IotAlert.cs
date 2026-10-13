namespace UnderSoilAPI.Models
{
    public class IotAlert
    {
        public string Id { get; set; } = string.Empty;
        public string DeviceId { get; set; } = string.Empty;
        public string UserId { get; set; } = string.Empty;
        public string AlertDesc { get; set; } = string.Empty;
        public string DateStr { get; set; } = string.Empty;
        public string DateTime { get; set; } = string.Empty;
        public bool IsSynced { get; set; } = true;


    }
}
