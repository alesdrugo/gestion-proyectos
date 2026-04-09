package es.merida.tfg.gestion_proyectos.service;

import es.merida.tfg.gestion_proyectos.model.Task;
import es.merida.tfg.gestion_proyectos.model.Team;
import es.merida.tfg.gestion_proyectos.model.User;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from.name:Gestión de Proyectos}")
    private String fromName;

    @Value("${app.mail.from.address:no-reply@tfg.local}")
    private String fromAddress;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    /** Enviar email al asignar una tarea a un usuario (UTF-8 correcto) */
    public void sendTaskAssigned(User user, Task task) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return;

        try {
            String subject = "Nueva tarea asignada: " + safe(task.getTitle());
            String due = task.getDueDate() != null ? task.getDueDate().toString() : "sin fecha";
            String link = baseUrl + "/tasks/view/" + task.getId();

            String body = """
                    Hola %s,

                    Se te ha asignado una nueva tarea:

                    • Proyecto: %s
                    • Título: %s
                    • Fecha límite: %s

                    Puedes verla aquí:
                    %s

                    — %s
                    """.formatted(
                    safe(user.getUsername()),
                    safe(task.getProject() != null ? task.getProject().getName() : "-"),
                    safe(task.getTitle()),
                    due,
                    link,
                    fromName
            );

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            helper.setTo(user.getEmail());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setSubject(subject);
            helper.setText(body, false); 

            mailSender.send(mime);

        } catch (Exception e) {
            
            e.printStackTrace();
        }
    }

    public void sendDueSoon(User user, Task task) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return;

        try {
            String subject = " Tarea próxima a vencer: " + safe(task.getTitle());
            String due = task.getDueDate() != null
                    ? task.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "sin fecha";
            String link = baseUrl + "/tasks/view/" + task.getId();

            String body = """
                    Hola %s,

                    La tarea "%s" está próxima a vencer (fecha límite: %s).

                    Proyecto: %s
                    Enlace directo: %s

                    Te recomendamos revisarla cuanto antes.

                    — %s
                    """.formatted(
                    safe(user.getUsername()),
                    safe(task.getTitle()),
                    due,
                    safe(task.getProject() != null ? task.getProject().getName() : "-"),
                    link,
                    fromName
            );

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            helper.setTo(user.getEmail());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setSubject(subject);
            helper.setText(body, false);

            mailSender.send(mime);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**  Aviso: tarea vencida */
    public void sendOverdue(User user, Task task) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return;

        try {
            String subject = " Tarea vencida: " + safe(task.getTitle());
            String due = task.getDueDate() != null
                    ? task.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "sin fecha";
            String link = baseUrl + "/tasks/view/" + task.getId();

            String body = """
                    Hola %s,

                    La tarea "%s" está vencida desde el %s.

                    Proyecto: %s
                    Enlace directo: %s

                    Por favor, revisa o actualiza su estado.

                    — %s
                    """.formatted(
                    safe(user.getUsername()),
                    safe(task.getTitle()),
                    due,
                    safe(task.getProject() != null ? task.getProject().getName() : "-"),
                    link,
                    fromName
            );

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            helper.setTo(user.getEmail());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setSubject(subject);
            helper.setText(body, false);

            mailSender.send(mime);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void sendUserAssignedToTeam(User user, Team team, String roleName) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return;

        try {
            
            String teamName = (team != null) ? safe(team.getName()) : "PENDING";
            String roleLabel = "ROLE_MANAGER".equalsIgnoreCase(roleName) ? "Manager" : "Usuario";

            String subject = "Asignación de cuenta: " + teamName + " (" + roleLabel + ")";
            String link = baseUrl + "/dashboard";

            String body = """
                    Hola %s,

                    Tu cuenta ha sido actualizada por el administrador:

                    • Equipo: %s
                    • Rol: %s

                    Ya puedes acceder a la aplicación aquí:
                    %s

                    — %s
                    """.formatted(
                    safe(user.getUsername()),
                    teamName,
                    roleLabel,
                    link,
                    fromName
            );

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            helper.setTo(user.getEmail());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setSubject(subject);
            helper.setText(body, false);

            mailSender.send(mime);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private String safe(String s) { return s == null ? "-" : s; }
}