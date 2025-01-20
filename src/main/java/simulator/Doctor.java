package simulator;

import java.util.concurrent.Semaphore;

public class Doctor extends Thread {

    private int specialty;
    private WaitingRoom waitingRoom;
    private int materialCount;
    private static final int MAX_MATERIAL = 3;
    private static Semaphore supplyStation = new Semaphore(2);

    public Doctor(WaitingRoom waitingRoom, int specialty, int id) {
        super("Doctor " + id);
        this.specialty = specialty;
        this.waitingRoom = waitingRoom;
        this.materialCount = MAX_MATERIAL; 
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
                if (materialCount == 0) {
                    restockMaterials();
                }
                // They attend patients in their queues
                waitingRoom.attend(this);
                materialCount--;
            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }

    private void restockMaterials() throws InterruptedException {
        System.out.println(getName() + " is out of materials and needs to restock.");
        supplyStation.acquire(); 
        System.out.println(getName() + " is restocking materials.");
        Thread.sleep(2000);
        materialCount = MAX_MATERIAL; 
        System.out.println(getName() + " has restocked materials and can treat more patients.");
        supplyStation.release(); 
    }
}
