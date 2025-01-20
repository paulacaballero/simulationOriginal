package simulator;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;

public class ServiceStation {
    private PriorityBlockingQueue<Doctor> doctorQueue; // Cola de prioridad para los doctores
    private Semaphore availableSpot; // Controla acceso simultáneo (solo 1 doctor puede reponer a la vez)

    public ServiceStation() {
        doctorQueue = new PriorityBlockingQueue<>();
        availableSpot = new Semaphore(1); // Una única estación de reposición
    }

    public void restockMaterials(Doctor doctor) throws InterruptedException {
        // Agregar doctor a la cola de prioridad
        doctorQueue.add(doctor);

        System.out.println(doctor.getName() + " está esperando para reponer materiales.");

        // Esperar turno según la prioridad
        synchronized (this) {
            while (doctorQueue.peek() != doctor) {
                wait();
            }
        }

        // Adquirir acceso a la estación de reposición
        availableSpot.acquire();

        // Doctor en reposición
        System.out.println(doctor.getName() + " está reponiendo materiales.");
        Thread.sleep(1000); // Simula el tiempo de reposición

        // Salir de la estación
        System.out.println(doctor.getName() + " ha terminado de reponer materiales.");
        availableSpot.release();

        // Retirar al doctor de la cola y notificar a los demás
        synchronized (this) {
            doctorQueue.poll();
            notifyAll();
        }
    }
}
