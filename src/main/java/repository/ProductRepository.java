package repository;

import entity.AppUser;
import entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByUserAndActiveTrue(AppUser user);
    List<Product> findByUserAndActiveTrueOrderByIdDesc(AppUser user);
    List<Product> findByUser(AppUser user);

    @Query("SELECT p FROM Product p WHERE p.user = :user AND p.active = true AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(p.code) LIKE LOWER(CONCAT('%', :kw, '%')))")
    List<Product> searchByUserAndKeyword(@Param("user") AppUser user, @Param("kw") String kw);

    boolean existsByCodeAndUser(String code, AppUser user);
    boolean existsByCodeAndUserAndActiveTrue(String code, AppUser user);
    boolean existsByCodeAndUserAndIdNot(String code, AppUser user, Long id);
    boolean existsByCodeAndUserAndActiveTrueAndIdNot(String code, AppUser user, Long id);

    Optional<Product> findByIdAndUser(Long id, AppUser user);
    Optional<Product> findByIdAndUserAndActiveTrue(Long id, AppUser user);
    Optional<Product> findByCodeAndUser(String code, AppUser user);
    Optional<Product> findByCodeAndUserAndActiveTrue(String code, AppUser user);
    Optional<Product> findFirstByNameContainingIgnoreCaseAndUser(String name, AppUser user);
    Optional<Product> findFirstByNameContainingIgnoreCaseAndUserAndActiveTrue(String name, AppUser user);

    List<Product> findTop5ByUserAndActiveTrueOrderByQuantityAsc(AppUser user);

    List<Product> findByUserAndActiveTrueAndQuantityEquals(AppUser user, Integer quantity);
    List<Product> findByUserAndActiveTrueAndQuantityLessThanEqual(AppUser user, Integer quantity);
    List<Product> findByUserAndActiveTrueAndExpiryDateBefore(AppUser user, LocalDate date);
    List<Product> findByUserAndActiveTrueAndExpiryDateBetween(AppUser user, LocalDate startDate, LocalDate endDate);
}
