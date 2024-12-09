package simulator;

import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WaitingRoom {
    final static int NUMSPECIALTIES = 3;
    final static int NUMPATIENTS = 20;
    private Lock mutex;
    private Event patientReady;
    private Event doctorDone;
    private Event patientDone;
    private Event patientRegistered;
    private Event triageValidated;
    private Event waitTurn;
    private Event newCaseInQueue;
    private Random rand;
    private PriorityBlockingQueue<Patient>[] queues;
    private long[] nextPatientId;

    public WaitingRoom(PriorityBlockingQueue<Patient>[] queues) {
        mutex = new ReentrantLock();
        patientReady = new Event(mutex.newCondition());
        doctorDone = new Event(mutex.newCondition());
        patientDone = new Event(mutex.newCondition());
        patientRegistered = new Event(mutex.newCondition());
        triageValidated = new Event(mutex.newCondition());
        waitTurn = new Event(mutex.newCondition());
        newCaseInQueue = new Event(mutex.newCondition());
        rand = new Random();
        this.queues = queues;
        nextPatientId = new long[NUMSPECIALTIES];
    }

    public void register(Patient patient) throws InterruptedException{
        
        mutex.lock();
        try{
            // The patient registers their symptomps
            Thread.sleep(rand.nextInt(1000));
            print(patient,0, ": registers");
            // The model assigns a specialty and a priority value
            patient.setSpecialty(rand.nextInt(NUMSPECIALTIES));
            patient.setPriority(rand.nextDouble(10));
            // The triage has to validate the results of the model
            patientRegistered.eSignal();
            triageValidated.eWait();
            // Once the triage staff validates the results the patient enters a queue
            entersQueue(patient);
        }finally{
            mutex.unlock();
        }
    }
    public void validate(Triage triage) throws InterruptedException{
        mutex.lock();
        try{
            // The triage staff checks if the values of the model are correct
            patientRegistered.eWaitAndReset();
            Thread.sleep(rand.nextInt(1000));
            print(triage,2, ": The prediction has been validated");
            triageValidated.eSignal();
        }finally{
            mutex.unlock();
        }
        
    }

    public void attend(Doctor doctor) throws InterruptedException {
        mutex.lock();
        try {
            // The doctor signals that is ready
            print(doctor,1, ": ready");
            takePatientFromQueue(doctor);
            // And waits for the patient to awake
            patientReady.eWaitAndReset();
            print(doctor,1, ": treating");
            Thread.sleep(rand.nextInt(1000) + 1000);
            print(doctor,1, ": treating done");
            // The treatment is done
            doctorDone.eSignal();
            patientDone.eWaitAndReset();
            print(doctor,1, ": done\n");
        } finally {
            mutex.unlock();
        }
    }

    public void getsAttended(Patient patient) throws InterruptedException {
        mutex.lock();
        try {
            print(patient,0, ": is the first in the queue");
            patientReady.eSignal();
            doctorDone.eWaitAndReset();
            print(patient,0, ": treating");
            Thread.sleep(rand.nextInt(1000) + 100);
            print(patient,0, ": treatment done");
            patientDone.eSignal();
        } finally {
            mutex.unlock();
        }
    }
    public void waitUntilYourTurn(Patient patient) throws InterruptedException{
        mutex.lock();
        try{
            /* While the patient isnt the one that comes out of the queue, wait */
            while(patient.getId() != nextPatientId[patient.getSpecialty()]){
                print(patient, 0, ": waits.");
                waitTurn.eWaitAndReset();
            }
            print(patient,0, ": turn arrived.");
        }finally{
            mutex.unlock();
        }
    }

    private void takePatientFromQueue(Doctor doctor) throws InterruptedException{
        
            print(doctor,1, ": about to take a pacient from the queue.");
            Patient patient = null;
            // nextPatientId[doctor.getSpecialty()] = 0;
            do{
                patient = queues[doctor.getSpecialty()].poll(1, TimeUnit.SECONDS);
                if (patient != null) {
                    nextPatientId[doctor.getSpecialty()] = patient.getId();
                    print(doctor,1, ": signal all patients that one is out.");
                    waitTurn.eSignalAll();
                }else{
                    print(doctor,1, ": has no patients in their queue.");
                    // nextPatientId[doctor.getSpecialty()] = -1;
                }
            } while(patient == null);
        print(doctor, 1, " out of the loop.");
        

    }
    public void entersQueue(Patient patient) throws InterruptedException{
        // mutex.lock();
        // try{
            print(patient,0, ": has been assigned to queue " + patient.getSpecialty() + " with priority " + patient.getPriority());
            queues[patient.getSpecialty()].add(patient);
            newCaseInQueue.eSignalAll();//

        // }finally{
        //     mutex.unlock();
        // }
        /* Cuando todos los pacientes han entrado en una cola ya no se avisa a los doctores que hay nuevos pacientes */
        
    }

    private void print(Thread thread, int id, String msg) {
        
        String gap = new String(new char[id + 1]).replace('\0', '\t');
        System.out.println(gap + "🖥️" + id + " " + thread.getName() + ": " + msg);
    }

}
