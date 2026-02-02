package es.merida.tfg.gestion_proyectos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    // 1 equipo -> 1 manager (usuario)
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "manager_user_id")
    private User manager;
}
