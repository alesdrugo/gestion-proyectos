package es.merida.tfg.gestion_proyectos.config;

import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.service.NotificationService;
import es.merida.tfg.gestion_proyectos.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

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

        if (session == null) {
            return;
        }

        String username = (String) session.getAttribute("username");
        if (username == null) {
            return;
        }

        User user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            return;
        }

        model.addAttribute("unreadCount", notificationService.countUnread(user));
        model.addAttribute("notifications", notificationService.latest(user));
    }
}
