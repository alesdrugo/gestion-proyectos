package es.merida.tfg.gestion_proyectos.controller;

import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpSession session,
                          Model model) {

        if (!userService.isEnabled(username)) {
            model.addAttribute("error", "Tu cuenta está deshabilitada. Contacta con el administrador.");
            return "login";
        }

        if (!userService.authenticate(username, password)) {
            model.addAttribute("error", "Usuario o contraseña incorrectos.");
            return "login";
        }

        session.setAttribute("username", username);

        boolean isAdmin = userService.findByUsername(username)
                .map(u -> u.hasRole("ROLE_ADMIN"))
                .orElse(false);

        session.setAttribute("isAdmin", isAdmin);

        return "redirect:/dashboard";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@ModelAttribute User user, Model model) {

        if (userService.findByUsername(user.getUsername()).isPresent()) {
            model.addAttribute("error", "El nombre de usuario ya existe.");
            model.addAttribute("user", user);
            return "register";
        }

        if (userService.findByEmail(user.getEmail()).isPresent()) {
            model.addAttribute("error", "Ya existe un usuario con ese email.");
            model.addAttribute("user", user);
            return "register";
        }

        userService.register(user);
        model.addAttribute("success", "Usuario registrado correctamente. Inicia sesión.");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
