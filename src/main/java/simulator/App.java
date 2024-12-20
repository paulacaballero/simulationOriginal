package simulator;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * TMS simulator
 * Author: Team 4
 *
 */
public class App {

    final static int NUMDOCTOR = 3;
    final static int NUMPATIENTS = 20;
    final static int NUMTRIAGE = 2;

    private Doctor doctors[];
    private Patient patients[];
    private Triage triage[];
    private PriorityBlockingQueue<Patient> queue;
    private WaitingRoom waitingRoom;

    public App() {

        // Initialize the queues
        queue = new PriorityBlockingQueue<>(
                10, (t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()) // Descending order by priority
            );
        // Initialize the waiting room
        waitingRoom = new WaitingRoom(queue, NUMDOCTOR);

        // Initialize the doctors
        doctors = new Doctor[NUMDOCTOR];
        for (int i = 0; i < NUMDOCTOR; i++) {
            doctors[i] = new Doctor(waitingRoom, i, i);
        }

        // Initialize the patients
        patients = new Patient[NUMPATIENTS];
        for (int i = 0; i < NUMPATIENTS; i++) {
            patients[i] = new Patient(waitingRoom, i);
        }

        // Initialize the triage
        triage = new Triage[NUMTRIAGE];
        for(int i = 0; i < NUMTRIAGE; i++){
            triage[i] = new Triage(waitingRoom, i);
        }
    }

    public void startThreads() {
        // Start patient threads
        for (int i = 0; i < NUMPATIENTS; i++) {
            patients[i].start();
        }

        // Start triage threads
        for(int i = 0; i < NUMTRIAGE; i++){
            triage[i].start();
        }

        // Start doctor threads
        for (int i = 0; i < NUMDOCTOR; i++) {
            doctors[i].start();
        }
        
    }

    public void waitEndOfThreads() {
        try {
            // Stop patient threads
            for (int i = 0; i < NUMPATIENTS; i++) {
                patients[i].join();
            }

            // Stop doctor threads
            for (int i = 0; i < NUMDOCTOR; i++) {
                doctors[i].interrupt();
            }
            for (int i = 0; i < NUMDOCTOR; i++) {
                doctors[i].join();
            }

            // Stop triage threads
            for (int i = 0; i < NUMTRIAGE; i++) {
                triage[i].interrupt();
            }
            for (int i = 0; i < NUMTRIAGE; i++) {
                triage[i].join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void calculateAvgWaitingTime(){
        int waitingMinutes = waitingRoom.getTotalWaitingTime();
        waitingMinutes = waitingMinutes / NUMPATIENTS;
        System.out.println("The average waiting time has been "+ waitingMinutes+" minutes.");
    }
    public static void main(String[] args) {

        App app = new App();

        app.startThreads();
        app.waitEndOfThreads();
        app.calculateAvgWaitingTime();
    }
}
