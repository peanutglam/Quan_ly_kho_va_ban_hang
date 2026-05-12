package repository;

import entity.AppUser;
import entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Top sản phẩm bán chạy theo số lượng, loại trừ đơn đã hủy.
    @Query("""
        SELECT i.product.name,
               SUM(i.quantity),
               SUM(
                   CASE
                       WHEN i.subtotal IS NULL OR i.subtotal = 0
                       THEN COALESCE(i.order.totalAmount, 0)
                       ELSE i.subtotal
                   END
               )
        FROM OrderItem i
        WHERE i.order.user = :user
          AND (i.order.status IS NULL OR i.order.status <> 'ĐÃ_HỦY')
        GROUP BY i.product.id, i.product.name
        ORDER BY SUM(i.quantity) DESC,
                 SUM(
                   CASE
                       WHEN i.subtotal IS NULL OR i.subtotal = 0
                       THEN COALESCE(i.order.totalAmount, 0)
                       ELSE i.subtotal
                   END
                 ) DESC
        """)
    List<Object[]> findBestSellingProducts(@Param("user") AppUser user, Pageable pageable);

    // Số lượng đã bán theo từng product id
    @Query("""
            SELECT i.product.id, SUM(i.quantity)
            FROM OrderItem i
            WHERE i.order.user = :user
              AND (i.order.status IS NULL OR i.order.status <> 'ĐÃ_HỦY')
            GROUP BY i.product.id
            """)
    List<Object[]> findSoldQtyPerProduct(@Param("user") AppUser user);

}
