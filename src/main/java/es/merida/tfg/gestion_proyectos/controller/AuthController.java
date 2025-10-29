package es.merida.tfg.gestion_proyectos.controller;

import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // === Página de login ===
    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    // === Procesa el login ===
    @PostMapping("/login")
    public String loginSubmit(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model
    ) {
        System.out.println("🧠 Intentando login para usuario: " + username);

        if (userService.authenticate(username, password)) {
            session.setAttribute("username", username);
            model.addAttribute("username", username);

            boolean isAdmin = userService.findByUsername(username)
                    .map(u -> u.hasRole("ROLE_ADMIN"))
                    .orElse(false);
            System.out.println("Es admin?:"+isAdmin);
            session.setAttribute("isAdmin", isAdmin);
            System.out.println("✅ Login correcto. Rol admin: " + isAdmin);

            return "redirect:/dashboard";
        } else {
            model.addAttribute("error", "Usuario o contraseña incorrectos");
            return "login";
        }
    }

    // === Página de registro ===
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    // === Procesa el registro ===
    @PostMapping("/register")
    public String registerSubmit(@ModelAttribute User user, Model model) {
        userService.register(user);
        model.addAttribute("success", "Usuario registrado correctamente. Inicia sesión.");
        return "login";
    }

    // === Logout ===
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}