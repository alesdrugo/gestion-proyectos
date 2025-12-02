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
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";
        model.addAttribute("session", session);
        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));

        model.addAttribute("projects", projectRepository.findAll());
        return "projects/list";
    }

    @GetMapping("/new")
    public String newProjectForm(Model model, HttpSession session) {
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin")))
            return "redirect:/projects";
        model.addAttribute("session", session);
        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));    
        model.addAttribute("project", new Project());
        return "projects/form";
    }

    @PostMapping
    public String saveProject(@ModelAttribute Project project, HttpSession session) {
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin")))
            return "redirect:/projects";        
        projectRepository.save(project);
        return "redirect:/projects";
    }

    @GetMapping("/delete/{id}")
    public String deleteProject(@PathVariable Long id, HttpSession session) {
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin")))
            return "redirect:/projects";
        projectRepository.deleteById(id);
        return "redirect:/projects";
    }
    
    @GetMapping("/edit/{id}")
    public String editProject(@PathVariable Long id, Model model, HttpSession session) {
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin")))
            return "redirect:/projects";

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + id));

        model.addAttribute("project", project);
        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("session", session);        
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));

        
        return "projects/form";
    }

    @PostMapping("/update/{id}")
    public String updateProject(@PathVariable Long id, @ModelAttribute Project updatedProject, HttpSession session) {
        if (session == null || session.getAttribute("username") == null)
            return "redirect:/login";
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin")))
            return "redirect:/projects";

        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + id));

        existing.setName(updatedProject.getName());
        existing.setDescription(updatedProject.getDescription());
        existing.setStatus(updatedProject.getStatus());
        existing.setStartDate(updatedProject.getStartDate());
        existing.setEndDate(updatedProject.getEndDate());

        projectRepository.save(existing);
        return "redirect:/projects";
    }
}