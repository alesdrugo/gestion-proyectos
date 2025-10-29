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
import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.repository.ProjectRepository;
import es.merida.tfg.gestion_proyectos.repository.TaskRepository;
import es.merida.tfg.gestion_proyectos.repository.UserRepository;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    @Autowired private TaskRepository taskRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private UserRepository userRepository;

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

    if (status != null && status.equalsIgnoreCase("completada") && maybeUser.isPresent()) {
        tasks = taskRepository.findByProjectAndCompletedAndAssignedUser(project, true, (User)maybeUser.get());
    } else if (status != null && status.equalsIgnoreCase("completada")) {
        tasks = taskRepository.findByProjectAndCompleted(project, true);
    } else if (status != null && status.equalsIgnoreCase("pendiente") && maybeUser.isPresent()) {
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

        if (task.getAssignedUser() != null && task.getAssignedUser().getId() != null) {
            User user = userRepository.findById(task.getAssignedUser().getId()).orElse(null);
            task.setAssignedUser(user);
        }

        // 📎 Guardar fichero si viene
        if (file != null && !file.isEmpty()) {
            String url = saveFileAndGetPublicUrl(file);
            task.setAttachmentPath(url);
            System.out.println("📎 Archivo subido para nueva tarea: " + url);
        }

        taskRepository.save(task);
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
            System.out.println("📎 Archivo subido para tarea " + taskId + ": " + url);
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

        // 🔒 Solo admin o usuario asignado puede cambiar estado
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

        existingTask.setTitle(updatedTask.getTitle());
        existingTask.setDescription(updatedTask.getDescription());
        existingTask.setDueDate(updatedTask.getDueDate());
        existingTask.setCompleted(updatedTask.isCompleted());

        if (updatedTask.getAssignedUser() != null && updatedTask.getAssignedUser().getId() != null) {
            User user = userRepository.findById(updatedTask.getAssignedUser().getId()).orElse(null);
            existingTask.setAssignedUser(user);
        }

        if (file != null && !file.isEmpty()) {
            String url = saveFileAndGetPublicUrl(file);
            existingTask.setAttachmentPath(url);
            System.out.println("📎 Archivo reemplazado para tarea " + taskId + ": " + url);
        }

        taskRepository.save(existingTask);
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