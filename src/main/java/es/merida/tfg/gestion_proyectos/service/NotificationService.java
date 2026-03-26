package es.merida.tfg.gestion_proyectos.service;

import es.merida.tfg.gestion_proyectos.model.Notification;
import es.merida.tfg.gestion_proyectos.model.User;
import es.merida.tfg.gestion_proyectos.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;

    /** Crear notificación rápida (alias de create). */
    public void notify(User user, String message, String link) {
        create(user, message, link);
    }

    /** Crear notificación (sin título, porque Notification no tiene campo title). */
    public Notification create(User user, String message, String link) {
        if (user == null) return null;
        Notification n = Notification.builder()
                .user(user)
                .message(message)
                .link(link)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
        return repo.save(n);
    }

    /** Nº de no leídas para el usuario. */
    public long countUnread(User user) {
        return (user == null) ? 0 : repo.countByUserAndReadIsFalse(user);
    }

    /** Últimas 10 notificaciones (nombre esperado por el Controller). */
    public List<Notification> getLatest(User user) {
        return (user == null) ? List.of() : repo.findTop10ByUserOrderByCreatedAtDesc(user);
    }

    /** Alias opcional si en alguna vista llamabas a latest(...) */
    public List<Notification> latest(User user) {
        return getLatest(user);
    }

    /** Marcar todas como leídas. */
    @Transactional
    public void markAllAsRead(User user) {
        if (user == null) return;
        List<Notification> unread = repo.findByUserAndReadIsFalse(user);
        if (unread.isEmpty()) return;
        unread.forEach(n -> n.setRead(true));
        repo.saveAll(unread);
    }

    /** Marcar UNA como leída (si pertenece al usuario). */
    @Transactional
    public void markAsRead(User user, Long notificationId) {
        if (user == null || notificationId == null) return;

        Notification n = repo.findByIdAndUser(notificationId, user)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));

        if (!n.isRead()) {
            n.setRead(true);
            repo.save(n);
        }
    }
}