package es.merida.tfg.gestion_proyectos.controller;

import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    // Mi perfil
    @GetMapping("/account")
    public String account(Model model, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) return "redirect:/login";

        User myUser = userService.findByUsername(username).orElse(null);
        if (myUser == null) return "redirect:/login";

        model.addAttribute("me", myUser);
        return "account";
    }

    // Guardar cambios de perfil (username, email)
    @PostMapping("/account")
    public String updateProfile(@RequestParam String username,
                                @RequestParam String email,
                                HttpSession session,
                                Model model) {

        String current = (String) session.getAttribute("username");
        if (current == null) return "redirect:/login";

        try {
            // Usa el método que espera el nombre actual, no el ID
            User updated = userService.updateProfile(current, username, email);

            // Refrescar la sesión si el nombre cambió
            session.setAttribute("username", updated.getUsername());
            model.addAttribute("me", updated);
            model.addAttribute("successProfile", "Perfil actualizado correctamente.");
        } catch (IllegalArgumentException ex) {
            User myUser = userService.findByUsername(current).orElse(null);
            model.addAttribute("me", myUser);
            model.addAttribute("errorProfile", ex.getMessage());
        }

        return "account";
    }

    // Cambiar contraseña
    @PostMapping("/account/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 Model model) {

        String username = (String) session.getAttribute("username");
        if (username == null) return "redirect:/login";

        User myUser = userService.findByUsername(username).orElse(null);
        if (myUser == null) return "redirect:/login";

        if (newPassword == null || newPassword.length() < 6) {
            model.addAttribute("me", myUser);
            model.addAttribute("errorPassword", "La nueva contraseña debe tener al menos 6 caracteres.");
            return "account";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("me", myUser);
            model.addAttribute("errorPassword", "Las nuevas contraseñas no coinciden.");
            return "account";
        }


        boolean ok = userService.changePassword(username, currentPassword, newPassword);
        if (!ok) {
            model.addAttribute("me", myUser);
            model.addAttribute("errorPassword", "La contraseña actual no es correcta.");
            return "account";
        }

        model.addAttribute("me", myUser);
        model.addAttribute("successPassword", "Contraseña actualizada correctamente.");
        return "account";
    }
}