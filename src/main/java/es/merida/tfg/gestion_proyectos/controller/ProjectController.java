
package es.merida.tfg.gestion_proyectos.controller;

import es.merida.tfg.gestion_proyectos.model.Project;
import es.merida.tfg.gestion_proyectos.repository.ProjectRepository;
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

    // ✅ Mostrar todos los proyectos
    @GetMapping
    public String listProjects(Model model) {
        model.addAttribute("projects", projectRepository.findAll());
        return "projects/list"; // plantilla Thymeleaf
    }

    // ✅ Formulario para nuevo proyecto
    @GetMapping("/new")
    public String newProjectForm(Model model) {
        model.addAttribute("project", new Project());
        return "projects/form";
    }

    // ✅ Guardar proyecto
    @PostMapping
    public String saveProject(@ModelAttribute Project project) {
        projectRepository.save(project);
        return "redirect:/projects";
    }

    // ✅ Eliminar proyecto
    @GetMapping("/delete/{id}")
    public String deleteProject(@PathVariable Long id) {
        projectRepository.deleteById(id);
        return "redirect:/projects";
    }
}
