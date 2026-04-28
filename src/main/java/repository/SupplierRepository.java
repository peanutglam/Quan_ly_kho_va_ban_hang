package repository;

import entity.AppUser;
import entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByUserOrderByIdDesc(AppUser user);
    Optional<Supplier> findByIdAndUser(Long id, AppUser user);
    Optional<Supplier> findByNameAndUser(String name, AppUser user);
}
