package repository;

import entity.AppUser;
import entity.StockImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface StockImportRepository extends JpaRepository<StockImport, Long> {

    List<StockImport> findByUserOrderByIdDesc(AppUser user);
    List<StockImport> findByUser(AppUser user);
    long countByUser(AppUser user);

    @Query("SELECT s.product.id, SUM(s.quantity) FROM StockImport s WHERE s.user = :user GROUP BY s.product.id")
    List<Object[]> findTotalImportedPerProduct(@Param("user") AppUser user);

    @Query("""
        SELECT COALESCE(SUM(s.importPrice * s.quantity), 0) FROM StockImport s
        WHERE s.user = :user AND s.createdAt >= :from AND s.createdAt < :to
        """)
    BigDecimal sumImportTotalByDateRange(@Param("user") AppUser user,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    @Query("""
        SELECT COALESCE(SUM(s.quantity), 0) FROM StockImport s
        WHERE s.user = :user AND s.createdAt >= :from AND s.createdAt < :to
        """)
    Long sumImportQtyByDateRange(@Param("user") AppUser user,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);
}
