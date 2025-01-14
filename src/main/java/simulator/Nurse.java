package simulator;


public class Nurse extends Thread {
    
    private WaitingRoom waitingRoom;

    public Nurse(WaitingRoom waitingRoom, int id) {
        super("Nurse " + id);
        this.waitingRoom = waitingRoom;
    }


    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                // They attend patients in the general queue and they assign patients to specialties
                waitingRoom.takePatientFromQueue(this);
            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }
}
