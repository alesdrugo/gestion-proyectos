package es.merida.tfg.gestion_proyectos.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "task_comments")
public class TaskComment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length = 2000)
    private String text;

    @Column(nullable=false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(optional=false, fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(optional=false, fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;
}