package es.merida.tfg.gestion_proyectos.repository;

import es.merida.tfg.gestion_proyectos.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name); 
}
