package simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class WaitingRoom {
    final static String RESET = "\u001B[0m";
    final static String RED = "\u001B[31m";
    final static String GREEN = "\u001B[32m";
    final static String YELLOW = "\u001B[33m";
    final static int NUMSPECIALTIES = 3;
    final static int NUMPATIENTS = 20;
    private Semaphore mutex;
    private Semaphore patientReady;
    private Semaphore doctorDone;
    private Semaphore patientDone;
    private Semaphore patientEntered;
    private Semaphore triageDone;
    private Semaphore newCase;
    private Semaphore queueSemaphore;
    private Random rand;
    private PriorityBlockingQueue<Patient> queue;
    private long nextPatientId;
    private int numPatientsWaiting;

    public WaitingRoom(PriorityBlockingQueue<Patient> queue) {
        mutex = new Semaphore(1);
        patientReady = new Semaphore(0);
        doctorDone = new Semaphore(0);
        patientDone = new Semaphore(0);
        patientEntered = new Semaphore(0);
        triageDone = new Semaphore(0);
        newCase = new Semaphore(0);
        queueSemaphore = new Semaphore(0);
        rand = new Random();
        this.queue = queue;
        nextPatientId = -1;
        numPatientsWaiting = 0;
    }

    public void register(Patient patient) throws InterruptedException{

            // The patient registers their symptomps
            print(patient,0, ": enters the waiting room", RED);
            // The triage checks the entering patient
            patientEntered.release();
            triageDone.acquire();

        mutex.acquire();
            // The triage assigns a priority value to the patient
                patient.setPriority(rand.nextInt(1,5));
        
            // Once the triage staff validates the results the patient enters a queue
            print(patient,0, ": has been assigned to the queue with priority " + patient.getPriority(), RED);
            queue.add(patient);
        mutex.release();
            // Signals that there is a new case in the queue
            newCase.release();
    }

    public void initialPatientCheck(Triage triage) throws InterruptedException{

        // The triage staff checks The vital signs of the patient
        patientEntered.acquire();
        Thread.sleep(rand.nextInt(1000));
        print(triage,2, ": a patient is checked by the triage.", YELLOW);
        triageDone.release();
        
    }

    public void attend(Doctor doctor) throws InterruptedException {
        
        // The doctor signals their patients that is ready
        print(doctor,5, ": ready", GREEN);
        takePatientFromQueue(doctor);
        // And waits for the patient to awake
        patientReady.acquire();
        print(doctor,5, ": treating", GREEN);
        Thread.sleep(rand.nextInt(1000) + 1000);
        doctorDone.release();
         // The treatment is done
        print(doctor,5, ": treating done", GREEN);
        
        patientDone.acquire();
        print(doctor,5, ": done\n", GREEN);
        
    }

    public void getsAttended(Patient patient) throws InterruptedException {
        mutex.acquire();
            // The patient is the first in the queue
            // print(patient,0, ": is the first in the queue");
            patientReady.release();

            // Gets treated
            print(patient,5, ": treating", RED);
            doctorDone.acquire();
            
            patientDone.release();
            print(patient,5, ": treatment done", RED);
            // Signals that they're done
            
        mutex.release();
        
    }
    public void waitUntilYourTurn(Patient patient) throws InterruptedException{
        mutex.acquire(); 
            // While the patient isnt the one that comes out of the queue, wait
            numPatientsWaiting++;
            while(patient.getId() != nextPatientId){
                print(patient, 2, ": waits.", RED);
                mutex.release();
                queueSemaphore.acquire();
            }

        mutex.acquire();
        // Since the thread exited the queue one less is waiting
            numPatientsWaiting--;
            print(patient,3, ": turn arrived.", RED);
            printWaitingRoom();
        mutex.release();
        
    }

    private void takePatientFromQueue(Doctor doctor) throws InterruptedException{
        
        print(doctor,2, ": about to take a pacient from the queue.", GREEN);
        Patient patient = null;
        // The doctor will remain in the loop until there is a patient in their queue
        do{
            mutex.acquire();
            printWaitingRoom();
            // We take the patient with the highest priority from the queue of the doctor, with a timeout of 1 second
            patient = queue.poll(1, TimeUnit.SECONDS);
            if (patient != null) {
                // If there is a patient taken from the queue its id is saved in a variable to sort it from the other patients in the queue
                nextPatientId = patient.getId();
                print(doctor,2, ": call for the next patient in the queue",GREEN);
                // All the threads in the doctor's queue are signaled
                queueSemaphore.release(numPatientsWaiting);
                mutex.release();
            } else {
                print(doctor,2, ": there is no patient in the queue.", GREEN);
                mutex.release();
                // If there is no patients in the queue the doctor waits until there is a new case
                newCase.acquire();
            }
        } while(patient == null);
        
    }

    private void print(Thread thread, int id, String msg, String color) {
        
        String gap = new String(new char[id + 1]).replace('\0', '\t');
        System.out.println(gap + color + "🖥️" + id + " " + thread.getName() + ": " + msg + RESET);
    }
    private void printWaitingRoom(){
        String out = new String();
        for(Patient patient : queue){
            out += " " + patient.getName() + " |";
        }
    
        System.out.println(out);
    }

}
