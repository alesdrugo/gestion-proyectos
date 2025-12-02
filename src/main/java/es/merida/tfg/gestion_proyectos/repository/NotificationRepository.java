package es.merida.tfg.gestion_proyectos.repository;

import es.merida.tfg.gestion_proyectos.model.Notification;
import es.merida.tfg.gestion_proyectos.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByUserAndReadIsFalse(User user);

    List<Notification> findTop10ByUserOrderByCreatedAtDesc(User user);

    // Necesario para markAllAsRead(...)
    List<Notification> findByUserAndReadIsFalse(User user);
}