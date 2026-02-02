package es.merida.tfg.gestion_proyectos.controller;

import es.merida.tfg.gestion_proyectos.model.Role;
import es.merida.tfg.gestion_proyectos.model.Team;
import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.repository.RoleRepository;
import es.merida.tfg.gestion_proyectos.repository.TeamRepository;
import es.merida.tfg.gestion_proyectos.repository.UserRepository;
import es.merida.tfg.gestion_proyectos.service.EmailService;
import es.merida.tfg.gestion_proyectos.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.util.Set;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;


    public AdminController(UserRepository userRepository,
                       TeamRepository teamRepository,
                       RoleRepository roleRepository,
                       EmailService emailService,
                       NotificationService notificationService) {
    this.userRepository = userRepository;
    this.teamRepository = teamRepository;
    this.roleRepository = roleRepository;
    this.emailService = emailService;
    this.notificationService = notificationService;
}


    @GetMapping("/users")
    public String users(@RequestParam(required = false) String q,
                        @RequestParam(required = false) Boolean enabled,
                        @RequestParam(required = false) Long teamId,
                        @RequestParam(required = false) String roleName,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        Model model,
                        HttpSession session) {

        if (!Authz.hasRole(session, "ROLE_ADMIN")) return "redirect:/dashboard";

        Page<User> users = userRepository.searchNonAdmins(q, enabled, teamId, roleName, PageRequest.of(page, size));


        model.addAttribute("username", Authz.username(session));
        model.addAttribute("isAdmin", true);
        model.addAttribute("usersPage", users);

        model.addAttribute("teams", teamRepository.findAll());

        // filtros actuales
        model.addAttribute("q", q);
        model.addAttribute("enabled", enabled);
        model.addAttribute("teamId", teamId);
        model.addAttribute("roleName", roleName);

        return "admin/users";
    }

   @PostMapping("/users/{id}/assign")
    public String assign(@PathVariable Long id,
                     @RequestParam(required = false) Long teamId,
                     @RequestParam String roleName,
                     @RequestParam(required = false) Boolean enabled,
                     HttpSession session) {

    if (!Authz.hasRole(session, "ROLE_ADMIN")) return "redirect:/dashboard";

    User user = userRepository.findById(id).orElseThrow();

    // ===== Estado previo (para evitar enviar emails/notificaciones si no cambió nada) =====
    Long prevTeamId = (user.getTeam() != null) ? user.getTeam().getId() : null;
    String prevRole = user.getRoles().stream().findFirst().map(Role::getName).orElse(null);
    boolean prevEnabled = user.isEnabled();

    // enabled opcional
    if (enabled != null) user.setEnabled(enabled);

    // equipo (null -> limbo)
    Team team = null;
    if (teamId != null) {
        team = teamRepository.findById(teamId).orElseThrow();
    }

    // === VALIDACIÓN: 1 manager por equipo ===
    if (teamId != null && "ROLE_MANAGER".equalsIgnoreCase(roleName)) {

        // ¿este usuario YA era manager?
        boolean userAlreadyManager =
                user.getRoles().stream().anyMatch(r -> "ROLE_MANAGER".equalsIgnoreCase(r.getName()));

        // ¿hay managers en ESE equipo?
        long managersInTeam = userRepository.countByTeamIdAndRoles_Name(teamId, "ROLE_MANAGER");

        // Si ya existe uno y no es el propio usuario -> bloquear
        if (managersInTeam >= 1 && !userAlreadyManager) {
            return "redirect:/admin/users?error=team_has_manager";
        }
    }

    user.setTeam(team);

    // rol
    Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + roleName));

    // reemplazar roles (colección mutable, ok con Hibernate)
    user.getRoles().clear();
    user.getRoles().add(role);

    userRepository.save(user);

    // ===== Notificar solo si hubo cambios reales =====
    boolean teamChanged = (prevTeamId == null && teamId != null)
            || (prevTeamId != null && !prevTeamId.equals(teamId));

    boolean roleChanged = (prevRole == null && roleName != null)
            || (prevRole != null && !prevRole.equalsIgnoreCase(roleName));

    boolean enabledChanged = (enabled != null) && (prevEnabled != enabled);

    if (teamChanged || roleChanged || enabledChanged) {

        // Email (si el user tiene email, el método ya lo comprueba)
        emailService.sendUserAssignedToTeam(user, team, roleName);

        // Notificación (solo si queda habilitado y con equipo)
        if (user.isEnabled() && team != null) {
            String roleLabel = "ROLE_MANAGER".equalsIgnoreCase(roleName) ? "Manager" : "Usuario";
            notificationService.create(
                    user,
                    "Tu cuenta ha sido asignada al equipo \"" + team.getName() + "\" como " + roleLabel + ".",
                    "/dashboard"
            );
        }
    }

    return "redirect:/admin/users";
}


}
