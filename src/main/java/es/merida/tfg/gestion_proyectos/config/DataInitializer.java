package es.merida.tfg.gestion_proyectos.config;

import es.merida.tfg.gestion_proyectos.model.Role;
import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.repository.RoleRepository;
import es.merida.tfg.gestion_proyectos.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   BCryptPasswordEncoder passwordEncoder) {
        return args -> {
            // Crear rol ADMIN si no existe
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setName("ROLE_ADMIN");
                        return roleRepository.save(newRole);
                    });
                    
            // Crear usuario admin si no existe
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin")); // Contraseña por defecto
                admin.setEnabled(true);
                admin.setRoles(Set.of(adminRole));
                
                userRepository.save(admin);
                System.out.println("✅ Usuario ADMIN creado (user: admin / pass: admin)");
            } else {
                System.out.println("ℹ️ Usuario ADMIN ya existe, no se ha modificado.");
            }

            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setName("ROLE_USER");
                        return roleRepository.save(newRole);
                    });

            // Crear usuario admin si no existe
            if (userRepository.findByUsername("user").isEmpty()) {
                User admin = new User();
                admin.setUsername("user");
                admin.setPassword(passwordEncoder.encode("user")); // Contraseña por defecto
                admin.setEnabled(true);
                admin.setRoles(Set.of(userRole));

                userRepository.save(admin);
                System.out.println("✅ Usuario USER creado (user: user / pass: user)");
            } else {
                System.out.println("ℹ️ Usuario USER ya existe, no se ha modificado.");
            }


        };
    }
}
