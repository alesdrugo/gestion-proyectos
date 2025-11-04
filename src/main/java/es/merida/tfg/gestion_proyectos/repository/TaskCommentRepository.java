package es.merida.tfg.gestion_proyectos.repository;

import es.merida.tfg.gestion_proyectos.model.Task;
import es.merida.tfg.gestion_proyectos.model.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    List<TaskComment> findByTaskOrderByCreatedAtDesc(Task task);
}