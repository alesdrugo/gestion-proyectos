package es.merida.tfg.gestion_proyectos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import es.merida.tfg.gestion_proyectos.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}