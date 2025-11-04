package es.merida.tfg.gestion_proyectos.controller;

import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.repository.UserRepository;
import es.merida.tfg.gestion_proyectos.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    // ============================
    // 🔹 LISTADO DE USUARIOS
    // ============================
    @GetMapping
    public String listUsers(Model model, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin")))
            return "redirect:/dashboard";

        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("isAdmin", true);
        return "users/list";
    }

    // ============================
    // 🔹 FORMULARIO DE EDICIÓN
    // ============================
    @GetMapping("/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin")))
            return "redirect:/dashboard";

        var user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        model.addAttribute("user", user);
        model.addAttribute("username", session.getAttribute("username"));
        return "users/form";
    }

    // ============================
    // 🔹 GUARDAR CAMBIOS
    // ============================
    @PostMapping("/update/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute User updatedUser, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin")))
            return "redirect:/dashboard";

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        existing.setUsername(updatedUser.getUsername());
        existing.setEmail(updatedUser.getEmail());
        existing.setEnabled(updatedUser.isEnabled());
        // 🔸 Si quieres permitir editar la contraseña:
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            existing.setPassword(userService.encodePassword(updatedUser.getPassword()));
        }

        userRepository.save(existing);
        return "redirect:/users";
    }

    // ============================
    // 🔹 ELIMINAR USUARIO
    // ============================
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin")))
            return "redirect:/dashboard";

        userRepository.deleteById(id);
        return "redirect:/users";
    }
}