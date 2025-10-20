package es.merida.tfg.gestion_proyectos.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute
    public void addGlobalAttributes(Model model, HttpSession session) {
        if (session != null) {
            model.addAttribute("username", session.getAttribute("username"));
            model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
        }
    }
}