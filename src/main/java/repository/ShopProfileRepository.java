package repository;

import entity.AppUser;
import entity.ShopProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShopProfileRepository extends JpaRepository<ShopProfile, Long> {
    Optional<ShopProfile> findByUser(AppUser user);
}
