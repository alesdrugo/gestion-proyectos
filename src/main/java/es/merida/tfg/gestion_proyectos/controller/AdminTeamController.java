package es.merida.tfg.gestion_proyectos.controller;

import es.merida.tfg.gestion_proyectos.model.Team;
import es.merida.tfg.gestion_proyectos.repository.ProjectRepository;
import es.merida.tfg.gestion_proyectos.repository.TeamRepository;
import es.merida.tfg.gestion_proyectos.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/teams")
public class AdminTeamController {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public AdminTeamController(TeamRepository teamRepository,
                               UserRepository userRepository,
                               ProjectRepository projectRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    @GetMapping
    public String list(Model model, HttpSession session,
                       @RequestParam(required = false) String error,
                       @RequestParam(required = false) String success) {

        if (!Authz.hasRole(session, "ROLE_ADMIN")) return "redirect:/dashboard";

        List<Team> teams = teamRepository.findAll();

        model.addAttribute("username", Authz.username(session));
        model.addAttribute("isAdmin", true);
        model.addAttribute("isManager", false);

        model.addAttribute("teams", teams);
        model.addAttribute("newTeam", new Team());

        // mapas simples para contadores
        model.addAttribute("usersCount", teams.stream().collect(java.util.stream.Collectors.toMap(
                Team::getId, t -> userRepository.countByTeamId(t.getId())
        )));
        model.addAttribute("projectsCount", teams.stream().collect(java.util.stream.Collectors.toMap(
                Team::getId, t -> projectRepository.countByTeamId(t.getId())
        )));

        model.addAttribute("error", error);
        model.addAttribute("success", success);

        return "admin/teams";
    }

    @PostMapping
    public String create(@ModelAttribute("newTeam") Team team, HttpSession session) {
        if (!Authz.hasRole(session, "ROLE_ADMIN")) return "redirect:/dashboard";

        String name = (team.getName() != null) ? team.getName().trim() : "";
        if (name.isBlank()) return "redirect:/admin/teams?error=empty";

        if (teamRepository.existsByNameIgnoreCase(name)) {
            return "redirect:/admin/teams?error=duplicate";
        }

        Team t = new Team();
        t.setName(name);
        teamRepository.save(t);

        return "redirect:/admin/teams?success=created";
    }

    @PostMapping("/{id}/rename")
    public String rename(@PathVariable Long id,
                         @RequestParam String name,
                         HttpSession session) {

        if (!Authz.hasRole(session, "ROLE_ADMIN")) return "redirect:/dashboard";

        String trimmed = (name != null) ? name.trim() : "";
        if (trimmed.isBlank()) return "redirect:/admin/teams?error=empty";

        // si otro equipo ya tiene ese nombre -> duplicado
        var existing = teamRepository.findByNameIgnoreCase(trimmed);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            return "redirect:/admin/teams?error=duplicate";
        }

        Team team = teamRepository.findById(id).orElseThrow();
        team.setName(trimmed);
        teamRepository.save(team);

        return "redirect:/admin/teams?success=renamed";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        if (!Authz.hasRole(session, "ROLE_ADMIN")) return "redirect:/dashboard";

        long users = userRepository.countByTeamId(id);
        long projects = projectRepository.countByTeamId(id);

        // ✅ Recomendación: NO borrar equipos con datos (evitas líos por FK y pérdidas)
        if (users > 0 || projects > 0) {
            return "redirect:/admin/teams?error=not_empty";
        }

        teamRepository.deleteById(id);
        return "redirect:/admin/teams?success=deleted";
    }
}
