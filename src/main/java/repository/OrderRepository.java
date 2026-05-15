package repository;

import entity.AppUser;
import entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserOrderByIdDesc(AppUser user);

    Optional<Order> findByIdAndUser(Long id, AppUser user);
    Optional<Order> findByOrderCode(String orderCode);
    Optional<Order> findByOrderCodeAndUser(String orderCode, AppUser user);

    boolean existsByOrderCode(String orderCode);
    long countByUser(AppUser user);
    long countByUserAndStatus(AppUser user, String status);

    @Query("""
        SELECT COALESCE(SUM(o.totalAmount), 0)
        FROM Order o
        WHERE o.user = :user
          AND (o.status = 'HOÀN_THÀNH' OR o.status = 'ĐÃ_GIAO')
        """)
    BigDecimal sumRevenueByUser(@Param("user") AppUser user);

    @Query("""
        SELECT o FROM Order o
        WHERE o.user = :user
          AND (:kw IS NULL OR :kw = ''
               OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :kw, '%'))
               OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :kw, '%'))
               OR LOWER(o.customerPhone) LIKE LOWER(CONCAT('%', :kw, '%')))
          AND (:status IS NULL OR :status = '' OR o.status = :status)
        ORDER BY o.id DESC
        """)
    Page<Order> filterOrdersPaged(@Param("user") AppUser user,
                                  @Param("kw") String kw,
                                  @Param("status") String status,
                                  Pageable pageable);

    // ---- Daily report queries ----
    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.user = :user
          AND o.createdAt >= :from AND o.createdAt < :to
        """)
    long countByUserAndDateRange(@Param("user") AppUser user,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);

    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.user = :user AND o.status = :status
          AND o.createdAt >= :from AND o.createdAt < :to
        """)
    long countByUserAndStatusAndDateRange(@Param("user") AppUser user,
                                          @Param("status") String status,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    @Query("""
        SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
        WHERE o.user = :user
          AND (o.status = 'HOÀN_THÀNH' OR o.status = 'ĐÃ_GIAO')
          AND o.createdAt >= :from AND o.createdAt < :to
        """)
    BigDecimal sumRevenueByUserAndDateRange(@Param("user") AppUser user,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    @Query("""
        SELECT o FROM Order o
        WHERE o.user = :user
          AND o.createdAt >= :from AND o.createdAt < :to
        ORDER BY o.id DESC
        """)
    List<Order> findByUserAndDateRange(@Param("user") AppUser user,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);
}
