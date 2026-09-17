package network;

public class ValveRequest {
    private boolean valveIsOpen;
    private int deviceIdInt;

    public ValveRequest() {
    }

    public ValveRequest(boolean valveIsOpen) {
        this.valveIsOpen = valveIsOpen;
    }

    public ValveRequest(boolean valveIsOpen, int deviceIdInt) {
        this.valveIsOpen = valveIsOpen;
        this.deviceIdInt = deviceIdInt;
    }

    public boolean isValveIsOpen() {
        return valveIsOpen;
    }

    public void setValveIsOpen(boolean valveIsOpen) {
        this.valveIsOpen = valveIsOpen;
    }

    public int getDeviceIdInt() {
        return deviceIdInt;
    }

    public void setDeviceIdInt(int deviceIdInt) {
        this.deviceIdInt = deviceIdInt;
    }
}
