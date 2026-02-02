package es.merida.tfg.gestion_proyectos.controller;

import es.merida.tfg.gestion_proyectos.model.Project;
import es.merida.tfg.gestion_proyectos.model.Team;
import es.merida.tfg.gestion_proyectos.repository.ProjectRepository;
import es.merida.tfg.gestion_proyectos.repository.TeamRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;

    public ProjectController(ProjectRepository projectRepository, TeamRepository teamRepository) {
        this.projectRepository = projectRepository;
        this.teamRepository = teamRepository;
    }

    @GetMapping
    public String listProjects(Model model, HttpSession session) {
        String username = Authz.username(session);
        if (username == null) return "redirect:/login";

        Long teamId = Authz.teamId(session);
        if (teamId == null && !Authz.hasRole(session, "ROLE_ADMIN")) return "redirect:/pending";

        addCommonAttributes(model, session, username);

        // Admin no debería operar proyectos; si entra aquí, lo llevamos al panel admin (cuando exista)
        if (Authz.hasRole(session, "ROLE_ADMIN")) {
            return "redirect:/admin/users";
        }

        model.addAttribute("projects", projectRepository.findByTeamId(teamId));
        return "projects/list";
    }

    @GetMapping("/new")
    public String newProjectForm(Model model, HttpSession session) {
        String username = Authz.username(session);
        if (username == null) return "redirect:/login";
        if (!Authz.hasRole(session, "ROLE_MANAGER")) return "redirect:/projects";

        Long teamId = Authz.teamId(session);
        if (teamId == null) return "redirect:/pending";

        addCommonAttributes(model, session, username);
        model.addAttribute("project", new Project());
        return "projects/form";
    }


    @PostMapping
    public String saveProject(@ModelAttribute Project project, HttpSession session) {
        String username = Authz.username(session);
        if (username == null) return "redirect:/login";
        if (!Authz.hasRole(session, "ROLE_MANAGER")) return "redirect:/projects";

        Long teamId = Authz.teamId(session);
        if (teamId == null) return "redirect:/pending";

        // 🔒 No confiar en el team que venga del formulario
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Equipo no encontrado"));

        project.setTeam(team);

        projectRepository.save(project);
        return "redirect:/projects";
    }

    @GetMapping("/edit/{id}")
    public String editProject(@PathVariable Long id, Model model, HttpSession session) {
        String username = Authz.username(session);
        if (username == null) return "redirect:/login";
        if (!Authz.hasRole(session, "ROLE_MANAGER")) return "redirect:/projects";

        Long teamId = Authz.teamId(session);
        if (teamId == null) return "redirect:/pending";

        Project project = projectRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));

        addCommonAttributes(model, session, username);
        model.addAttribute("project", project);
        return "projects/form";
    }

    @PostMapping("/update/{id}")
    public String updateProject(@PathVariable Long id,
                                @ModelAttribute Project updatedProject,
                                HttpSession session) {

        String username = Authz.username(session);
        if (username == null) return "redirect:/login";
        if (!Authz.hasRole(session, "ROLE_MANAGER")) return "redirect:/projects";

        Long teamId = Authz.teamId(session);
        if (teamId == null) return "redirect:/pending";

        Project existing = projectRepository.findByIdAndTeamId(id, teamId)
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
        String username = Authz.username(session);
        if (username == null) return "redirect:/login";
        if (!Authz.hasRole(session, "ROLE_MANAGER")) return "redirect:/projects";

        Long teamId = Authz.teamId(session);
        if (teamId == null) return "redirect:/pending";

        Project project = projectRepository.findByIdAndTeamId(id, teamId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));

        projectRepository.delete(project);
        return "redirect:/projects";
    }

    private void addCommonAttributes(Model model, HttpSession session, String username) {
        model.addAttribute("username", username);
        model.addAttribute("roles", Authz.roles(session));
        //model.addAttribute("isAdmin", Authz.hasRole(session, "ROLE_ADMIN")); 
        model.addAttribute("isManager", Authz.hasRole(session, "ROLE_MANAGER"));
    }
}
