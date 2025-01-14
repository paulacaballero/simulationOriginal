package simulator;

import java.util.Random;
import java.util.concurrent.Semaphore;

public class Patient extends Thread{

    private WaitingRoom waitingRoom;
    private int specialty;
    private int waitingTime;
    private Semaphore doctorQSemaphore;
    private Semaphore generalQSemaphore;
    private Random rand;

    public Patient(WaitingRoom waitingRoom, int id) {
        super("Patient " + id);
        this.waitingRoom = waitingRoom;
        waitingTime = 0;
        doctorQSemaphore = new Semaphore(0);
        generalQSemaphore = new Semaphore(0);
        rand = new Random();
    }

    
    public Semaphore getDoctorQSemaphore() {
        return doctorQSemaphore;
    }


    public Semaphore getGeneralQSemaphore() {
        return generalQSemaphore;
    }


    public int getSpecialty() {
        return specialty;
    }

    public void setSpecialty(int specialty) {
        this.specialty = specialty;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(int turnsWaited) {
        this.waitingTime = turnsWaited;
    }

    @Override
    public void run() {
        try {
            // This is a way to scatter the patients in time
            if(this.getId() % 2 == 0)
                Thread.sleep(rand.nextInt(10000) + 2000);
            else
                Thread.sleep(rand.nextInt(10000));
            
            // The patient first enters the waiting room and gets attended by the triage
            waitingRoom.register(this);
            // Then waits for their turn with the nurses
            waitingRoom.waitUntilYourTurn(this);
            // Finally gets attended by a doctor
            waitingRoom.getsAttended(this);
        } catch (InterruptedException e) {
        }
    }

}
