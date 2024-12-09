package simulator;

public class Triage extends Thread {

    private WaitingRoom waitingRoom;
    private int id;

    public Triage(WaitingRoom waitingRoom, int id) {
        super("Triage " + id);
        this.waitingRoom = waitingRoom;
        this.id = id;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                waitingRoom.validate(this);
            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }
}
