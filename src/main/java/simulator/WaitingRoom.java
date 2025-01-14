package simulator;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class WaitingRoom {
    final static String RESET = "\u001B[0m";
    final static String RED = "\u001B[31m";
    final static String GREEN = "\u001B[32m";
    final static String YELLOW = "\u001B[33m";
    final static String ORANGE = "\u001B[34m";
    final static String PURPLE = "\u001B[35m";
    private Semaphore mutex;
    private Semaphore[] patientReady;
    private Semaphore[] doctorDone;
    private Semaphore[] patientDone;
    private Semaphore patientEntered;
    private Semaphore triageDone;
    private Semaphore[] caseInQueue;
    private Random rand;
    private PriorityBlockingQueue<Patient> generalQueue;
    private PriorityBlockingQueue<Patient>[] doctorQueue;
    private BlockingQueue<Patient> entranceQueue;
    private int numPatientsOutofGeneral;
    private int[] numPatientsOutofDoctor;
    public int totalWaitingTime;
    public int NUMDOCTOR;

    @SuppressWarnings("unchecked")
    public WaitingRoom(PriorityBlockingQueue<Patient> queue, int numDoctor) {
        mutex = new Semaphore(1);

        patientEntered = new Semaphore(0);
        triageDone = new Semaphore(0);
        caseInQueue = new Semaphore[numDoctor];
        rand = new Random();
        entranceQueue = new LinkedBlockingQueue<>();
        doctorQueue = new PriorityBlockingQueue[numDoctor];
        patientReady = new Semaphore[numDoctor];
        doctorDone = new Semaphore[numDoctor];
        patientDone = new Semaphore[numDoctor];
        numPatientsOutofDoctor = new int[numDoctor];
        for (int i = 0; i < numDoctor; i++) {
            caseInQueue[i] = new Semaphore(0);
            patientReady[i] = new Semaphore(0);
            doctorDone[i] = new Semaphore(0);
            patientDone[i] = new Semaphore(0);
            doctorQueue[i] = new PriorityBlockingQueue<>(
                    10, (t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()) // Descending order by priority
            );
            numPatientsOutofDoctor[i] = 0;
        }
        this.generalQueue = queue;
        numPatientsOutofGeneral = 0;
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

        generalQueue.add(patient);

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

        
        print(doctor, 5, ": ready", GREEN);

        int specialty = doctor.getSpecialty();
        // The doctor waits for a patient to be waiting in their queue
        caseInQueue[specialty].acquire();
        mutex.acquire();
        printQueue(doctorQueue[specialty], ORANGE);
        // The doctor takes a patient from their queue
        Patient patient = doctorQueue[specialty].poll();
        // and wakes them up
        patient.getDoctorQSemaphore().release();
        mutex.release();
        // The doctor waits until the patient signals them back
        patientReady[specialty].acquire();
        print(doctor, 5, ": treating", GREEN);
        Thread.sleep(rand.nextInt(1000) + 1000);

        // The treatment is done
        doctorDone[specialty].release();
        print(doctor, 5, ": treating done", GREEN);

        patientDone[specialty].acquire();
        print(doctor, 5, ": done\n", GREEN);

    }

    public void getsAttended(Patient patient) throws InterruptedException {

        // Waits until the doctor signals that is their turn
        patient.getDoctorQSemaphore().acquire();

        int specialty = patient.getSpecialty();
        patientReady[specialty].release();

        // Gets treated
        print(patient, 5, ": treating", RED);
        doctorDone[specialty].acquire();

        // Signals that they're done
        patientDone[specialty].release();
        print(patient, 5, ": treatment done", RED);

        mutex.acquire();
        // Calculates how much they have been waiting for their turn based on the quantity 
        // of patients their doctor has treated before them
        int waitedTime = patient.getWaitingTime();
        patient.setWaitingTime(waitedTime + (numPatientsOutofDoctor[specialty] * 30));
        numPatientsOutofDoctor[specialty]++;
        // As this is the last step of the patient before leaving the waited time is added to the overall waiting time
        calculateWaitingTime(patient);
        mutex.release();

    }

    private void calculateWaitingTime(Patient patient) {
        // We calculate the approximate waiting time
        double waitingTime = 0;
        waitingTime = patient.getWaitingTime();
        totalWaitingTime += waitingTime;
        print(patient, 7, "Waited " + waitingTime + " minutes", RESET);
    }

    public void waitUntilYourTurn(Patient patient) throws InterruptedException {

        print(patient, 2, ": waits.", RED);
        // Wait until a nurse signals that is their turn to be attended by them
        patient.getGeneralQSemaphore().acquire();

        mutex.acquire();
        // Calculates how much time they have been waiting in the general queue for their turn based on
        // the quantity of patients that have already been treated
        int waitedTime = patient.getWaitingTime();
        patient.setWaitingTime(waitedTime + 20 * numPatientsOutofGeneral);
        numPatientsOutofGeneral++;
        mutex.release();
        print(patient, 3, ": turn arrived.", RED);

    }

    public void takePatientFromQueue(Nurse nurse) throws InterruptedException {

        print(nurse, 2, ": about to take a pacient from the general queue.", PURPLE);
        Patient patient = null;
        // The nurse will remain in the loop until there is a patient in their queue
        while (true) {
            mutex.acquire();
            printQueue(generalQueue, RESET);
            // We take the patient with the highest priority from the general queue
            // with a timeout of 1 second
            patient = generalQueue.poll(1, TimeUnit.SECONDS);
            if (patient != null) {
                print(nurse, 2, ": " + patient.getName() + " is attended by the nurse.", PURPLE);
                // The nurse attends the patients and assigns them to a doctor (specialty)
                patient.setSpecialty(rand.nextInt(NUMDOCTOR));

                // And adds them to the queue of the respective doctor
                doctorQueue[patient.getSpecialty()].add(patient);

                // Signals the patient
                patient.getGeneralQSemaphore().release();
                // Signals the doctor
                caseInQueue[patient.getSpecialty()].release();

                mutex.release();
                break; // Get out of the while because the nurse has found a patient
            } else {
                print(nurse, 2, ": there is no patient in the queue.", GREEN);
                mutex.release();
                // If there is no patients in the queue the nurse waits for a fixed time (1
                // sec) before checking again
                Thread.sleep(1000);
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
