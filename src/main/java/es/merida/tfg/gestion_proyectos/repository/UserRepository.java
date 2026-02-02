package es.merida.tfg.gestion_proyectos.repository;

import es.merida.tfg.gestion_proyectos.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // =========================
    // Básicos
    // =========================
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameAndEnabledTrue(String username);

    List<User> findByEnabledTrueOrderByUsernameAsc();

    // =========================
    // Por equipo (aislamiento)
    // =========================
    List<User> findByTeamId(Long teamId);
    Optional<User> findByIdAndTeamId(Long id, Long teamId);
    long countByTeamId(Long teamId);

    // Para validar "1 manager por equipo"
    long countByTeamIdAndRoles_Name(Long teamId, String roleName);

    // =========================
    // Por rol (si lo usas)
    // =========================
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findAllByRole(@Param("roleName") String roleName);

    // =========================
    // Admin - búsqueda + filtros + paginado (incluye usuarios sin roles)
    // =========================
    @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN u.roles r
        WHERE
          (:q IS NULL OR :q = ''
              OR lower(u.username) LIKE lower(concat('%', :q, '%'))
              OR lower(u.email) LIKE lower(concat('%', :q, '%')))
          AND (:enabled IS NULL OR u.enabled = :enabled)
          AND (:teamId IS NULL OR u.team.id = :teamId)
          AND (:roleName IS NULL OR :roleName = '' OR r.name = :roleName)
    """)
    Page<User> search(@Param("q") String q,
                      @Param("enabled") Boolean enabled,
                      @Param("teamId") Long teamId,
                      @Param("roleName") String roleName,
                      Pageable pageable);

    // =========================
    // Admin - búsqueda excluyendo admins (RECOMENDADA)
    // - incluye pending (sin roles)
    // - excluye ROLE_ADMIN aunque tenga otros roles
    // =========================
    @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN u.roles r
        WHERE
          NOT EXISTS (
            SELECT 1
            FROM User u2
            JOIN u2.roles r2
            WHERE u2.id = u.id AND lower(r2.name) = 'role_admin'
          )
          AND (:q IS NULL OR :q = ''
              OR lower(u.username) LIKE lower(concat('%', :q, '%'))
              OR lower(u.email) LIKE lower(concat('%', :q, '%')))
          AND (:enabled IS NULL OR u.enabled = :enabled)
          AND (:teamId IS NULL OR u.team.id = :teamId)
          AND (
              :roleName IS NULL OR :roleName = ''
              OR EXISTS (
                  SELECT 1
                  FROM User u3
                  JOIN u3.roles r3
                  WHERE u3.id = u.id AND lower(r3.name) = lower(:roleName)
              )
          )
    """)
    Page<User> searchNonAdmins(@Param("q") String q,
                              @Param("enabled") Boolean enabled,
                              @Param("teamId") Long teamId,
                              @Param("roleName") String roleName,
                              Pageable pageable);

    // =========================
    // Pendientes (útiles si tienes filtros rápidos)
    // =========================
    Page<User> findByTeamIsNull(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.team IS NOT NULL AND u.roles IS EMPTY")
    Page<User> findByTeamAssignedButNoRoles(Pageable pageable);
}
