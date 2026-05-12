package repository;

import entity.AppUser;
import entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("""
        SELECT o FROM Order o
        WHERE o.user = :user
          AND (
                :kw IS NULL OR :kw = ''
                OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :kw, '%'))
                OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :kw, '%'))
                OR LOWER(o.customerPhone) LIKE LOWER(CONCAT('%', :kw, '%'))
          )
          AND (
                :status IS NULL OR :status = ''
                OR o.status = :status
          )
        ORDER BY o.id DESC
        """)
    Page<Order> filterOrdersPaged(@Param("user") AppUser user,
                                  @Param("kw") String kw,
                                  @Param("status") String status,
                                  Pageable pageable);
    List<Order> findByUserOrderByIdDesc(AppUser user);
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
    List<Object[]> findBestSellingProductsAll(@Param("user") AppUser user);
    Optional<Order> findByIdAndUser(Long id, AppUser user);

    Optional<Order> findByOrderCode(String orderCode);

    Optional<Order> findByOrderCodeAndUser(String orderCode, AppUser user);

    boolean existsByOrderCode(String orderCode);

    long countByUser(AppUser user);

    long countByUserAndStatus(AppUser user, String status);
}