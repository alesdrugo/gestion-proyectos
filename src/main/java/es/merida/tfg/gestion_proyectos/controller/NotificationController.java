package es.merida.tfg.gestion_proyectos.controller;

import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.service.NotificationService;
import es.merida.tfg.gestion_proyectos.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping("/notifications")
    public String notifications(Model model, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) return "redirect:/login";

        var optUser = userService.findByUsername(username);
        if (optUser.isEmpty()) return "redirect:/login";

        User user = optUser.get();

        model.addAttribute("username", username);
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));

        model.addAttribute("notifications", notificationService.getLatest(user));
        model.addAttribute("unreadCount", notificationService.countUnread(user));

        return "notifications/list";
    }

    @GetMapping("/notifications/mark-all")
    public String markAllRead(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) return "redirect:/login";

        userService.findByUsername(username)
                .ifPresent(notificationService::markAllAsRead);

        return "redirect:/notifications";
    }


    @GetMapping("/notifications/read/{id}")
    public String markOneRead(@PathVariable Long id, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) return "redirect:/login";

        var optUser = userService.findByUsername(username);
        if (optUser.isEmpty()) return "redirect:/login";

        User user = optUser.get();

        // Aquí idealmente marcas SOLO si la notificación es del usuario
        notificationService.markAsRead(user, id);

        return "redirect:/notifications";
    }
}