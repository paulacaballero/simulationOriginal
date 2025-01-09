package simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class WaitingRoom {
    final static String RESET = "\u001B[0m";
    final static String RED = "\u001B[31m";
    final static String GREEN = "\u001B[32m";
    final static String YELLOW = "\u001B[33m";
    private Semaphore mutex;
    private Semaphore patientReady;
    private Semaphore doctorDone;
    private Semaphore patientDone;
    private Semaphore patientEntered;
    private Semaphore triageDone;
    private Semaphore queueSemaphore;
    private Semaphore caseInQueue;
    private Random rand;
    private PriorityBlockingQueue<Patient> queue;
    private BlockingQueue<Patient> entranceQueue;
    private long nextPatientId;
    private int numPatientsWaiting;
    public int totalWaitingTime;
    public int NUMDOCTOR;

    public WaitingRoom(PriorityBlockingQueue<Patient> queue, int numDoctor) {
        mutex = new Semaphore(1);
        patientReady = new Semaphore(0);
        doctorDone = new Semaphore(0);
        patientDone = new Semaphore(0);
        patientEntered = new Semaphore(0);
        triageDone = new Semaphore(0);
        queueSemaphore = new Semaphore(0);
        caseInQueue = new Semaphore(0);
        rand = new Random();
        entranceQueue = new LinkedBlockingDeque<>();
        this.queue = queue;
        nextPatientId = -1;
        numPatientsWaiting = 0;
        NUMDOCTOR = numDoctor;
        totalWaitingTime = 0;
    }

    public void register(Patient patient) throws InterruptedException {

        // The patient enters the hospital
        print(patient, 0, ": enters the waiting room", RED);
        entranceQueue.add(patient);

        // The triage checks the entering patient
        patientEntered.release();
        triageDone.acquire();

        mutex.acquire();
        // Once the triage staff validates the results the patient enters a queue
        print(patient, 0, ": has been assigned to the queue with priority " + patient.getPriority(), RED);
        mutex.release();

        queue.add(patient);
        caseInQueue.release();
    }

    public void initialPatientCheck(Triage triage) throws InterruptedException {

        patientEntered.acquire();
        mutex.acquire();
        // The triage staff checks the vital signs of the patient
        Patient patient = entranceQueue.poll(1, TimeUnit.SECONDS);

        // The triage assigns a priority value to the patient
        patient.setPriority(rand.nextInt(1, 6));
        mutex.release();
        Thread.sleep(rand.nextInt(1000));
        print(triage, 2, ": " + patient.getName() + " is checked by the triage.", YELLOW);
        triageDone.release();
    }

    public void attend(Doctor doctor) throws InterruptedException {

        // The doctor signals their patients that is ready
        print(doctor, 5, ": ready", GREEN);
        takePatientFromQueue(doctor);
        // And waits for the patient to awake
        patientReady.acquire();
        print(doctor, 5, ": treating", GREEN);
        Thread.sleep(rand.nextInt(1000) + 1000);

        // The treatment is done
        doctorDone.release();
        print(doctor, 5, ": treating done", GREEN);

        patientDone.acquire();
        print(doctor, 5, ": done\n", GREEN);

    }

    public void getsAttended(Patient patient) throws InterruptedException {

        // The patient is the first in the queue
        patientReady.release();

        // Gets treated
        print(patient, 5, ": treating", RED);
        doctorDone.acquire();

        // Signals that they're done
        patientDone.release();
        print(patient, 5, ": treatment done", RED);
        mutex.acquire();
        calculateWaitingTime(patient);
        mutex.release();

    }

    private void calculateWaitingTime(Patient patient) {
        // We calculate the approximate waiting time, assuming that a patient is treated
        // in 30 minutes
        double waitingTime = 0;
        waitingTime = patient.getTurnsWaited() * 30;
        totalWaitingTime += waitingTime;
        print(patient, 7, "Waited " + waitingTime + " minutes", RESET);
    }

    public void waitUntilYourTurn(Patient patient) throws InterruptedException {
        boolean out = false;
        mutex.acquire();
        numPatientsWaiting++;
        mutex.release();
        // While the patient isnt the one that comes out of the queue, wait
        do {
            mutex.acquire();

            int turns = patient.getTurnsWaited();
            // Everytime a thread exits the loop the ones remaining count one more turn
            patient.setTurnsWaited(turns + 1);
            print(patient, 2, ": waits.", RED);
            mutex.release();

            queueSemaphore.acquire();

            mutex.acquire();
            out = patient.getId() == nextPatientId;
            mutex.release();
        } while (out == false);

        mutex.acquire();
        // Since the thread exited the queue one less is waiting
        numPatientsWaiting--;
        print(patient, 3, ": turn arrived.", RED);
        printQueue(queue, RESET);
        mutex.release();

    }

    private void takePatientFromQueue(Doctor doctor) throws InterruptedException {

        print(doctor, 2, ": about to take a pacient from the queue.", GREEN);
        Patient patient = null;
        // The doctor will remain in the loop until there is a patient in their queue
        while (true) {
            caseInQueue.acquire();

            mutex.acquire();
            printQueue(queue, RESET);
            // We take the patient with the highest priority from the queue of the doctor,
            // with a timeout of 1 second
            patient = queue.poll(1, TimeUnit.SECONDS);
            if (patient != null) {
                // If there is a patient taken from the queue its id is saved in a variable to
                // sort them from the other patients in the queue
                nextPatientId = patient.getId();
                print(doctor, 2, ": call for the next patient in the queue", GREEN);
                // All the threads in the doctor's queue are signaled
                queueSemaphore.release(numPatientsWaiting);

                mutex.release();
                break; // Get out of the while because the doctor has found a patient
            } else {
                print(doctor, 2, ": there is no patient in the queue.", GREEN);
                mutex.release();
                // If there is no patients in the queue the doctor waits for a fixed time (1
                // sec) before checking again
                // Thread.sleep(1000);
            }
        }

    }

    private void print(Thread thread, int id, String msg, String color) {

        String gap = new String(new char[id + 1]).replace('\0', '\t');
        System.out.println(gap + color + "🖥️" + id + " " + thread.getName() + ": " + msg + RESET);
    }

    private void printQueue(BlockingQueue<Patient> blockingQueue, String color) {
        String out = new String();
        for (Patient patient : blockingQueue) {
            out += " " + patient.getName() + " |";
        }
        System.out.println(color + out + RESET);
    }

    public int getTotalWaitingTime() {
        return totalWaitingTime;
    }

}
