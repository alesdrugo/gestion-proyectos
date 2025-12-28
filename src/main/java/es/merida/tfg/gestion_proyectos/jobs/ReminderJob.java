package es.merida.tfg.gestion_proyectos.jobs;

import es.merida.tfg.gestion_proyectos.model.Task;
import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.repository.TaskRepository;
import es.merida.tfg.gestion_proyectos.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ReminderJob {

    private final TaskRepository taskRepository;
    private final EmailService emailService;

    @Value("${app.reminders.enabled:true}")
    private boolean remindersEnabled;

    public ReminderJob(TaskRepository taskRepository, EmailService emailService) {
        this.taskRepository = taskRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyReminders() {
        if (!remindersEnabled) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        taskRepository.findByCompletedFalseAndDueDateBetween(tomorrow, tomorrow)
                .forEach(task -> notify(task, NotificationType.DUE_SOON));

        taskRepository.findByCompletedFalseAndDueDateBefore(today)
                .forEach(task -> notify(task, NotificationType.OVERDUE));
    }

    private enum NotificationType { DUE_SOON, OVERDUE }

    private void notify(Task task, NotificationType type) {
        User assignee = task.getAssignedUser();
        if (assignee == null) return;
        if (!assignee.isEnabled()) return;
        if (assignee.getEmail() == null || assignee.getEmail().isBlank()) return;

        if (type == NotificationType.DUE_SOON) {
            emailService.sendDueSoon(assignee, task);
        } else {
            emailService.sendOverdue(assignee, task);
        }
    }
}
