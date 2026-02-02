package es.merida.tfg.gestion_proyectos.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import es.merida.tfg.gestion_proyectos.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByTeamId(Long teamId);
    Optional<Project> findByIdAndTeamId(Long id, Long teamId);
    long countByTeamIdAndStatus(Long teamId, String status);
    //long countByStatus(String status); // "Pendiente", "En curso", "Completado"
    long countByTeamId(Long teamId);

}