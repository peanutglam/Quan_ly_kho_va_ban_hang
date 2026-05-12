package repository;

import entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    List<AppUser> findAllByOrderByIdDesc();

    List<AppUser> findByOwnerOrderByIdDesc(AppUser owner);

    Optional<AppUser> findByIdAndOwner(Long id, AppUser owner);

    Optional<AppUser> findFirstByUsernameAndActiveTrueOrderByIdAsc(String username);

    Optional<AppUser> findFirstByUsernameOrderByIdAsc(String username);

    Optional<AppUser> findFirstByRoleAndActiveTrueOrderByIdAsc(String role);

    Optional<AppUser> findFirstByRoleOrderByIdAsc(String role);

    List<AppUser> findByRoleAndActiveTrueOrderByIdAsc(String role);
}