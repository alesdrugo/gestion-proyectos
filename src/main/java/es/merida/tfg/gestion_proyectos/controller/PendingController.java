package es.merida.tfg.gestion_proyectos.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PendingController {

    @GetMapping("/pending")
    public String pending(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) return "redirect:/login";
        return "pending";
    }
}
