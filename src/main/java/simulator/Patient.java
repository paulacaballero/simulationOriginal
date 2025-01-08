package simulator;

import java.util.Random;

public class Patient extends Thread{

    private WaitingRoom waitingRoom;
    private int specialty;
    private int turnsWaited;
    private Random rand;
    private double waitingTime;

    public Patient(WaitingRoom waitingRoom, int id) {
        super("Patient " + id);
        this.waitingRoom = waitingRoom;
        turnsWaited = 0;
        rand = new Random();
        waitingTime = 0;
    }

    public int getSpecialty() {
        return specialty;
    }


    public void setSpecialty(int specialty) {
        this.specialty = specialty;
    }

    public int getTurnsWaited() {
        return turnsWaited;
    }

    public void setTurnsWaited(int turnsWaited) {
        this.turnsWaited = turnsWaited;
    }

    public double getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(double waitingTime) {
        this.waitingTime = waitingTime;
    }
    @Override
    public void run() {
        try {
            
            Thread.sleep(rand.nextInt(10000));
            
            waitingRoom.register(this);
            waitingRoom.waitUntilYourTurn(this);
            waitingRoom.getsAttended(this);
        } catch (InterruptedException e) {
        }
    }

}
