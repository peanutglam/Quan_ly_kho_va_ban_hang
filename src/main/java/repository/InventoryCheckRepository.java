package repository;

import entity.AppUser;
import entity.InventoryCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryCheckRepository extends JpaRepository<InventoryCheck, Long> {
    @EntityGraph(attributePaths = {"actor", "product"})
    Page<InventoryCheck> findByUserOrderByCheckedAtDescIdDesc(AppUser user, Pageable pageable);
}