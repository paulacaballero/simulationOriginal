package simulator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * TMS simulator
 * Author: Team 4
 *
 */
public class App {

    final static int NUMDOCTOR = 10;
    final static int NUMPATIENTS = 20;
    final static int NUMTRIAGE = 5;
    final static int NUMNURSES = 10;
    final static int NUMSPECIALTY = 10;

    private Doctor[] doctors;
    private Patient[] patients;
    private Triage[] triage;
    private Nurse[] nurses;
    private PriorityBlockingQueue<Patient> queue;
    private WaitingRoom waitingRoom;
    private ServiceStation serviceStation;

    public App() {

        // Initialize the queues
        serviceStation = new ServiceStation();
        queue = new PriorityBlockingQueue<>(
            NUMPATIENTS, (t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()) // Descending order by priority
        );
        // Initialize the waiting room
        waitingRoom = new WaitingRoom(queue, NUMDOCTOR);

        // Initialize the doctors
        doctors = new Doctor[NUMDOCTOR];
        for (int i = 0; i < NUMDOCTOR; i++) {
            int priority = (i % 3) + 1; // Priority (1, 2, o 3)
            int specialty = (i % NUMSPECIALTY); //Specialty 0, 1 y 2
            doctors[i] = new Doctor(waitingRoom, specialty, i, priority, serviceStation);
        }

        // Initialize the patients
        patients = new Patient[NUMPATIENTS];
        for (int i = 0; i < NUMPATIENTS; i++) {
            patients[i] = new Patient(waitingRoom, i);
        }

        nurses = new Nurse[NUMNURSES];
        for (int i = 0; i < NUMNURSES; i++){
            nurses[i] = new Nurse(waitingRoom, i);
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

        for (int i = 0; i < NUMNURSES; i++){
            nurses[i].start();
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

            // Stop nurse threads
            for (int i = 0; i < NUMNURSES; i++){
                nurses[i].interrupt();
            }
            for (int i = 0; i < NUMNURSES; i++){
                nurses[i].join();
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
public void calculateAvgWaitingTime() {
        int waitingMinutes = waitingRoom.getTotalWaitingTime();
        waitingMinutes = waitingMinutes / NUMPATIENTS;
        saveAverageWaitingTimeToFile(waitingMinutes);
    }

    private void saveAverageWaitingTimeToFile(int averageWaitingTime) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/results/10doc20patOri.txt", true))) {
            writer.write(String.valueOf(averageWaitingTime));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        for(int i =0; i<1;i++){
            App app = new App();

            app.startThreads();
            app.waitEndOfThreads();
            app.calculateAvgWaitingTime();
        }

    }
}
