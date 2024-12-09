package simulator;

import java.util.concurrent.PriorityBlockingQueue;

public class Doctor extends Thread {

    private int id;
    private int specialty;
    private WaitingRoom waitingRoom;

    public Doctor(WaitingRoom waitingRoom, int specialty, int id) {
        super("Doctor " + id);
        
        this.specialty = specialty;
        this.waitingRoom = waitingRoom;
        this.id = id;
    }
    

    public int getSpecialty() {
        return specialty;
    }


    public void setSpecialty(int specialty) {
        this.specialty = specialty;
    }


    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                waitingRoom.attend(this);
            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }
}
