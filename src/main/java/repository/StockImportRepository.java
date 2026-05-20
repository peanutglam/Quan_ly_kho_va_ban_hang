package repository;

import entity.AppUser;
import entity.StockImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface StockImportRepository extends JpaRepository<StockImport, Long> {

    List<StockImport> findByUserOrderByIdDesc(AppUser user);

    long countByUser(AppUser user);

    @Query("""
            SELECT si.product.id,
                   COALESCE(SUM(si.quantity), 0)
            FROM StockImport si
            WHERE si.user = :user
              AND si.product IS NOT NULL
            GROUP BY si.product.id
            """)
    List<Object[]> findTotalImportedPerProduct(@Param("user") AppUser user);

    @Query("""
            SELECT COALESCE(SUM(si.importPrice * si.quantity), 0)
            FROM StockImport si
            WHERE si.user = :user
              AND si.createdAt >= :start
              AND si.createdAt < :end
              AND si.importPrice IS NOT NULL
              AND si.quantity IS NOT NULL
            """)
    BigDecimal sumImportTotalByDateRange(@Param("user") AppUser user,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(si.quantity), 0)
            FROM StockImport si
            WHERE si.user = :user
              AND si.createdAt >= :start
              AND si.createdAt < :end
              AND si.quantity IS NOT NULL
            """)
    Long sumImportQtyByDateRange(@Param("user") AppUser user,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    @Query("""
            SELECT si
            FROM StockImport si
            WHERE si.user = :user
              AND si.createdAt >= :start
              AND si.createdAt < :end
            ORDER BY si.id DESC
            """)
    List<StockImport> findByUserAndDateRange(@Param("user") AppUser user,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE StockImport si
            SET si.user = :owner
            WHERE si.user IS NULL OR si.user <> :owner
            """)
    int repairOwner(@Param("owner") AppUser owner);
}