package repository;

import entity.AppUser;
import entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByUserAndActiveTrue(AppUser user);

    List<Product> findByUserAndActiveTrueOrderByIdDesc(AppUser user);

    List<Product> findByUser(AppUser user);

    Optional<Product> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT p FROM Product p WHERE p.user = :user AND p.active = true AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(p.code) LIKE LOWER(CONCAT('%', :kw, '%')))")
    List<Product> searchByUserAndKeyword(@Param("user") AppUser user, @Param("kw") String kw);
    @Query("""
        SELECT p FROM Product p
        WHERE p.user = :user
          AND p.active = true
          AND (
                :kw IS NULL OR :kw = ''
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%'))
                OR LOWER(p.code) LIKE LOWER(CONCAT('%', :kw, '%'))
          )
          AND (
                :stockStatus IS NULL OR :stockStatus = ''
                OR (:stockStatus = 'OUT_OF_STOCK' AND p.quantity = 0)
                OR (:stockStatus = 'LOW_STOCK' AND p.quantity > 0 AND p.quantity <= 5)
                OR (:stockStatus = 'AVAILABLE' AND p.quantity > 5)
          )
          AND (
                :expiryStatus IS NULL OR :expiryStatus = ''
                OR (:expiryStatus = 'EXPIRED' AND p.expiryDate IS NOT NULL AND p.expiryDate < :today)
                OR (:expiryStatus = 'EXPIRING_SOON'
                    AND p.expiryDate IS NOT NULL
                    AND p.expiryDate >= :today
                    AND p.expiryDate <= :soonDate)
          )
        ORDER BY p.id DESC
        """)
    Page<Product> filterProductsPaged(@Param("user") AppUser user,
                                      @Param("kw") String kw,
                                      @Param("stockStatus") String stockStatus,
                                      @Param("expiryStatus") String expiryStatus,
                                      @Param("today") LocalDate today,
                                      @Param("soonDate") LocalDate soonDate,
                                      Pageable pageable);
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

    List<Product> findAllByCodeOrderByIdAsc(String code);

    Optional<Product> findFirstByCodeOrderByIdAsc(String code);
}