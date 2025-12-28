package es.merida.tfg.gestion_proyectos.controller;

import es.merida.tfg.gestion_proyectos.model.Project;
import es.merida.tfg.gestion_proyectos.repository.ProjectRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;

    public ProjectController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @GetMapping
    public String listProjects(Model model, HttpSession session) {
        String username = requireLogin(session);
        if (username == null) return "redirect:/login";

        addCommonAttributes(model, session, username);
        model.addAttribute("projects", projectRepository.findAll());

        return "projects/list";
    }

    @GetMapping("/new")
    public String newProjectForm(Model model, HttpSession session) {
        String username = requireLogin(session);
        if (username == null) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/projects";

        addCommonAttributes(model, session, username);
        model.addAttribute("project", new Project());

        return "projects/form";
    }

    @PostMapping
    public String saveProject(@ModelAttribute Project project, HttpSession session) {
        String username = requireLogin(session);
        if (username == null) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/projects";

        projectRepository.save(project);
        return "redirect:/projects";
    }

    @GetMapping("/edit/{id}")
    public String editProject(@PathVariable Long id, Model model, HttpSession session) {
        String username = requireLogin(session);
        if (username == null) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/projects";

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));

        addCommonAttributes(model, session, username);
        model.addAttribute("project", project);

        return "projects/form";
    }

    @PostMapping("/update/{id}")
    public String updateProject(@PathVariable Long id,
                                @ModelAttribute Project updatedProject,
                                HttpSession session) {

        String username = requireLogin(session);
        if (username == null) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/projects";

        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));

        existing.setName(updatedProject.getName());
        existing.setDescription(updatedProject.getDescription());
        existing.setStatus(updatedProject.getStatus());
        existing.setStartDate(updatedProject.getStartDate());
        existing.setEndDate(updatedProject.getEndDate());

        projectRepository.save(existing);
        return "redirect:/projects";
    }

    @GetMapping("/delete/{id}")
    public String deleteProject(@PathVariable Long id, HttpSession session) {
        String username = requireLogin(session);
        if (username == null) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/projects";

        projectRepository.deleteById(id);
        return "redirect:/projects";
    }

    // ---- helpers ----

    private String requireLogin(HttpSession session) {
        return (String) session.getAttribute("username");
    }

    private boolean isAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("isAdmin"));
    }

    private void addCommonAttributes(Model model, HttpSession session, String username) {
        model.addAttribute("username", username);
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
    }
}
