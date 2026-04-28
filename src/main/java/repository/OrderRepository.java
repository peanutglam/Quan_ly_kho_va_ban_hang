package repository;

import entity.AppUser;
import entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserOrderByIdDesc(AppUser user);

    Optional<Order> findByIdAndUser(Long id, AppUser user);

    Optional<Order> findByOrderCodeAndUser(String orderCode, AppUser user);

    long countByUser(AppUser user);

    long countByUserAndStatus(AppUser user, String status);
}