package simulator;

import java.util.Random;

public class Patient extends Thread {

    private WaitingRoom waitingRoom;
    private int specialty;
    private double priority;
    private Random rand;

    public Patient(WaitingRoom waitingRoom, int id) {
        super("Patient " + id);
        this.waitingRoom = waitingRoom;
        rand = new Random();
    }

    public int getSpecialty() {
        return specialty;
    }


    public void setSpecialty(int specialty) {
        this.specialty = specialty;
    }


    public double isPriority() {
        return priority;
    }


    public void setPriority(double priority) {
        this.priority = priority;
    }


    @Override
    public void run() {
        try {
            waitingRoom.register(this);
            waitingRoom.waitUntilYourTurn(this);
            waitingRoom.getsAttended(this);
        } catch (InterruptedException e) {
        }
    }

}
