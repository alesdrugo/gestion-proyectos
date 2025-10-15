package es.merida.tfg.gestion_proyectos.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = true)
    private int taskNumber; // Número secuencial dentro del proyecto
    private String title;
    private String description;
    private boolean completed;
    private LocalDate dueDate;
     // Relación N tareas -> 1 proyecto
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;
}
