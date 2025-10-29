package es.merida.tfg.gestion_proyectos.controller;

import es.merida.tfg.gestion_proyectos.model.Comment;
import es.merida.tfg.gestion_proyectos.model.Task;
import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.repository.CommentRepository;
import es.merida.tfg.gestion_proyectos.repository.TaskRepository;
import es.merida.tfg.gestion_proyectos.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/task")
public class TaskDetailController {

    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public TaskDetailController(TaskRepository taskRepository, CommentRepository commentRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{taskId}")
    public String view(@PathVariable Long taskId, Model model, HttpSession session) {
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";

        Task task = taskRepository.findById(taskId).orElseThrow();
        model.addAttribute("task", task);
        model.addAttribute("comments", commentRepository.findByTaskOrderByCreatedAtDesc(task));
        model.addAttribute("newComment", new Comment());
        return "tasks/view";
    }

    @PostMapping("/{taskId}/comment")
    public String addComment(@PathVariable Long taskId,
                             @ModelAttribute("newComment") Comment newComment,
                             HttpSession session) {
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";

        Task task = taskRepository.findById(taskId).orElseThrow();
        String username = (String) session.getAttribute("username");
        User author = userRepository.findByUsername(username).orElseThrow();

        newComment.setTask(task);
        newComment.setAuthor(author);
        commentRepository.save(newComment);

        return "redirect:/task/" + taskId;
    }
}