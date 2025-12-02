package es.merida.tfg.gestion_proyectos.repository;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.model.Project;
import es.merida.tfg.gestion_proyectos.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProject(Project project);
    List<Task> findByProjectAndCompleted(Project project, boolean completed);
    List<Task> findByProjectAndAssignedUser(Project project, User user);
    List<Task> findByProjectAndCompletedAndAssignedUser(Project project, boolean completed, User user);
    List<Task> findByProjectAndCompletedFalseAndDueDateBefore(Project project, LocalDate date);

    @Query("SELECT COALESCE(MAX(t.taskNumber), 0) FROM Task t WHERE t.project = :project")
    int findMaxTaskNumberByProject(Project project);
    long countByCompleted(boolean completed);
    long countByCompletedFalseAndDueDateBefore(java.time.LocalDate date);

    List<Task> findTop5ByAssignedUser_UsernameAndCompletedFalseOrderByDueDateAsc(String username);

    // Tareas que vencen en un rango (hoy..fecha)
    List<Task> findByCompletedFalseAndDueDateBetween(LocalDate from, LocalDate to);

    // Tareas vencidas (fecha límite < hoy)
    List<Task> findByCompletedFalseAndDueDateBefore(LocalDate date);

    
}