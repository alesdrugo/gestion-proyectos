package es.merida.tfg.gestion_proyectos.controller;

import es.merida.tfg.gestion_proyectos.model.Task;
import es.merida.tfg.gestion_proyectos.repository.ProjectRepository;
import es.merida.tfg.gestion_proyectos.repository.TaskRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
public class DashboardController {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public DashboardController(ProjectRepository projectRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    @GetMapping({"/", "/dashboard"})
    public String home(Model model, HttpSession session) {

        String username = (String) session.getAttribute("username");
        if (username == null) return "redirect:/login";

        long totalProjects = projectRepository.count();
        long totalTasks = taskRepository.count();
        long completedTasks = taskRepository.countByCompleted(true);
        long pendingTasks = taskRepository.countByCompleted(false);
        long overdueTasks = taskRepository.countByCompletedFalseAndDueDateBefore(LocalDate.now());

        List<Task> upcomingTasks =
                taskRepository.findTop5ByAssignedUser_UsernameAndCompletedFalseOrderByDueDateAsc(username);

        model.addAttribute("username", username);
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));

        model.addAttribute("upcomingTasks", upcomingTasks);

        model.addAttribute("totalProjects", totalProjects);
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("overdueTasks", overdueTasks);

        model.addAttribute("chartLabels", new String[]{"Pendientes", "Completadas", "Vencidas"});
        model.addAttribute("chartData", new long[]{pendingTasks, completedTasks, overdueTasks});

        return "dashboard";
    }
}
