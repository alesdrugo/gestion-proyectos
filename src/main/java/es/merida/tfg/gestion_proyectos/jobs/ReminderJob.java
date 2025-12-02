package es.merida.tfg.gestion_proyectos.jobs;

import es.merida.tfg.gestion_proyectos.model.Task;
import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.repository.TaskRepository;
import es.merida.tfg.gestion_proyectos.service.EmailService;
import es.merida.tfg.gestion_proyectos.service.UserService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Objects;

@Component
public class ReminderJob {

    private final TaskRepository taskRepository;
    private final EmailService emailService;
    private final UserService userService;
    @Value("${app.reminders.enabled:true}")
    private boolean remindersEnabled;

    public ReminderJob(TaskRepository taskRepository,
                       EmailService emailService,
                       UserService userService) {
        this.taskRepository = taskRepository;
        this.emailService = emailService;
        this.userService = userService;
    }

    // Todos los días a las 09:00 (hora del servidor)
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyReminders() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        // Próximas a vencer (mañana)
        taskRepository.findByCompletedFalseAndDueDateBetween(tomorrow, tomorrow)
                .forEach(t -> notifyIfAllowed(t, NotificationType.DUE_SOON));

        // Vencidas (fecha < hoy)
        taskRepository.findByCompletedFalseAndDueDateBefore(today)
                .forEach(t -> notifyIfAllowed(t, NotificationType.OVERDUE));
    }

    private enum NotificationType { DUE_SOON, OVERDUE }

    private void notifyIfAllowed(Task task, NotificationType type) {
        User assignee = task.getAssignedUser();
        if (assignee == null) return;
        if (!assignee.isEnabled()) return;     // no avisar a usuarios desactivados
        if (assignee.getEmail() == null) return;

        switch (type) {
            case DUE_SOON -> emailService.sendDueSoon(assignee, task);
            case OVERDUE -> emailService.sendOverdue(assignee, task);
        }
    }
}