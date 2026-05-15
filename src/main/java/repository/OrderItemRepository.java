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

    @Query("""
        SELECT i.product.name, SUM(i.quantity),
               SUM(CASE WHEN i.subtotal IS NULL OR i.subtotal = 0 THEN COALESCE(i.order.totalAmount,0) ELSE i.subtotal END)
        FROM OrderItem i
        WHERE i.order.user = :user
          AND (i.order.status IS NULL OR i.order.status <> 'ĐÃ_HỦY')
        GROUP BY i.product.id, i.product.name
        ORDER BY SUM(i.quantity) DESC
        """)
    List<Object[]> findBestSellingProducts(@Param("user") AppUser user, Pageable pageable);

    @Query("""
        SELECT i.product.id, SUM(i.quantity)
        FROM OrderItem i
        WHERE i.order.user = :user
          AND (i.order.status IS NULL OR i.order.status <> 'ĐÃ_HỦY')
        GROUP BY i.product.id
        """)
    List<Object[]> findSoldQtyPerProduct(@Param("user") AppUser user);

    // ---- Daily report ----
    @Query("""
        SELECT i.product.name,
               SUM(i.quantity),
               SUM(CASE WHEN i.subtotal IS NULL OR i.subtotal = 0 THEN 0 ELSE i.subtotal END),
               SUM(CASE WHEN i.costPrice IS NULL THEN 0 ELSE i.costPrice * i.quantity END),
               SUM(CASE WHEN i.profit IS NULL THEN 0 ELSE i.profit END)
        FROM OrderItem i
        WHERE i.order.user = :user
          AND (i.order.status = 'HOÀN_THÀNH' OR i.order.status = 'ĐÃ_GIAO')
          AND i.order.createdAt >= :from AND i.order.createdAt < :to
        GROUP BY i.product.id, i.product.name
        ORDER BY SUM(i.quantity) DESC
        """)
    List<Object[]> findProductSummaryByDateRange(@Param("user") AppUser user,
                                                 @Param("from") LocalDateTime from,
                                                 @Param("to") LocalDateTime to);

    @Query("""
        SELECT COALESCE(SUM(i.quantity), 0) FROM OrderItem i
        WHERE i.order.user = :user
          AND (i.order.status = 'HOÀN_THÀNH' OR i.order.status = 'ĐÃ_GIAO')
          AND i.order.createdAt >= :from AND i.order.createdAt < :to
        """)
    Long sumQtySoldByDateRange(@Param("user") AppUser user,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

    @Query("""
        SELECT COALESCE(SUM(i.costPrice * i.quantity), 0) FROM OrderItem i
        WHERE i.order.user = :user
          AND (i.order.status = 'HOÀN_THÀNH' OR i.order.status = 'ĐÃ_GIAO')
          AND i.order.createdAt >= :from AND i.order.createdAt < :to
        """)
    BigDecimal sumCostOfGoodsByDateRange(@Param("user") AppUser user,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);
}