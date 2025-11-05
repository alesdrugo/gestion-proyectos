package es.merida.tfg.gestion_proyectos.repository;

import es.merida.tfg.gestion_proyectos.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findAllByRoles_Name(String roleName);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameAndEnabledTrue(String username);
    List<User> findByEnabledTrueOrderByUsernameAsc();

}


