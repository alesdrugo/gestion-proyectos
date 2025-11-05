package es.merida.tfg.gestion_proyectos.config;

import es.merida.tfg.gestion_proyectos.model.*;
import es.merida.tfg.gestion_proyectos.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   ProjectRepository projectRepository,
                                   TaskRepository taskRepository,
                                   TaskCommentRepository taskCommentRepository,
                                   BCryptPasswordEncoder passwordEncoder) {
        return args -> {
            // ==== ROLES =======================================================
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setName("ROLE_ADMIN");
                        return roleRepository.save(r);
                    });

            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setName("ROLE_USER");
                        return roleRepository.save(r);
                    });

            // ==== USUARIOS ====================================================
            // Admin
            userRepository.findByUsername("admin").orElseGet(() -> {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin"));
                admin.setEnabled(true);
                admin.setRoles(Set.of(adminRole, userRole));
                // usa un email que no choque con tu usuario real
                String adminEmail = "ale.castillo.gonzalez@gmail.com";
                if (userRepository.findByEmail(adminEmail).isPresent()) {
                    adminEmail = "admin2@example.com";
                }
                admin.setEmail(adminEmail);
                userRepository.save(admin);
                System.out.println("✅ Usuario ADMIN creado (admin/admin)");
                return admin;
            });

            // Demo
            // Evita colisión por email único: si ya existe, cambia
            Optional<User> maybeDemo = userRepository.findByUsername("demo");
            User demo = maybeDemo.orElseGet(() -> {
                String demoEmail = "taqer_@hotmail.com.com";
                if (userRepository.findByEmail(demoEmail).isPresent()) {
                    demoEmail = "demo2@example.com";
                }
                User u = new User();
                u.setUsername("demo");
                u.setPassword(passwordEncoder.encode("demo"));
                u.setEnabled(true);
                u.setRoles(Set.of(userRole));
                u.setEmail(demoEmail);
                userRepository.save(u);
                System.out.println("✅ Usuario DEMO creado (demo/demo)");
                return u;
            });

            System.out.println("ℹ️ Usuarios listos.");

            // ==== PROYECTOS (idempotente: solo si no hay) =====================
            if (projectRepository.count() == 0) {
                Project p1 = new Project();
                p1.setName("TFG – Gestión de Proyectos");
                p1.setDescription("Spring Boot + Thymeleaf + Emails.");
                p1.setStatus("En curso");
                p1.setStartDate(LocalDate.now().minusDays(7));
                p1.setEndDate(LocalDate.now().plusMonths(1));
                p1 = projectRepository.save(p1);

                Project p2 = new Project();
                p2.setName("Aquascaping 100×50×40");
                p2.setDescription("Planificación de montaje, hardscape y emergido.");
                p2.setStatus("Planificado");
                p2.setStartDate(LocalDate.now());
                p2.setEndDate(LocalDate.now().plusMonths(2));
                p2 = projectRepository.save(p2);

                // ==== TAREAS P1 ====
                Task t1 = createTask(taskRepository, p1,
                        "Configurar login y perfil",
                        "Endpoints /login, /account y cambio de contraseña.",
                        LocalDate.now().plusDays(3),
                        false, demo, null);

                createTask(taskRepository, p1,
                        "Navbar como fragmento y dashboard",
                        "Unificar navbar y métricas del dashboard.",
                        LocalDate.now().plusDays(5),
                        true, demo, null);

                Task t3 = createTask(taskRepository, p1,
                        "Emails de asignación",
                        "Enviar correo cuando se asigna una tarea.",
                        LocalDate.now().plusDays(7),
                        false, demo, null);

                // Comentario ejemplo
                TaskComment c = new TaskComment();
                c.setTask(t3);
                c.setAuthor(demo);
                c.setText("Dejo preparada la plantilla básica de correo.");
                taskCommentRepository.save(c);

                // ==== TAREAS P2 ====
                createTask(taskRepository, p2,
                        "Elegir iluminación",
                        "Comparar Twinstar vs Chihiros; potencia y montaje.",
                        LocalDate.now().plusDays(10),
                        false, demo, null);

                createTask(taskRepository, p2,
                        "Lista de plantas",
                        "Emergidas/sumergidas y proveedores.",
                        LocalDate.now().plusDays(14),
                        false, demo, null);

                System.out.println("✅ Proyectos y tareas de ejemplo creados.");
            } else {
                System.out.println("ℹ️ Ya hay proyectos en la BD, no se crean datos de ejemplo.");
            }
        };
    }

    private Task createTask(TaskRepository taskRepository,
                            Project project,
                            String title,
                            String description,
                            LocalDate dueDate,
                            boolean completed,
                            User assignee,
                            String attachmentUrl) {
        int next = taskRepository.findMaxTaskNumberByProject(project) + 1;

        Task t = new Task();
        t.setProject(project);
        t.setTaskNumber(next);
        t.setTitle(title);
        t.setDescription(description);
        t.setDueDate(dueDate);
        t.setCompleted(completed);
        t.setAssignedUser(assignee);
        t.setAttachmentPath(attachmentUrl);

        return taskRepository.save(t);
    }
}