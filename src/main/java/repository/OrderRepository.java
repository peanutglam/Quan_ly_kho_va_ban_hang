package repository;

import entity.AppUser;
import entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserOrderByIdDesc(AppUser user);

    Optional<Order> findByIdAndUser(Long id, AppUser user);

    long countByUser(AppUser user);

    long countByUserAndStatus(AppUser user, String status);

    boolean existsByOrderCode(String orderCode);

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

    /*
     * Method đang được OrderService.totalRevenue() gọi.
     * Chỉ tính doanh thu đơn đã giao / hoàn thành.
     */
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

    /*
     * Bản tổng quát hơn nếu service cần truyền danh sách trạng thái.
     */
    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0)
            FROM Order o
            WHERE o.user = :user
              AND o.status IN :statuses
            """)
    BigDecimal sumTotalAmountByUserAndStatuses(@Param("user") AppUser user,
                                               @Param("statuses") List<String> statuses);

    /*
     * Đếm tổng đơn trong khoảng ngày.
     */
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

    /*
     * Đếm đơn theo trạng thái trong khoảng ngày.
     */
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

    /*
     * Tính doanh thu trong khoảng ngày.
     * Chỉ tính đơn đã giao / hoàn thành.
     */
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

    /*
     * Lấy danh sách đơn trong khoảng ngày.
     */
    @Query("""
            SELECT o
            FROM Order o
            WHERE o.user = :user
              AND o.createdAt >= :start
              AND o.createdAt < :end
            ORDER BY o.id DESC
            """)
    List<Order> findByUserAndDateRange(@Param("user") AppUser user,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    /*
     * Doanh thu theo tháng cho dashboard.
     * Object[] = [monthNumber, totalAmount]
     */
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

    /*
     * Tối ưu DataOwnershipRepairRunner:
     * bulk update trực tiếp trong DB, không load toàn bộ orders lên RAM.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Order o
            SET o.user = :owner
            WHERE o.user IS NULL OR o.user <> :owner
            """)
    int repairOwner(@Param("owner") AppUser owner);
}