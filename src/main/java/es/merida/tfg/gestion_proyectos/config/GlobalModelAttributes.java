package es.merida.tfg.gestion_proyectos.config;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import es.merida.tfg.gestion_proyectos.service.NotificationService;
import es.merida.tfg.gestion_proyectos.service.UserService;

@ControllerAdvice
public class GlobalModelAttributes {

   @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @ModelAttribute
    public void addGlobalAttributes(Model model, HttpSession session) {
        if (session != null && session.getAttribute("username") != null) {
            String username = session.getAttribute("username").toString();
            userService.findByUsername(username).ifPresent(user -> {
                model.addAttribute("unreadCount", notificationService.countUnread(user));
                model.addAttribute("notifications", notificationService.latest(user));
            });
        }
    }
}