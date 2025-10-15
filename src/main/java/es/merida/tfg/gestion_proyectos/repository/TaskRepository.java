package es.merida.tfg.gestion_proyectos.repository;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import es.merida.tfg.gestion_proyectos.model.Project;
import es.merida.tfg.gestion_proyectos.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProject(Project project);
    @Query("SELECT COALESCE(MAX(t.taskNumber), 0) FROM Task t WHERE t.project = :project")
    int findMaxTaskNumberByProject(Project project);
}