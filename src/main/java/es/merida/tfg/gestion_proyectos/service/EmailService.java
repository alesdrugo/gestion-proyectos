package es.merida.tfg.gestion_proyectos.service;

import es.merida.tfg.gestion_proyectos.model.Task;
import es.merida.tfg.gestion_proyectos.model.User;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

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
            helper.setText(body, false); // pon true si quieres HTML

            mailSender.send(mime);

        } catch (Exception e) {
            // loguea si quieres
            e.printStackTrace();
        }
    }

    private String safe(String s) { return s == null ? "-" : s; }
}