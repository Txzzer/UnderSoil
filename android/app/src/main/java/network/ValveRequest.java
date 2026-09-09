package network;

public class ValveRequest {
    private boolean valveIsOpen;

    public ValveRequest(boolean valveIsOpen, int i) {
        this.valveIsOpen = valveIsOpen;
    }

    public boolean isValveIsOpen() {
        return valveIsOpen;
    }

    public void setValveIsOpen(boolean valveIsOpen) {
        this.valveIsOpen = valveIsOpen;
    }
}
