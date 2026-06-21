package repository;

import entity.AppUser;
import entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserOrderByIdDesc(AppUser user, Pageable pageable);
    List<Order> findByUserOrderByIdDesc(AppUser user);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndUser(Long id, AppUser user);

    long countByUser(AppUser user);

    long countByUserAndStatus(AppUser user, String status);

    boolean existsByOrderCode(String orderCode);
    @Query("""
        SELECT 
            FUNCTION('DATE', o.createdAt),
            COUNT(o.id),
            COALESCE(SUM(
                CASE
                    WHEN o.status = 'ĐÃ_GIAO' OR o.status = 'HOÀN_THÀNH'
                    THEN COALESCE(o.totalBill, o.totalAmount, 0)
                    ELSE 0
                END
            ), 0)
        FROM Order o
        WHERE o.user = :user
          AND o.createdAt >= :start
          AND o.createdAt < :end
          AND (o.status IS NULL OR o.status <> 'ĐÃ_HỦY')
        GROUP BY FUNCTION('DATE', o.createdAt)
        ORDER BY FUNCTION('DATE', o.createdAt) ASC
        """)
    List<Object[]> findDailyOrderRevenueTrend(@Param("user") AppUser user,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);
    @Query("""
            SELECT DISTINCT o FROM Order o
            WHERE o.user = :user
              AND (
                    :status = ''
                    OR o.status = :status
                  )
              AND (
                    :kw = ''
                    OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :kw, '%'))
                    OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :kw, '%'))
                    OR LOWER(o.customerPhone) LIKE LOWER(CONCAT('%', :kw, '%'))
                    OR LOWER(o.customerAddress) LIKE LOWER(CONCAT('%', :kw, '%'))
                  )
            ORDER BY o.id DESC
            """)
    Page<Order> filterOrdersPaged(@Param("user") AppUser user,
                                  @Param("kw") String kw,
                                  @Param("status") String status,
                                  Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0)
            FROM Order o
            WHERE o.user = :user
              AND (
                    o.status = 'ĐÃ_GIAO'
                    OR o.status = 'HOÀN_THÀNH'
                  )
            """)
    BigDecimal sumRevenueByUser(@Param("user") AppUser user);

    @Query("""
            SELECT MONTH(o.createdAt), COALESCE(SUM(o.totalAmount), 0)
            FROM Order o
            WHERE o.user = :user
              AND (
                    o.status = 'ĐÃ_GIAO'
                    OR o.status = 'HOÀN_THÀNH'
                  )
              AND o.createdAt IS NOT NULL
            GROUP BY MONTH(o.createdAt)
            ORDER BY MONTH(o.createdAt)
            """)
    List<Object[]> revenueByMonth(@Param("user") AppUser user);

    @Query("""
            SELECT COUNT(o)
            FROM Order o
            WHERE o.user = :user
              AND o.createdAt >= :start
              AND o.createdAt < :end
            """)
    long countByUserAndDateRange(@Param("user") AppUser user,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    @Query("""
            SELECT COUNT(o)
            FROM Order o
            WHERE o.user = :user
              AND o.status = :status
              AND o.createdAt >= :start
              AND o.createdAt < :end
            """)
    long countByUserAndStatusAndDateRange(@Param("user") AppUser user,
                                          @Param("status") String status,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0)
            FROM Order o
            WHERE o.user = :user
              AND (
                    o.status = 'ĐÃ_GIAO'
                    OR o.status = 'HOÀN_THÀNH'
                  )
              AND o.createdAt >= :start
              AND o.createdAt < :end
            """)
    BigDecimal sumRevenueByUserAndDateRange(@Param("user") AppUser user,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("""
            SELECT DISTINCT o
            FROM Order o
            WHERE o.user = :user
              AND o.createdAt >= :start
              AND o.createdAt < :end
            ORDER BY o.id DESC
            """)
    List<Order> findByUserAndDateRange(@Param("user") AppUser user,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Order o
            SET o.user = :owner
            WHERE o.user IS NULL OR o.user <> :owner
            """)
    int repairOwner(@Param("owner") AppUser owner);
}