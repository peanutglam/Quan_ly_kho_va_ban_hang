package repository;

import entity.AppUser;
import entity.InventoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    @EntityGraph(attributePaths = {"actor", "product"})
    Page<InventoryLog> findByUserOrderByCreatedAtDescIdDesc(AppUser user, Pageable pageable);
}