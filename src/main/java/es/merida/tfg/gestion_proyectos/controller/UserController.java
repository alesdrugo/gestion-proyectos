package es.merida.tfg.gestion_proyectos.controller;

import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.repository.UserRepository;
import es.merida.tfg.gestion_proyectos.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(Model model, HttpSession session) {
        String username = requireLogin(session);
        if (username == null) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/dashboard";

        addCommonAttributes(model, session, username);
        model.addAttribute("users", userRepository.findAll());

        return "users/list";
    }

    @GetMapping("/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model, HttpSession session) {
        String username = requireLogin(session);
        if (username == null) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/dashboard";

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        addCommonAttributes(model, session, username);
        model.addAttribute("user", user);

        return "users/form";
    }

    @PostMapping("/update/{id}")
    public String updateUser(@PathVariable Long id,
                             @ModelAttribute User updatedUser,
                             HttpSession session) {

        String username = requireLogin(session);
        if (username == null) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/dashboard";

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        existing.setUsername(updatedUser.getUsername());
        existing.setEmail(updatedUser.getEmail());
        existing.setEnabled(updatedUser.isEnabled());

        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            existing.setPassword(userService.encodePassword(updatedUser.getPassword()));
        }

        userRepository.save(existing);
        return "redirect:/users";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, HttpSession session) {
        String username = requireLogin(session);
        if (username == null) return "redirect:/login";
        if (!isAdmin(session)) return "redirect:/dashboard";

        userRepository.deleteById(id);
        return "redirect:/users";
    }

    

    private String requireLogin(HttpSession session) {
        return (String) session.getAttribute("username");
    }

    private boolean isAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("isAdmin"));
    }

    private void addCommonAttributes(Model model, HttpSession session, String username) {
        model.addAttribute("username", username);
        model.addAttribute("isAdmin", session.getAttribute("isAdmin"));
    }
}
