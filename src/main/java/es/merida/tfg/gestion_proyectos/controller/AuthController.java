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
                        Model model,
                        jakarta.servlet.http.HttpServletRequest request) {

        if (!userService.isEnabled(username)) {
            model.addAttribute("error", "Tu cuenta está deshabilitada. Contacta con el administrador.");
            return "login";
        }

        if (!userService.authenticate(username, password)) {
            model.addAttribute("error", "Usuario o contraseña incorrectos.");
            return "login";
        }

        
        session.invalidate();
        HttpSession newSession = request.getSession(true);

        var userOpt = userService.findByUsername(username);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Usuario no encontrado.");
            return "login";
        }
        var user = userOpt.get();

        newSession.setAttribute("username", username);

        // Guardar roles en sesión
        var roleNames = user.getRoles().stream().map(r -> r.getName()).toList();
        newSession.setAttribute("roles", roleNames);

        // Guardar teamId 
        Long teamId = (user.getTeam() != null) ? user.getTeam().getId() : null;
        newSession.setAttribute("teamId", teamId);

     
        boolean isAdmin = roleNames.stream().anyMatch(r -> r.equalsIgnoreCase("ROLE_ADMIN"));
        newSession.setAttribute("isAdmin", isAdmin);

        // Estado PENDING 
        boolean isPending = (teamId == null) || roleNames.isEmpty();
        if (isPending && !isAdmin) {
            return "redirect:/pending";
        }

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
