package es.merida.tfg.gestion_proyectos.config;

import es.merida.tfg.gestion_proyectos.model.Project;
import es.merida.tfg.gestion_proyectos.model.Role;
import es.merida.tfg.gestion_proyectos.model.Task;
import es.merida.tfg.gestion_proyectos.model.TaskComment;
import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.repository.ProjectRepository;
import es.merida.tfg.gestion_proyectos.repository.RoleRepository;
import es.merida.tfg.gestion_proyectos.repository.TaskCommentRepository;
import es.merida.tfg.gestion_proyectos.repository.TaskRepository;
import es.merida.tfg.gestion_proyectos.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@Configuration
@Profile("dev")
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   ProjectRepository projectRepository,
                                   TaskRepository taskRepository,
                                   TaskCommentRepository taskCommentRepository,
                                   BCryptPasswordEncoder passwordEncoder) {
        return args -> {

            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(newRole("ROLE_ADMIN")));

            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> roleRepository.save(newRole("ROLE_USER")));

            User admin = userRepository.findByUsername("admin").orElseGet(() -> {
                User u = new User();
                u.setUsername("admin");
                u.setPassword(passwordEncoder.encode("admin"));
                u.setEnabled(true);
                u.setRoles(Set.of(adminRole, userRole));
                u.setEmail(uniqueEmail(userRepository, "admin@example.com"));
                return userRepository.save(u);
            });

            User demo = userRepository.findByUsername("demo").orElseGet(() -> {
                User u = new User();
                u.setUsername("demo");
                u.setPassword(passwordEncoder.encode("demo"));
                u.setEnabled(true);
                u.setRoles(Set.of(userRole));
                u.setEmail(uniqueEmail(userRepository, "demo@example.com"));
                return userRepository.save(u);
            });

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

                createTask(taskRepository, p1,
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

                TaskComment c = new TaskComment();
                c.setTask(t3);
                c.setAuthor(demo);
                c.setText("Dejo preparada la plantilla básica de correo.");
                taskCommentRepository.save(c);

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
            }
        };
    }

    private Role newRole(String name) {
        Role r = new Role();
        r.setName(name);
        return r;
    }

    private String uniqueEmail(UserRepository userRepository, String baseEmail) {
        if (userRepository.findByEmail(baseEmail).isEmpty()) return baseEmail;

        String[] parts = baseEmail.split("@", 2);
        String prefix = parts[0];
        String domain = parts[1];

        int i = 2;
        String candidate = prefix + i + "@" + domain;
        while (userRepository.findByEmail(candidate).isPresent()) {
            i++;
            candidate = prefix + i + "@" + domain;
        }
        return candidate;
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
