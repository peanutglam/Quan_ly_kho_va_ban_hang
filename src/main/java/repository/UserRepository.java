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
}