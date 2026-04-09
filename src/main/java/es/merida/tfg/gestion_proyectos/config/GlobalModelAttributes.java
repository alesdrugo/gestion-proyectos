package es.merida.tfg.gestion_proyectos.config;

import es.merida.tfg.gestion_proyectos.controller.Authz;
import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.service.NotificationService;
import es.merida.tfg.gestion_proyectos.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalModelAttributes {

    private final NotificationService notificationService;
    private final UserService userService;

    public GlobalModelAttributes(NotificationService notificationService,
                                 UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model, HttpSession session) {

        model.addAttribute("username", null);
        model.addAttribute("roles", List.of());
        model.addAttribute("teamId", null);

        model.addAttribute("isAdmin", false);
        model.addAttribute("isManager", false);

        model.addAttribute("unreadCount", 0L);
        model.addAttribute("notifications", List.of());

        if (session == null) return;

        String username = (String) session.getAttribute("username");
        if (username == null) return;

        // Roles y teamId desde sesión 
        List<String> roles = Authz.roles(session);
        Long teamId = Authz.teamId(session);

        boolean isAdmin = Authz.hasRole(session, "ROLE_ADMIN");
        boolean isManager = Authz.hasRole(session, "ROLE_MANAGER");

        model.addAttribute("username", username);
        model.addAttribute("roles", roles);
        model.addAttribute("teamId", teamId);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isManager", isManager);

        // Notificaciones
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) return;

        model.addAttribute("unreadCount", notificationService.countUnread(user));
        model.addAttribute("notifications", notificationService.latest(user));
    }
}
