package repository;

import entity.AppUser;
import entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /*
     * Top sản phẩm bán chạy cho Dashboard.
     * Object[] gồm:
     * [0] tên sản phẩm
     * [1] tổng số lượng bán
     * [2] tổng doanh thu
     */
    @Query("""
            SELECT i.product.name,
                   COALESCE(SUM(i.quantity), 0),
                   COALESCE(SUM(
                        CASE
                            WHEN i.subtotal IS NULL OR i.subtotal = 0
                            THEN i.unitPrice * i.quantity
                            ELSE i.subtotal
                        END
                   ), 0)
            FROM OrderItem i
            WHERE i.order.user = :user
              AND (i.order.status IS NULL OR i.order.status <> 'ĐÃ_HỦY')
              AND i.product IS NOT NULL
            GROUP BY i.product.id, i.product.name
            ORDER BY COALESCE(SUM(i.quantity), 0) DESC
            """)
    List<Object[]> findBestSellingProducts(@Param("user") AppUser user,
                                           Pageable pageable);

    /*
     * Tổng số lượng đã bán theo từng sản phẩm.
     * Dùng cho đồng bộ thống kê sản phẩm.
     */
    @Query("""
            SELECT i.product.id,
                   COALESCE(SUM(i.quantity), 0)
            FROM OrderItem i
            WHERE i.order.user = :user
              AND (i.order.status IS NULL OR i.order.status <> 'ĐÃ_HỦY')
              AND i.product IS NOT NULL
            GROUP BY i.product.id
            """)
    List<Object[]> findSoldQtyPerProduct(@Param("user") AppUser user);

    /*
     * Tổng vốn hàng bán trong ngày/khoảng ngày.
     * Đây là method OrderService đang gọi:
     * sumCostOfGoodsByDateRange(ownerUser, from, to)
     */
    @Query("""
            SELECT COALESCE(SUM(
                CASE
                    WHEN i.costPrice IS NULL
                    THEN 0
                    ELSE i.costPrice * i.quantity
                END
            ), 0)
            FROM OrderItem i
            WHERE i.order.user = :user
              AND (
                    i.order.status = 'ĐÃ_GIAO'
                    OR i.order.status = 'HOÀN_THÀNH'
                  )
              AND i.order.createdAt >= :start
              AND i.order.createdAt < :end
            """)
    BigDecimal sumCostOfGoodsByDateRange(@Param("user") AppUser user,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    /*
     * Tổng số lượng sản phẩm đã bán trong ngày/khoảng ngày.
     * Đây là method OrderService đang gọi:
     * sumQtySoldByDateRange(ownerUser, from, to)
     */
    @Query("""
            SELECT COALESCE(SUM(i.quantity), 0)
            FROM OrderItem i
            WHERE i.order.user = :user
              AND (
                    i.order.status = 'ĐÃ_GIAO'
                    OR i.order.status = 'HOÀN_THÀNH'
                  )
              AND i.order.createdAt >= :start
              AND i.order.createdAt < :end
            """)
    Long sumQtySoldByDateRange(@Param("user") AppUser user,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    /*
     * Báo cáo sản phẩm bán trong ngày/khoảng ngày.
     * Đây là method OrderService đang gọi:
     * findProductSummaryByDateRange(ownerUser, from, to)
     *
     * Object[] gồm:
     * [0] tên sản phẩm
     * [1] tổng số lượng bán
     * [2] doanh thu
     * [3] vốn
     * [4] lợi nhuận
     */
    @Query("""
            SELECT i.product.name,
                   COALESCE(SUM(i.quantity), 0),
                   COALESCE(SUM(
                        CASE
                            WHEN i.subtotal IS NULL OR i.subtotal = 0
                            THEN i.unitPrice * i.quantity
                            ELSE i.subtotal
                        END
                   ), 0),
                   COALESCE(SUM(
                        CASE
                            WHEN i.costPrice IS NULL
                            THEN 0
                            ELSE i.costPrice * i.quantity
                        END
                   ), 0),
                   COALESCE(SUM(
                        CASE
                            WHEN i.profit IS NULL
                            THEN 0
                            ELSE i.profit
                        END
                   ), 0)
            FROM OrderItem i
            WHERE i.order.user = :user
              AND (
                    i.order.status = 'ĐÃ_GIAO'
                    OR i.order.status = 'HOÀN_THÀNH'
                  )
              AND i.order.createdAt >= :start
              AND i.order.createdAt < :end
              AND i.product IS NOT NULL
            GROUP BY i.product.id, i.product.name
            ORDER BY COALESCE(SUM(i.quantity), 0) DESC
            """)
    List<Object[]> findProductSummaryByDateRange(@Param("user") AppUser user,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    /*
     * Giữ thêm tên method dạng mới để tránh lỗi nếu file khác đang gọi tên này.
     */
    @Query("""
            SELECT COALESCE(SUM(
                CASE
                    WHEN i.costPrice IS NULL
                    THEN 0
                    ELSE i.costPrice * i.quantity
                END
            ), 0)
            FROM OrderItem i
            WHERE i.order.user = :user
              AND (
                    i.order.status = 'ĐÃ_GIAO'
                    OR i.order.status = 'HOÀN_THÀNH'
                  )
              AND i.order.createdAt >= :start
              AND i.order.createdAt < :end
            """)
    BigDecimal sumCostOfGoodsByUserAndDateRange(@Param("user") AppUser user,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(i.quantity), 0)
            FROM OrderItem i
            WHERE i.order.user = :user
              AND (
                    i.order.status = 'ĐÃ_GIAO'
                    OR i.order.status = 'HOÀN_THÀNH'
                  )
              AND i.order.createdAt >= :start
              AND i.order.createdAt < :end
            """)
    Long sumQtySoldByUserAndDateRange(@Param("user") AppUser user,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    @Query("""
            SELECT i.product.name,
                   COALESCE(SUM(i.quantity), 0),
                   COALESCE(SUM(
                        CASE
                            WHEN i.subtotal IS NULL OR i.subtotal = 0
                            THEN i.unitPrice * i.quantity
                            ELSE i.subtotal
                        END
                   ), 0),
                   COALESCE(SUM(
                        CASE
                            WHEN i.costPrice IS NULL
                            THEN 0
                            ELSE i.costPrice * i.quantity
                        END
                   ), 0),
                   COALESCE(SUM(
                        CASE
                            WHEN i.profit IS NULL
                            THEN 0
                            ELSE i.profit
                        END
                   ), 0)
            FROM OrderItem i
            WHERE i.order.user = :user
              AND (
                    i.order.status = 'ĐÃ_GIAO'
                    OR i.order.status = 'HOÀN_THÀNH'
                  )
              AND i.order.createdAt >= :start
              AND i.order.createdAt < :end
              AND i.product IS NOT NULL
            GROUP BY i.product.id, i.product.name
            ORDER BY COALESCE(SUM(i.quantity), 0) DESC
            """)
    List<Object[]> findProductSummariesByDateRange(@Param("user") AppUser user,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);
}