package es.merida.tfg.gestion_proyectos.repository;

import es.merida.tfg.gestion_proyectos.model.Project;
import es.merida.tfg.gestion_proyectos.model.Task;
import es.merida.tfg.gestion_proyectos.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // =========================
    // Básicos por proyecto
    // =========================
    List<Task> findByProject(Project project);

    List<Task> findByProjectAndCompleted(Project project, boolean completed);

    List<Task> findByProjectAndAssignedUser(Project project, User user);

    List<Task> findByProjectAndCompletedAndAssignedUser(Project project, boolean completed, User user);

    // =========================
    // Seguridad / aislamiento por equipo
    // =========================
    Optional<Task> findByIdAndProjectTeamId(Long id, Long teamId);

    List<Task> findByProjectIdAndProjectTeamId(Long projectId, Long teamId);

    // =========================
    // Numeración de tareas
    // =========================
    @Query("""
        SELECT COALESCE(MAX(t.taskNumber), 0)
        FROM Task t
        WHERE t.project.id = :projectId
    """)
    int findMaxTaskNumberByProjectId(@Param("projectId") Long projectId);

    // =========================
    // Métricas (dashboard)
    // =========================
    long countByProjectTeamId(Long teamId);

    long countByProjectTeamIdAndCompleted(Long teamId, boolean completed);

    long countByProjectTeamIdAndCompletedFalseAndDueDateBefore(Long teamId, LocalDate date);

    // =========================
    // Próximas tareas del usuario (dashboard)
    // =========================
    @Query("""
        SELECT t FROM Task t
        WHERE t.project.team.id = :teamId
          AND t.assignedUser.username = :username
          AND t.completed = false
        ORDER BY t.dueDate ASC
    """)
    List<Task> findUpcomingTasksForUser(
            @Param("teamId") Long teamId,
            @Param("username") String username,
            Pageable pageable
    );

    // =========================
    // ReminderJob
    // =========================
    @Query("""
        SELECT t FROM Task t
        WHERE t.completed = false
          AND t.dueDate BETWEEN :start AND :end
    """)
    List<Task> findTasksDueBetween(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
        SELECT t FROM Task t
        WHERE t.completed = false
          AND t.dueDate < :date
    """)
    List<Task> findOverdueTasks(@Param("date") LocalDate date);

    // =========================
    // Calendario (vencimientos del mes)
    // =========================
    @Query("""
        SELECT t FROM Task t
        WHERE t.project.team.id = :teamId
          AND t.assignedUser.username = :username
          AND t.dueDate BETWEEN :start AND :end
        ORDER BY t.dueDate ASC
    """)
    List<Task> findTasksForUserBetweenDates(
            @Param("teamId") Long teamId,
            @Param("username") String username,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
    SELECT t FROM Task t
    WHERE t.project.team.id = :teamId
      AND t.dueDate BETWEEN :start AND :end
    ORDER BY t.dueDate ASC
    """)
    List<Task> findTasksForTeamBetweenDates(
            @Param("teamId") Long teamId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
