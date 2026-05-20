package repository;

import entity.AppUser;
import entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByUserOrderByIdDesc(AppUser user);

    Optional<Supplier> findByIdAndUser(Long id, AppUser user);

    Optional<Supplier> findFirstByNameAndUserOrderByIdAsc(String name, AppUser user);

    List<Supplier> findAllByNameAndUserOrderByIdAsc(String name, AppUser user);

    long countByUser(AppUser user);

    /*
     * Tối ưu DataOwnershipRepairRunner:
     * không load toàn bộ suppliers lên RAM.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Supplier s
            SET s.user = :owner
            WHERE s.user IS NULL OR s.user <> :owner
            """)
    int repairOwner(@Param("owner") AppUser owner);
}