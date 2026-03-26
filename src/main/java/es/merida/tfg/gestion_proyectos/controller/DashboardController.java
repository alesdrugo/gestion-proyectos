
package es.merida.tfg.gestion_proyectos.controller;

import es.merida.tfg.gestion_proyectos.model.Task;
import es.merida.tfg.gestion_proyectos.repository.ProjectRepository;
import es.merida.tfg.gestion_proyectos.repository.TaskRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

@Controller
public class DashboardController {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public DashboardController(ProjectRepository projectRepository,
                               TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    @GetMapping({"/", "/dashboard"})
    public String home(@RequestParam(required = false) Integer month,
                       Model model,
                       HttpSession session) {

        String username = (String) session.getAttribute("username");
        if (username == null) return "redirect:/login";

        if (Authz.hasRole(session, "ROLE_ADMIN")) {
            return "redirect:/admin/users";
        }

        Long teamId = Authz.teamId(session);
        if (teamId == null) return "redirect:/pending";

        
        // Métricas generales
        
        long totalProjects = projectRepository.findByTeamId(teamId).size();
        long totalTasks = taskRepository.countByProjectTeamId(teamId);
        long completedTasks = taskRepository.countByProjectTeamIdAndCompleted(teamId, true);
        long pendingTasks = taskRepository.countByProjectTeamIdAndCompleted(teamId, false);
        long overdueTasks = taskRepository
                .countByProjectTeamIdAndCompletedFalseAndDueDateBefore(teamId, LocalDate.now());

        model.addAttribute("totalProjects", totalProjects);
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("overdueTasks", overdueTasks);

        model.addAttribute("chartLabels", new String[]{"Pendientes", "Completadas", "Vencidas"});
        model.addAttribute("chartData", new long[]{pendingTasks, completedTasks, overdueTasks});

        
        //Próximas tareas (usuario)

        List<Task> upcomingTasks =
                taskRepository.findUpcomingTasksForUser(
                        teamId,
                        username,
                        PageRequest.of(0, 5)
                );
        model.addAttribute("upcomingTasks", upcomingTasks);

        /* =====================
           Calendario (equipo)
           ===================== */
        LocalDate today = LocalDate.now();
        YearMonth ym = (month != null)
                ? YearMonth.of(today.getYear(), month)
                : YearMonth.from(today);

        LocalDate calStart = ym.atDay(1);
        LocalDate calEnd = ym.atEndOfMonth();

        // Ajustar a semanas completas (lunes → domingo)
        while (calStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            calStart = calStart.minusDays(1);
        }
        while (calEnd.getDayOfWeek() != DayOfWeek.SUNDAY) {
            calEnd = calEnd.plusDays(1);
        }

        // IMPORTANTE: filtrar por teamId (y NO filtrar completed)
        List<Task> monthTasks = taskRepository.findTasksForTeamBetweenDates(teamId, calStart, calEnd);

        // Map por String yyyy-MM-dd (compatible con Thymeleaf indexando por String)
        Map<String, List<Task>> tasksByDate = new HashMap<>();
        for (Task t : monthTasks) {
            if (t.getDueDate() == null) continue;
            String key = t.getDueDate().toString(); // yyyy-MM-dd
            tasksByDate.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        // Construcción semanas
        List<List<LocalDate>> weeks = new ArrayList<>();
        LocalDate cursor = calStart;
        while (!cursor.isAfter(calEnd)) {
            List<LocalDate> week = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                week.add(cursor);
                cursor = cursor.plusDays(1);
            }
            weeks.add(week);
        }

        model.addAttribute("calWeeks", weeks);
        model.addAttribute("calTasksByDate", tasksByDate);

        model.addAttribute("calStart", calStart);
        model.addAttribute("calEnd", calEnd);
        model.addAttribute("calMonth", ym.getMonthValue());
        model.addAttribute("calYear", ym.getYear());
        model.addAttribute("calMonthLabel",
                ym.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES")));

        model.addAttribute("calPrevMonth", ym.minusMonths(1).getMonthValue());
        model.addAttribute("calNextMonth", ym.plusMonths(1).getMonthValue());

        model.addAttribute("username", username);
        model.addAttribute("isAdmin", false);

        return "dashboard";
    }
}