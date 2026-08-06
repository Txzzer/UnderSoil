package network;

public class TelemetryReading{

    private String deviceId;
    private double flowRateLitersPerMin;
    private boolean valveIsOpen;
    private String timestamp;

    public TelemetryReading(
            String deviceId,
            double flowRateLitersPerMin,
            boolean valveIsOpen,
            String timestamp
    ) {
        this.deviceId = deviceId;
        this.flowRateLitersPerMin = flowRateLitersPerMin;
        this.valveIsOpen = valveIsOpen;
        this.timestamp = timestamp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public double getFlowRateLitersPerMin() {
        return flowRateLitersPerMin;
    }

    public boolean getValveIsOpen() {
        return valveIsOpen;
    }

    public String getTimestamp() {
        return timestamp;
    }
}