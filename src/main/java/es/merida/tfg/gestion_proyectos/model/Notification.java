package es.merida.tfg.gestion_proyectos.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario destinatario de la notificación */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Mensaje a mostrar en la campanita */
    @Column(nullable = false, length = 200)
    private String message;

    /** Enlace opcional (ej: /tasks/view/123) */
    @Column(length = 300)
    private String link;

    /** Leída o no */
    @Column(nullable = false)
    private boolean read = false;

    /** Marca temporal */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}