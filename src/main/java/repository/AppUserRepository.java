package repository;

import entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByUsernameAndActiveTrue(String username);

    boolean existsByUsername(String username);

    Optional<AppUser> findFirstByRoleAndActiveTrueOrderByIdAsc(String role);

    List<AppUser> findByOwnerAndActiveTrueOrderByIdDesc(AppUser owner);

    Page<AppUser> findByRoleOrderByIdDesc(String role, Pageable pageable);

    Page<AppUser> findByOwnerAndRoleOrderByIdDesc(AppUser owner, String role, Pageable pageable);

    Optional<AppUser> findByIdAndRole(Long id, String role);

    Optional<AppUser> findByIdAndOwnerAndRole(Long id, AppUser owner, String role);
}