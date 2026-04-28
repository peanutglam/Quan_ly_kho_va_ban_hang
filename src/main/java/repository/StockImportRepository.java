package repository;
import entity.AppUser;
import entity.Product;
import entity.StockImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockImportRepository extends JpaRepository<StockImport, Long> {

    List<StockImport> findByUserOrderByIdDesc(AppUser user);
    List<StockImport> findByUser(AppUser user);

    // Tổng số lượng đã nhập theo từng product
    @Query("SELECT s.product.id, SUM(s.quantity) FROM StockImport s WHERE s.user = :user GROUP BY s.product.id")
    List<Object[]> findTotalImportedPerProduct(@Param("user") AppUser user);
}