package es.merida.tfg.gestion_proyectos.controller;

import es.merida.tfg.gestion_proyectos.model.Project;
import es.merida.tfg.gestion_proyectos.model.Task;
import es.merida.tfg.gestion_proyectos.model.TaskComment;
import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.repository.ProjectRepository;
import es.merida.tfg.gestion_proyectos.repository.TaskCommentRepository;
import es.merida.tfg.gestion_proyectos.repository.TaskRepository;
import es.merida.tfg.gestion_proyectos.repository.UserRepository;
import es.merida.tfg.gestion_proyectos.service.EmailService;
import es.merida.tfg.gestion_proyectos.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    public TaskController(TaskRepository taskRepository,
                          ProjectRepository projectRepository,
                          UserRepository userRepository,
                          TaskCommentRepository taskCommentRepository,
                          EmailService emailService,
                          NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.taskCommentRepository = taskCommentRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    // =========================
    // LIST
    // =========================
    @GetMapping("/{projectId}")
    public String listTasks(@PathVariable Long projectId,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) Long assigneeId,
                            Model model,
                            HttpSession session) {

        String username = Authz.username(session);
        if (username == null) return "redirect:/login";

        // Admin no opera tareas
        if (Authz.hasRole(session, "ROLE_ADMIN")) return "redirect:/admin/users";

        Long teamId = Authz.teamId(session);
        if (teamId == null) return "redirect:/pending";

        boolean isManager = Authz.hasRole(session, "ROLE_MANAGER");

        Project project = projectRepository.findByIdAndTeamId(projectId, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));

        // Cargamos el usuario actual (para filtrar si es ROLE_USER)
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) return "redirect:/login";

        // Users del equipo solo para el selector del manager
        List<User> teamUsers = isManager ? userRepository.findByTeamId(teamId) : List.of();

        User assigneeFilter = null;
        if (isManager && assigneeId != null) {
            assigneeFilter = userRepository.findByIdAndTeamId(assigneeId, teamId).orElse(null);
        }

        List<Task> tasks = resolveTasks(project, status, isManager, currentUser, assigneeFilter);

        model.addAttribute("project", project);
        model.addAttribute("tasks", tasks);
        model.addAttribute("users", teamUsers);

        model.addAttribute("username", username);
        model.addAttribute("isManager", isManager);

        // filtros (solo manager usa assigneeId)
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterAssigneeId", isManager ? assigneeId : null);

        return "tasks/list";
    }

    // =========================
    // VIEW
    // =========================
    @GetMapping("/view/{taskId}")
    public String viewTask(@PathVariable Long taskId, Model model, HttpSession session) {

        String username = Authz.username(session);
        if (username == null) return "redirect:/login";

        if (Authz.hasRole(session, "ROLE_ADMIN")) return "redirect:/admin/users";

        Long teamId = Authz.teamId(session);
        if (teamId == null) return "redirect:/pending";

        boolean isManager = Authz.hasRole(session, "ROLE_MANAGER");

        Task task = taskRepository.findByIdAndProjectTeamId(taskId, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));

        boolean isOwner = task.getAssignedUser() != null
                && task.getAssignedUser().getUsername().equals(username);

        // USER solo puede ver su tarea
        if (!isManager && !isOwner) {
            return "redirect:/tasks/" + task.getProject().getId();
        }

        var comments = taskCommentRepository.findByTaskOrderByCreatedAtDesc(task);

        model.addAttribute("task", task);
        model.addAttribute("comments", comments);
        model.addAttribute("newComment", new TaskComment());

        model.addAttribute("username", username);
        model.addAttribute("isManager", isManager);

        return "tasks/view";
    }

    // =========================
    // COMMENT
    // =========================
    @PostMapping("/{taskId}/comment")
    public String addComment(@PathVariable Long taskId,
                             @ModelAttribute("newComment") TaskComment newComment,
                             HttpSession session) {

        String username = Authz.username(session);
        if (username == null) return "redirect:/login";

        if (Authz.hasRole(session, "ROLE_ADMIN")) return "redirect:/admin/users";

        Long teamId = Authz.teamId(session);
        if (teamId == null) return "redirect:/pending";

        boolean isManager = Authz.hasRole(session, "ROLE_MANAGER");

        User author = userRepository.findByUsername(username).orElse(null);
        if (author == null) return "redirect:/login";

        Task task = taskRepository.findByIdAndProjectTeamId(taskId, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));

        boolean isOwner = task.getAssignedUser() != null
                && task.getAssignedUser().getUsername().equals(username);

        // USER solo comenta en su tarea
        if (!isManager && !isOwner) {
            return "redirect:/tasks/" + task.getProject().getId();
        }

        if (newComment.getText() == null || newComment.getText().trim().isEmpty()) {
            return "redirect:/tasks/view/" + taskId;
        }

        newComment.setTask(task);
        newComment.setAuthor(author);
        taskCommentRepository.save(newComment);

        if (task.getAssignedUser() != null && !task.getAssignedUser().equals(author)) {
            notificationService.create(
                    task.getAssignedUser(),
                    "Nuevo comentario en tu tarea: " + task.getTitle(),
                    "/tasks/view/" + task.getId()
            );
        }

        return "redirect:/tasks/view/" + taskId;
    }

    // =========================
    // MANAGER CRUD
    // =========================
    @GetMapping("/add/{projectId}")
    public String showAddForm(@PathVariable Long projectId, Model model, HttpSession session) {

        String username = Authz.username(session);
        if (username == null) return "redirect:/login";
        if (!Authz.hasRole(session, "ROLE_MANAGER")) return "redirect:/projects";

        Long teamId = Authz.teamId(session);
        if (teamId == null) return "redirect:/pending";

        Project project = projectRepository.findByIdAndTeamId(projectId, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));

        Task task = new Task();
        task.setProject(project);

        model.addAttribute("project", project);
        model.addAttribute("task", task);
        model.addAttribute("users", userRepository.findByTeamId(teamId));
        model.addAttribute("username", username);
        model.addAttribute("isManager", true);

        return "tasks/form";
    }

    @PostMapping(value = "/add/{projectId}", consumes = {"multipart/form-data"})
    public String addTask(@PathVariable Long projectId,
                          @ModelAttribute Task task,
                          @RequestParam(value = "file", required = false) MultipartFile file,
                          HttpSession session) throws IOException {

        String username = Authz.username(session);
        if (username == null) return "redirect:/login";
        if (!Authz.hasRole(session, "ROLE_MANAGER")) return "redirect:/projects";

        Long teamId = Authz.teamId(session);
        if (teamId == null) return "redirect:/pending";

        Project project = projectRepository.findByIdAndTeamId(projectId, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));

        int nextNumber = taskRepository.findMaxTaskNumberByProjectId(projectId) + 1;

        task.setTaskNumber(nextNumber);
        task.setProject(project);

        User assignee = resolveAssigneeFromTeam(task, teamId);
        task.setAssignedUser(assignee);

        if (file != null && !file.isEmpty()) {
            String url = saveFileAndGetPublicUrl(file);
            task.setAttachmentPath(url);
        }

        Task saved = taskRepository.save(task);

        if (assignee != null) {
            emailService.sendTaskAssigned(assignee, saved);
            notificationService.create(
                    assignee,
                    "Se te ha asignado la tarea: " + saved.getTitle(),
                    "/tasks/view/" + saved.getId()
            );
        }

        return "redirect:/tasks/" + projectId;
    }

    @PostMapping(value = "/{taskId}/upload", consumes = {"multipart/form-data"})
    public String uploadAttachment(@PathVariable Long taskId,
                                   @RequestParam("file") MultipartFile file,
                                   HttpSession session) throws IOException {

        String username = Authz.username(session);
        if (username == null) return "redirect:/login";
        if (!Authz.hasRole(session, "ROLE_MANAGER")) return "redirect:/projects";

        Long teamId = Authz.teamId(session);
        if (teamId == null) return "redirect:/pending";

        Task task = taskRepository.findByIdAndProjectTeamId(taskId, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));

        if (file != null && !file.isEmpty()) {
            String url = saveFileAndGetPublicUrl(file);
            task.setAttachmentPath(url);
            taskRepository.save(task);
        }

        return "redirect:/tasks/" + task.getProject().getId();
    }

    @GetMapping("/toggle/{taskId}")
    public String toggleTask(@PathVariable Long taskId, HttpSession session) {

        String username = Authz.username(session);
        if (username == null) return "redirect:/login";

        if (Authz.hasRole(session, "ROLE_ADMIN")) return "redirect:/admin/users";

        Long teamId = Authz.teamId(session);
        if (teamId == null) return "redirect:/pending";

        Task task = taskRepository.findByIdAndProjectTeamId(taskId, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));

        boolean isManager = Authz.hasRole(session, "ROLE_MANAGER");
        boolean isOwner = task.getAssignedUser() != null
                && task.getAssignedUser().getUsername().equals(username);

        if (!isManager && !isOwner) {
            return "redirect:/tasks/" + task.getProject().getId();
        }

        task.setCompleted(!task.isCompleted());
        taskRepository.save(task);

        return "redirect:/tasks/" + task.getProject().getId();
    }

    @GetMapping("/edit/{taskId}")
    public String showEditForm(@PathVariable Long taskId, Model model, HttpSession session) {

        String username = Authz.username(session);
        if (username == null) return "redirect:/login";
        if (!Authz.hasRole(session, "ROLE_MANAGER")) return "redirect:/projects";

        Long teamId = Authz.teamId(session);
        if (teamId == null) return "redirect:/pending";

        Task task = taskRepository.findByIdAndProjectTeamId(taskId, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));

        model.addAttribute("task", task);
        model.addAttribute("project", task.getProject());
        model.addAttribute("users", userRepository.findByTeamId(teamId));
        model.addAttribute("username", username);
        model.addAttribute("isManager", true);

        return "tasks/form";
    }

    @PostMapping(value = "/update/{taskId}", consumes = {"multipart/form-data"})
    public String updateTask(@PathVariable Long taskId,
                             @ModelAttribute Task updatedTask,
                             @RequestParam(value = "file", required = false) MultipartFile file,
                             HttpSession session) throws IOException {

        String username = Authz.username(session);
        if (username == null) return "redirect:/login";
        if (!Authz.hasRole(session, "ROLE_MANAGER")) return "redirect:/projects";

        Long teamId = Authz.teamId(session);
        if (teamId == null) return "redirect:/pending";

        Task existingTask = taskRepository.findByIdAndProjectTeamId(taskId, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));

        Long previousAssigneeId = existingTask.getAssignedUser() != null
                ? existingTask.getAssignedUser().getId()
                : null;

        existingTask.setTitle(updatedTask.getTitle());
        existingTask.setDescription(updatedTask.getDescription());
        existingTask.setCompleted(updatedTask.isCompleted());
        existingTask.setDueDate(updatedTask.getDueDate());

        User newAssignee = null;
        if (updatedTask.getAssignedUser() != null && updatedTask.getAssignedUser().getId() != null) {
            newAssignee = userRepository.findByIdAndTeamId(updatedTask.getAssignedUser().getId(), teamId).orElse(null);
        }
        existingTask.setAssignedUser(newAssignee);

        if (file != null && !file.isEmpty()) {
            String url = saveFileAndGetPublicUrl(file);
            existingTask.setAttachmentPath(url);
        }

        taskRepository.save(existingTask);

        Long newAssigneeId = (newAssignee != null) ? newAssignee.getId() : null;
        boolean assigneeChanged = (previousAssigneeId == null && newAssigneeId != null)
                || (previousAssigneeId != null && !previousAssigneeId.equals(newAssigneeId));

        if (assigneeChanged && newAssignee != null) {
            emailService.sendTaskAssigned(newAssignee, existingTask);
        }

        return "redirect:/tasks/" + existingTask.getProject().getId();
    }

    @GetMapping("/delete/{taskId}")
    public String deleteTask(@PathVariable Long taskId, HttpSession session) {

        String username = Authz.username(session);
        if (username == null) return "redirect:/login";
        if (!Authz.hasRole(session, "ROLE_MANAGER")) return "redirect:/projects";

        Long teamId = Authz.teamId(session);
        if (teamId == null) return "redirect:/pending";

        Task task = taskRepository.findByIdAndProjectTeamId(taskId, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));

        Long projectId = task.getProject().getId();
        taskRepository.delete(task);

        return "redirect:/tasks/" + projectId;
    }

    // =========================
    // HELPERS
    // =========================
    private List<Task> resolveTasks(Project project, String status, boolean isManager, User currentUser, User assigneeFilter) {

        // USER: siempre sus tareas, ignorando filtro de asignado
        if (!isManager) {
            if ("completada".equalsIgnoreCase(status)) {
                return taskRepository.findByProjectAndCompletedAndAssignedUser(project, true, currentUser);
            }
            if ("pendiente".equalsIgnoreCase(status)) {
                return taskRepository.findByProjectAndCompletedAndAssignedUser(project, false, currentUser);
            }
            return taskRepository.findByProjectAndAssignedUser(project, currentUser);
        }

        // MANAGER: filtros completos
        if ("completada".equalsIgnoreCase(status) && assigneeFilter != null) {
            return taskRepository.findByProjectAndCompletedAndAssignedUser(project, true, assigneeFilter);
        }
        if ("completada".equalsIgnoreCase(status)) {
            return taskRepository.findByProjectAndCompleted(project, true);
        }
        if ("pendiente".equalsIgnoreCase(status) && assigneeFilter != null) {
            return taskRepository.findByProjectAndCompletedAndAssignedUser(project, false, assigneeFilter);
        }
        if (assigneeFilter != null) {
            return taskRepository.findByProjectAndAssignedUser(project, assigneeFilter);
        }
        return taskRepository.findByProject(project);
    }

    private User resolveAssigneeFromTeam(Task task, Long teamId) {
        if (task.getAssignedUser() == null || task.getAssignedUser().getId() == null) return null;
        return userRepository.findByIdAndTeamId(task.getAssignedUser().getId(), teamId).orElse(null);
    }

    private String saveFileAndGetPublicUrl(MultipartFile file) throws IOException {
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String filename = System.currentTimeMillis() + "_" + original;

        Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);

        Path target = uploadDir.resolve(filename);
        file.transferTo(target.toFile());

        return "/uploads/" + filename;
    }
}
