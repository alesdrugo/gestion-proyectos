package es.merida.tfg.gestion_proyectos.controller;

import java.util.List;
import java.io.IOException;
import java.nio.file.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import es.merida.tfg.gestion_proyectos.model.Project;
import es.merida.tfg.gestion_proyectos.model.Task;
import es.merida.tfg.gestion_proyectos.model.TaskComment;
import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.repository.ProjectRepository;
import es.merida.tfg.gestion_proyectos.repository.TaskCommentRepository;
import es.merida.tfg.gestion_proyectos.repository.TaskRepository;
import es.merida.tfg.gestion_proyectos.repository.UserRepository;
import es.merida.tfg.gestion_proyectos.service.EmailService;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    @Autowired private TaskRepository taskRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TaskCommentRepository taskCommentRepository;
    @Autowired private EmailService emailService;

    // ============================
    // 🔹 LISTADO DE TAREAS
    // ============================
    @GetMapping("/{projectId}")
    public String listTasks(@PathVariable Long projectId,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) Long assigneeId,
                            Model model, HttpSession session) {
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";

        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + projectId));

        List<Task> tasks;
        var maybeUser = (assigneeId != null) ? userRepository.findById(assigneeId) : java.util.Optional.empty();

        if ("completada".equalsIgnoreCase(status) && maybeUser.isPresent()) {
            tasks = taskRepository.findByProjectAndCompletedAndAssignedUser(project, true, (User)maybeUser.get());
        } else if ("completada".equalsIgnoreCase(status)) {
            tasks = taskRepository.findByProjectAndCompleted(project, true);
        } else if ("pendiente".equalsIgnoreCase(status) && maybeUser.isPresent()) {
            tasks = taskRepository.findByProjectAndCompletedAndAssignedUser(project, false, (User)maybeUser.get());
        } else if (maybeUser.isPresent()) {
            tasks = taskRepository.findByProjectAndAssignedUser(project, (User)maybeUser.get());
        } else {
            tasks = taskRepository.findByProject(project);
        }

        model.addAttribute("project", project);
        model.addAttribute("tasks", tasks);
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterAssigneeId", assigneeId);

        return "tasks/list";
    }

    // ============================
    // 🔹 DETALLE DE TAREA + COMENTARIOS
    // ============================
    @GetMapping("/view/{taskId}")
    public String viewTask(@PathVariable Long taskId, Model model, HttpSession session) {
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada: " + taskId));

        var comments = taskCommentRepository.findByTaskOrderByCreatedAtDesc(task);

        model.addAttribute("task", task);
        model.addAttribute("comments", comments);
        model.addAttribute("newComment", new TaskComment());
        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));

        return "tasks/view";
    }

    // ============================
    // 🔹 AÑADIR COMENTARIO
    // ============================
    @PostMapping("/{taskId}/comment")
    public String addComment(@PathVariable Long taskId,
                             @ModelAttribute("newComment") TaskComment newComment,
                             HttpSession session) {
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";

        String username = (String) session.getAttribute("username");
        User author = userRepository.findByUsername(username).orElse(null);
        if (author == null) return "redirect:/login";

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada: " + taskId));

        if (newComment.getText() == null || newComment.getText().trim().isEmpty())
            return "redirect:/tasks/view/" + taskId;

        newComment.setTask(task);
        newComment.setAuthor(author);
        taskCommentRepository.save(newComment);

        // (Si luego añades notificación por comentario, aquí es buen sitio para llamarla)
        // emailService.sendTaskComment(...);  <-- NO existe en tu EmailService actual

        return "redirect:/tasks/view/" + taskId;
    }

    // ============================
    // 🔹 FORMULARIO NUEVA TAREA
    // ============================
    @GetMapping("/add/{projectId}")
    public String showAddForm(@PathVariable Long projectId, Model model, HttpSession session) {
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";

        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin")))
            return "redirect:/projects";

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + projectId));

        Task task = new Task();
        task.setProject(project);

        model.addAttribute("project", project);
        model.addAttribute("task", task);
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("username", session.getAttribute("username"));

        return "tasks/form";
    }

    // ============================
    // 🔹 GUARDAR NUEVA TAREA
    // ============================
    @PostMapping(value = "/add/{projectId}", consumes = {"multipart/form-data"})
    public String addTask(@PathVariable Long projectId,
                          @ModelAttribute Task task,
                          @RequestParam(value = "file", required = false) MultipartFile file,
                          HttpSession session) throws IOException {

        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";

        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin")))
            return "redirect:/projects";

        Project project = projectRepository.findById(projectId).orElseThrow();
        int nextNumber = taskRepository.findMaxTaskNumberByProject(project) + 1;

        task.setTaskNumber(nextNumber);
        task.setProject(project);

        // Resolver usuario asignado (si viene id)
        User assignee = null;
        if (task.getAssignedUser() != null && task.getAssignedUser().getId() != null) {
            assignee = userRepository.findById(task.getAssignedUser().getId()).orElse(null);
            task.setAssignedUser(assignee);
        }

        if (file != null && !file.isEmpty()) {
            String url = saveFileAndGetPublicUrl(file);
            task.setAttachmentPath(url);
        }

        taskRepository.save(task); // Necesitamos el id para el enlace del mail

        // Enviar email si hay asignado
        if (assignee != null) {
            emailService.sendTaskAssigned(assignee, task);
        }

        return "redirect:/tasks/" + projectId;
    }

    // ============================
    // 🔹 SUBIR / ACTUALIZAR DOCUMENTO DESDE LISTA
    // ============================
    @PostMapping(value = "/{taskId}/upload", consumes = {"multipart/form-data"})
    public String uploadAttachment(@PathVariable Long taskId,
                                   @RequestParam("file") MultipartFile file,
                                   HttpSession session) throws IOException {
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";

        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin")))
            return "redirect:/projects";

        Task task = taskRepository.findById(taskId).orElseThrow();

        if (file != null && !file.isEmpty()) {
            String url = saveFileAndGetPublicUrl(file);
            task.setAttachmentPath(url);
            taskRepository.save(task);
        }

        return "redirect:/tasks/" + task.getProject().getId();
    }

    // ============================
    // 🔹 CAMBIAR ESTADO (toggle)
    // ============================
    @GetMapping("/toggle/{taskId}")
    public String toggleTask(@PathVariable Long taskId, HttpSession session) {
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";

        Task task = taskRepository.findById(taskId).orElseThrow();
        String currentUser = (String) session.getAttribute("username");
        boolean isAdmin = Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        boolean isOwner = task.getAssignedUser() != null &&
                          task.getAssignedUser().getUsername().equals(currentUser);

        if (!isAdmin && !isOwner)
            return "redirect:/tasks/" + task.getProject().getId();

        task.setCompleted(!task.isCompleted());
        taskRepository.save(task);

        return "redirect:/tasks/" + task.getProject().getId();
    }

    // ============================
    // 🔹 EDITAR TAREA
    // ============================
    @GetMapping("/edit/{taskId}")
    public String showEditForm(@PathVariable Long taskId, Model model, HttpSession session) {
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";

        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin")))
            return "redirect:/projects";

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada: " + taskId));

        model.addAttribute("task", task);
        model.addAttribute("project", task.getProject());
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("username", session.getAttribute("username"));

        return "tasks/form";
    }

    // ============================
    // 🔹 GUARDAR CAMBIOS EN TAREA
    // ============================
    @PostMapping(value = "/update/{taskId}", consumes = {"multipart/form-data"})
    public String updateTask(@PathVariable Long taskId,
                             @ModelAttribute Task updatedTask,
                             @RequestParam(value = "file", required = false) MultipartFile file,
                             HttpSession session) throws IOException {
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";

        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin")))
            return "redirect:/projects";

        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada: " + taskId));

        // Guardamos el asignado previo para detectar cambios
        Long previousAssigneeId = existingTask.getAssignedUser() != null
                ? existingTask.getAssignedUser().getId()
                : null;

        existingTask.setTitle(updatedTask.getTitle());
        existingTask.setDescription(updatedTask.getDescription());
        
        if (updatedTask.getDueDate() != null) {
            existingTask.setDueDate(updatedTask.getDueDate());
        }
        
        existingTask.setCompleted(updatedTask.isCompleted());

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

        // Si cambia el asignado, o si antes no había y ahora sí -> enviar mail
        Long newAssigneeId = newAssignee != null ? newAssignee.getId() : null;
        boolean assigneeChanged = (previousAssigneeId == null && newAssigneeId != null)
                || (previousAssigneeId != null && !previousAssigneeId.equals(newAssigneeId));

        if (assigneeChanged && newAssignee != null) {
            emailService.sendTaskAssigned(newAssignee, existingTask);
        }

        return "redirect:/tasks/" + existingTask.getProject().getId();
    }

    // ============================
    // 🔹 ELIMINAR TAREA
    // ============================
    @GetMapping("/delete/{taskId}")
    public String deleteTask(@PathVariable Long taskId, HttpSession session) {
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";

        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin")))
            return "redirect:/projects";

        Task task = taskRepository.findById(taskId).orElseThrow();
        Long projectId = task.getProject().getId();
        taskRepository.delete(task);

        return "redirect:/tasks/" + projectId;
    }

    // ============================
    // 🔹 UTILIDAD: GUARDAR ARCHIVO
    // ============================
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