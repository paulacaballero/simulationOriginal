package simulator;

import java.util.concurrent.PriorityBlockingQueue;

public class Doctor extends Thread implements Comparable<Doctor> {
    private int specialty;
    private int priorityDoc; // Nivel de prioridad
    private WaitingRoom waitingRoom;
    private ServiceStation serviceStation; // Referencia a la estación de reposición
    private int materials;

    public Doctor(WaitingRoom waitingRoom, int specialty, int id, int priorityDoc, ServiceStation serviceStation) {
        super("Doctor " + id);
        this.specialty = specialty;
        this.priorityDoc = priorityDoc;
        this.waitingRoom = waitingRoom;
        this.serviceStation = serviceStation;
        this.materials = 3; // Inicia con materiales para 3 pacientes
    }

    public int getSpecialty() {
        return specialty;
    }
    public int getPriorityDoc() {
        return priorityDoc;
    }

    
    @Override
    public int compareTo(Doctor other) {
        // Comparación para la cola de prioridad: menor valor = mayor prioridad
        return Integer.compare(this.priorityDoc, other.priorityDoc);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                if (materials == 0) {
                    // Si no hay materiales, ir a la estación de reposición
                    serviceStation.restockMaterials(this);
                    materials = 3; // Recupera materiales para 3 pacientes
                }
                // Atender pacientes
                waitingRoom.attend(this);
                materials--; // Consume un material por paciente
            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }
}
