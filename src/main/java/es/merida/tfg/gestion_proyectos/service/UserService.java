package es.merida.tfg.gestion_proyectos.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean authenticate(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .filter(User::isEnabled)
                .map(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .orElse(false);
    }

    public boolean isEnabled(String username) {
        return userRepository.findByUsername(username)
                .map(User::isEnabled)
                .orElse(false);
    }

    public List<User> listEnabledUsers() {
        return userRepository.findByEnabledTrueOrderByUsernameAsc();
    }


    public void register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {   // 👈 añadido
        return userRepository.findByEmail(email);
    }

    public String encodePassword(String plain) {
        return passwordEncoder.encode(plain);
    }

    @Transactional
    public User updateProfile(String currentUsername, String newUsername, String email) {
        User me = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Validaciones de unicidad
        if (!newUsername.equalsIgnoreCase(me.getUsername())
                && userRepository.findByUsername(newUsername).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
        }
        if (!email.equalsIgnoreCase(me.getEmail())
                && userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ese email ya está registrado.");
        }

        me.setUsername(newUsername);
        me.setEmail(email);
        return userRepository.save(me);
    }

    @Transactional
    public boolean changePassword(String username, String currentRaw, String newRaw) {
        User me = userRepository.findByUsername(username).orElse(null);
        if (me == null) return false;
        if (!passwordEncoder.matches(currentRaw, me.getPassword())) return false;

        me.setPassword(passwordEncoder.encode(newRaw));
        userRepository.save(me);
        return true;
    }

    // 

}