package repository;

import entity.AppUser;
import entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByUserAndActiveTrue(AppUser user);

    List<Product> findByUserAndActiveTrueOrderByIdDesc(AppUser user);

    List<Product> findByUser(AppUser user);

    Optional<Product> findFirstByCodeOrderByIdAsc(String code);

    Optional<Product> findFirstByCodeAndUserOrderByIdAsc(String code, AppUser user);

    Optional<Product> findFirstByCodeAndUserAndActiveTrueOrderByIdAsc(String code, AppUser user);

    boolean existsByCode(String code);

    boolean existsByCodeAndUser(String code, AppUser user);

    boolean existsByCodeAndUserAndActiveTrue(String code, AppUser user);

    boolean existsByCodeAndUserAndIdNot(String code, AppUser user, Long id);

    boolean existsByCodeAndUserAndActiveTrueAndIdNot(String code, AppUser user, Long id);

    Optional<Product> findByIdAndUser(Long id, AppUser user);

    Optional<Product> findByIdAndUserAndActiveTrue(Long id, AppUser user);

    Optional<Product> findFirstByNameContainingIgnoreCaseAndUserOrderByIdAsc(String name, AppUser user);

    Optional<Product> findFirstByNameContainingIgnoreCaseAndUserAndActiveTrueOrderByIdAsc(String name, AppUser user);

    List<Product> findTop5ByUserAndActiveTrueOrderByQuantityAsc(AppUser user);

    List<Product> findByUserAndActiveTrueAndQuantityEquals(AppUser user, Integer quantity);

    List<Product> findByUserAndActiveTrueAndQuantityLessThanEqual(AppUser user, Integer quantity);

    List<Product> findByUserAndActiveTrueAndExpiryDateBefore(AppUser user, LocalDate date);

    List<Product> findByUserAndActiveTrueAndExpiryDateBetween(AppUser user, LocalDate from, LocalDate to);

    List<Product> findAllByCodeOrderByIdAsc(String code);

    long countByUserAndActiveTrue(AppUser user);

    List<Product> findAllByActiveTrueAndQuantityGreaterThanOrderByQuantityDescIdDesc(int minQty);

    List<Product> findTop20ByUserAndActiveTrueAndQuantityGreaterThanAndQuantityLessThanEqualOrderByQuantityAscIdDesc(
            AppUser user,
            Integer minQuantity,
            Integer maxQuantity
    );

    List<Product> findTop20ByUserAndActiveTrueAndExpiryDateBetweenOrderByExpiryDateAscIdDesc(
            AppUser user,
            LocalDate from,
            LocalDate to
    );

    @Query("""
            SELECT p FROM Product p
            WHERE p.user = :user
              AND p.active = true
              AND (
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%'))
                    OR LOWER(p.code) LIKE LOWER(CONCAT('%', :kw, '%'))
                    OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :kw, '%'))
                    OR LOWER(p.category) LIKE LOWER(CONCAT('%', :kw, '%'))
                  )
            ORDER BY
              CASE WHEN p.quantity > 0 THEN 0 ELSE 1 END,
              p.quantity DESC,
              p.id DESC
            """)
    List<Product> searchByUserAndKeyword(@Param("user") AppUser user,
                                         @Param("kw") String kw);

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND p.quantity > 0
              AND (
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%'))
                    OR LOWER(p.code) LIKE LOWER(CONCAT('%', :kw, '%'))
                    OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :kw, '%'))
                    OR LOWER(p.category) LIKE LOWER(CONCAT('%', :kw, '%'))
                  )
            ORDER BY p.quantity DESC, p.id DESC
            """)
    List<Product> searchPublicProducts(@Param("kw") String kw);

    @Query("""
            SELECT p FROM Product p
            WHERE p.user = :user
              AND p.active = true
              AND (
                    :kw = ''
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :kw, '%'))
                    OR LOWER(p.code) LIKE LOWER(CONCAT('%', :kw, '%'))
                    OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :kw, '%'))
                    OR LOWER(p.category) LIKE LOWER(CONCAT('%', :kw, '%'))
                  )
              AND (
                    :stockStatus = ''
                    OR (:stockStatus = 'OUT_OF_STOCK' AND p.quantity = 0)
                    OR (:stockStatus = 'LOW_STOCK' AND p.quantity > 0 AND p.quantity <= 5)
                    OR (:stockStatus = 'AVAILABLE' AND p.quantity > 5)
                  )
              AND (
                    :expiryStatus = ''
                    OR (:expiryStatus = 'EXPIRED' AND p.expiryDate IS NOT NULL AND p.expiryDate < :today)
                    OR (:expiryStatus = 'EXPIRING_SOON' AND p.expiryDate IS NOT NULL AND p.expiryDate >= :today AND p.expiryDate <= :soonDate)
                  )
            ORDER BY
              CASE WHEN p.quantity > 0 THEN 0 ELSE 1 END,
              p.quantity DESC,
              p.id DESC
            """)
    Page<Product> filterProductsPaged(@Param("user") AppUser user,
                                      @Param("kw") String kw,
                                      @Param("stockStatus") String stockStatus,
                                      @Param("expiryStatus") String expiryStatus,
                                      @Param("today") LocalDate today,
                                      @Param("soonDate") LocalDate soonDate,
                                      Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Product p
            SET p.user = :owner
            WHERE p.user IS NULL OR p.user <> :owner
            """)
    int repairOwner(@Param("owner") AppUser owner);
}