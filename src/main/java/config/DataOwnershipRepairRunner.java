package config;

import entity.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import repository.UserRepository;

import java.util.Locale;

/**
 * Chỉ kiểm tra xem Owner chính có tồn tại không.
 * KHÔNG gọi findAll() trên các bảng lớn như products/orders để tránh OOM.
 * DataOwnership repair được bỏ - dữ liệu đã được normalize từ trước.
 */
@Component
@Order(2)
public class DataOwnershipRepairRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    @Value("${app.owner.username:owner}")
    private String ownerUsername;

    public DataOwnershipRepairRunner(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        String username = ownerUsername == null ? "owner" : ownerUsername.trim().toLowerCase(Locale.ROOT);
        AppUser owner = userRepository.findFirstByUsernameOrderByIdAsc(username)
                .or(() -> userRepository.findFirstByRoleAndActiveTrueOrderByIdAsc(AppUser.ROLE_OWNER))
                .orElse(null);
        if (owner == null) {
            System.out.println("[WARN] Không tìm thấy Owner. Hãy kiểm tra OwnerAccountInitializer.");
        } else {
            System.out.println("[INFO] Owner chính: id=" + owner.getId() + ", username=" + owner.getUsername());
        }
    }
}