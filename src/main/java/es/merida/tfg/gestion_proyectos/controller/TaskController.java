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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    @GetMapping("/{projectId}")
    public String listTasks(@PathVariable Long projectId,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) Long assigneeId,
                            Model model,
                            HttpSession session) {

        String username = requireLogin(session);
        if (username == null) return "redirect:/login";

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));

        List<Task> tasks = resolveTasks(project, status, assigneeId);

        model.addAttribute("project", project);
        model.addAttribute("tasks", tasks);
        model.addAttribute("users", userRepository.findAll());

        model.addAttribute("username", username);
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));

        model.addAttribute("filterStatus", status);
        model.addAttribute("filterAssigneeId", assigneeId);

        return "tasks/list";
    }

    @GetMapping("/view/{taskId}")
    public String viewTask(@PathVariable Long taskId, Model model, HttpSession session) {

        String username = requireLogin(session);
        if (username == null) return "redirect:/login";

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));

        var comments = taskCommentRepository.findByTaskOrderByCreatedAtDesc(task);

        model.addAttribute("task", task);
        model.addAttribute("comments", comments);
        model.addAttribute("newComment", new TaskComment());

        model.addAttribute("username", username);
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));

        return "tasks/view";
    }

    @PostMapping("/{taskId}/comment")
    public String addComment(@PathVariable Long taskId,
                             @ModelAttribute("newComment") TaskComment newComment,
                             HttpSession session) {

        String username = requireLogin(session);
        if (username == null) return "redirect:/login";

        User author = userRepository.findByUsername(username).orElse(null);
        if (author == null) return "redirect:/login";

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));

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

    @GetMapping("/add/{projectId}")
    public String showAddForm(@PathVariable Long projectId, Model model, HttpSession session) {

        String username = requireLogin(session);
        if (username == null) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/projects";

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));

        Task task = new Task();
        task.setProject(project);

        model.addAttribute("project", project);
        model.addAttribute("task", task);
        model.addAttribute("users", userRepository.findAll());

        model.addAttribute("username", username);
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));

        return "tasks/form";
    }

    @PostMapping(value = "/add/{projectId}", consumes = {"multipart/form-data"})
    public String addTask(@PathVariable Long projectId,
                          @ModelAttribute Task task,
                          @RequestParam(value = "file", required = false) MultipartFile file,
                          HttpSession session) throws IOException {

        String username = requireLogin(session);
        if (username == null) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/projects";

        Project project = projectRepository.findById(projectId).orElseThrow();
        int nextNumber = taskRepository.findMaxTaskNumberByProject(project) + 1;

        task.setTaskNumber(nextNumber);
        task.setProject(project);

        User assignee = resolveAssignee(task);
        task.setAssignedUser(assignee);

        if (file != null && !file.isEmpty()) {
            String url = saveFileAndGetPublicUrl(file);
            task.setAttachmentPath(url);
        }

        taskRepository.save(task);

        if (assignee != null) {
            emailService.sendTaskAssigned(assignee, task);
            notificationService.create(
                    assignee,
                    "Se te ha asignado la tarea: " + task.getTitle(),
                    "/tasks/view/" + task.getId()
            );
        }

        return "redirect:/tasks/" + projectId;
    }

    @PostMapping(value = "/{taskId}/upload", consumes = {"multipart/form-data"})
    public String uploadAttachment(@PathVariable Long taskId,
                                   @RequestParam("file") MultipartFile file,
                                   HttpSession session) throws IOException {

        String username = requireLogin(session);
        if (username == null) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/projects";

        Task task = taskRepository.findById(taskId).orElseThrow();

        if (file != null && !file.isEmpty()) {
            String url = saveFileAndGetPublicUrl(file);
            task.setAttachmentPath(url);
            taskRepository.save(task);
        }

        return "redirect:/tasks/" + task.getProject().getId();
    }

    @GetMapping("/toggle/{taskId}")
    public String toggleTask(@PathVariable Long taskId, HttpSession session) {

        String username = requireLogin(session);
        if (username == null) return "redirect:/login";

        Task task = taskRepository.findById(taskId).orElseThrow();

        boolean admin = isAdmin(session);
        boolean owner = task.getAssignedUser() != null
                && task.getAssignedUser().getUsername().equals(username);

        if (!admin && !owner) {
            return "redirect:/tasks/" + task.getProject().getId();
        }

        task.setCompleted(!task.isCompleted());
        taskRepository.save(task);

        return "redirect:/tasks/" + task.getProject().getId();
    }

    @GetMapping("/edit/{taskId}")
    public String showEditForm(@PathVariable Long taskId, Model model, HttpSession session) {

        String username = requireLogin(session);
        if (username == null) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/projects";

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));

        model.addAttribute("task", task);
        model.addAttribute("project", task.getProject());
        model.addAttribute("users", userRepository.findAll());

        model.addAttribute("username", username);
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));

        return "tasks/form";
    }

    @PostMapping(value = "/update/{taskId}", consumes = {"multipart/form-data"})
    public String updateTask(@PathVariable Long taskId,
                             @ModelAttribute Task updatedTask,
                             @RequestParam(value = "file", required = false) MultipartFile file,
                             HttpSession session) throws IOException {

        String username = requireLogin(session);
        if (username == null) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/projects";

        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada"));

        Long previousAssigneeId = existingTask.getAssignedUser() != null
                ? existingTask.getAssignedUser().getId()
                : null;

        existingTask.setTitle(updatedTask.getTitle());
        existingTask.setDescription(updatedTask.getDescription());
        existingTask.setCompleted(updatedTask.isCompleted());

        if (updatedTask.getDueDate() != null) {
            existingTask.setDueDate(updatedTask.getDueDate());
        }

        User newAssignee = null;
        if (updatedTask.getAssignedUser() != null && updatedTask.getAssignedUser().getId() != null) {
            newAssignee = userRepository.findById(updatedTask.getAssignedUser().getId()).orElse(null);
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

        String username = requireLogin(session);
        if (username == null) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/projects";

        Task task = taskRepository.findById(taskId).orElseThrow();
        Long projectId = task.getProject().getId();
        taskRepository.delete(task);

        return "redirect:/tasks/" + projectId;
    }

    // ---- helpers ----

    private String requireLogin(HttpSession session) {
        return (String) session.getAttribute("username");
    }

    private boolean isAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("isAdmin"));
    }

    private List<Task> resolveTasks(Project project, String status, Long assigneeId) {
        Optional<User> maybeUser = (assigneeId != null) ? userRepository.findById(assigneeId) : Optional.empty();

        if ("completada".equalsIgnoreCase(status) && maybeUser.isPresent()) {
            return taskRepository.findByProjectAndCompletedAndAssignedUser(project, true, maybeUser.get());
        }
        if ("completada".equalsIgnoreCase(status)) {
            return taskRepository.findByProjectAndCompleted(project, true);
        }
        if ("pendiente".equalsIgnoreCase(status) && maybeUser.isPresent()) {
            return taskRepository.findByProjectAndCompletedAndAssignedUser(project, false, maybeUser.get());
        }
        if (maybeUser.isPresent()) {
            return taskRepository.findByProjectAndAssignedUser(project, maybeUser.get());
        }
        return taskRepository.findByProject(project);
    }

    private User resolveAssignee(Task task) {
        if (task.getAssignedUser() == null || task.getAssignedUser().getId() == null) {
            return null;
        }
        return userRepository.findById(task.getAssignedUser().getId()).orElse(null);
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
