package network;

public class ValveRequest {
    private boolean valveIsOpen;

    public ValveRequest(boolean valveIsOpen) {
        this.valveIsOpen = valveIsOpen;
    }

    public boolean isValveIsOpen() {
        return valveIsOpen;
    }

    public void setValveIsOpen(boolean valveIsOpen) {
        this.valveIsOpen = valveIsOpen;
    }
}
