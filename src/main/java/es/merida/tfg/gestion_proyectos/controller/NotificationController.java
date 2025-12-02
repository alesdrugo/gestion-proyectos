package es.merida.tfg.gestion_proyectos.controller;

import es.merida.tfg.gestion_proyectos.model.Notification;
import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.service.NotificationService;
import es.merida.tfg.gestion_proyectos.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

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

        User user = userService.findByUsername(username).orElse(null);
        if (user == null) return "redirect:/login";

        model.addAttribute("notifications", notificationService.getLatest(user));
        model.addAttribute("unreadCount",  notificationService.countUnread(user));

        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("isAdmin",  session.getAttribute("isAdmin"));

        return "notifications/list";
    }

    @GetMapping("/notifications/mark-all")
    public String markAllRead(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        if (username == null) return "redirect:/login";

        userService.findByUsername(username).ifPresent(notificationService::markAllAsRead);

        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("isAdmin",  session.getAttribute("isAdmin"));

        return "redirect:/notifications";
    }
}