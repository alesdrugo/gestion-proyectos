package es.merida.tfg.gestion_proyectos.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import es.merida.tfg.gestion_proyectos.model.Project;
import es.merida.tfg.gestion_proyectos.model.Task;
import es.merida.tfg.gestion_proyectos.repository.ProjectRepository;
import es.merida.tfg.gestion_proyectos.repository.TaskRepository;

@Controller
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @GetMapping("/{projectId}")
    public String listTasks(@PathVariable Long projectId, Model model) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + projectId));

        List<Task> tasks = taskRepository.findByProject(project);

        model.addAttribute("project", project);
        model.addAttribute("tasks", tasks);
        model.addAttribute("newTask", new Task());

        return "tasks/list";
    }

    // Formulario para crear nueva tarea
    @GetMapping("/add/{projectId}")
    public String showAddForm(@PathVariable Long projectId, Model model) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + projectId));

        Task task = new Task();
        task.setProject(project);

        model.addAttribute("project", project);
        model.addAttribute("task", task);

        return "tasks/form";
    }


    // Guardar tarea en la base de datos
    /*@PostMapping("/save")
    public String saveTask(@ModelAttribute("task") Task task) {
        taskRepository.save(task);
        return "redirect:/tasks/" + task.getProject().getId();
    }*/



    @PostMapping("/add/{projectId}")
    public String addTask(@PathVariable Long projectId, @ModelAttribute Task task) {
        Project project = projectRepository.findById(projectId).orElseThrow();

            // Obtener las tareas actuales del proyecto
        List<Task> existingTasks = taskRepository.findByProject(project);

        // 🧠 Calcular siguiente número
        int nextNumber = taskRepository.findMaxTaskNumberByProject(project) + 1;
        System.out.println("Alex!!!!!!!!!!!!! "+ taskRepository.findMaxTaskNumberByProject(project) + 1);
        /*int nextNumber = existingTasks.stream()
            .map(Task::getTaskNumber)
            .filter(Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(0) + 1;
        */
        task.setTaskNumber(nextNumber);

        task.setProject(project);
        taskRepository.save(task);

        return "redirect:/tasks/" + projectId;
    }

    @GetMapping("/toggle/{taskId}")
    public String toggleTask(@PathVariable Long taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        task.setCompleted(!task.isCompleted());
        taskRepository.save(task);
        return "redirect:/tasks/" + task.getProject().getId();
    }

    @GetMapping("/delete/{taskId}")
    public String deleteTask(@PathVariable Long taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        Long projectId = task.getProject().getId();
        taskRepository.delete(task);
        return "redirect:/tasks/" + projectId;
    }

    // Mostrar formulario de edición
    @GetMapping("/edit/{taskId}")
    public String showEditForm(@PathVariable Long taskId, Model model) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada: " + taskId));
        model.addAttribute("task", task);
        model.addAttribute("project", task.getProject());
        return "tasks/form";
    }

    // Guardar cambios de la edición
    @PostMapping("/update/{taskId}")
    public String updateTask(@PathVariable Long taskId, @ModelAttribute Task updatedTask) {
        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada: " + taskId));

        existingTask.setTitle(updatedTask.getTitle());
        existingTask.setDescription(updatedTask.getDescription());
        existingTask.setDueDate(updatedTask.getDueDate());
        existingTask.setCompleted(updatedTask.isCompleted());

        taskRepository.save(existingTask);

        return "redirect:/tasks/" + existingTask.getProject().getId();
    }
}
