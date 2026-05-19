package config;

import entity.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import repository.OrderRepository;
import repository.ProductRepository;
import repository.StockImportRepository;
import repository.SupplierRepository;
import repository.UserRepository;

import java.util.List;
import java.util.Locale;

@Component
@org.springframework.core.annotation.Order(2)
public class DataOwnershipRepairRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final StockImportRepository stockImportRepository;

    @Value("${app.owner.username:owner}")
    private String ownerUsername;

    /*
     * Mặc định vẫn bật để giữ đúng logic cũ:
     * dữ liệu luôn được đưa về Owner chính.
     *
     * Nếu sau này dữ liệu đã ổn định và muốn app start nhanh hơn nữa,
     * có thể thêm vào application.properties:
     * app.data-repair.enabled=false
     */
    @Value("${app.data-repair.enabled:true}")
    private boolean dataRepairEnabled;

    public DataOwnershipRepairRunner(UserRepository userRepository,
                                     ProductRepository productRepository,
                                     OrderRepository orderRepository,
                                     SupplierRepository supplierRepository,
                                     StockImportRepository stockImportRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.supplierRepository = supplierRepository;
        this.stockImportRepository = stockImportRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!dataRepairEnabled) {
            System.out.println("DataOwnershipRepairRunner: đã tắt bằng app.data-repair.enabled=false");
            return;
        }

        AppUser owner = findCanonicalOwner();

        if (owner == null || owner.getId() == null) {
            System.out.println("DataOwnershipRepairRunner: không tìm thấy Owner chính");
            return;
        }

        int productsUpdated = productRepository.repairOwner(owner);
        int ordersUpdated = orderRepository.repairOwner(owner);
        int suppliersUpdated = supplierRepository.repairOwner(owner);
        int importsUpdated = stockImportRepository.repairOwner(owner);

        int employeesUpdated = repairEmployees(owner);
        int ownersDisabled = disableExtraOwners(owner);

        System.out.println(
                "Đã đồng bộ dữ liệu về Owner chính: id=" + owner.getId()
                        + ", username=" + owner.getUsername()
                        + " | products=" + productsUpdated
                        + ", orders=" + ordersUpdated
                        + ", suppliers=" + suppliersUpdated
                        + ", stockImports=" + importsUpdated
                        + ", employees=" + employeesUpdated
                        + ", extraOwnersDisabled=" + ownersDisabled
        );
    }

    private AppUser findCanonicalOwner() {
        String username = ownerUsername == null
                ? "owner"
                : ownerUsername.trim().toLowerCase(Locale.ROOT);

        return userRepository.findFirstByUsernameAndActiveTrueOrderByIdAsc(username)
                .or(() -> userRepository.findFirstByRoleAndActiveTrueOrderByIdAsc(AppUser.ROLE_OWNER))
                .orElse(null);
    }

    private int repairEmployees(AppUser owner) {
        List<AppUser> users = userRepository.findAll();
        int changed = 0;

        for (AppUser user : users) {
            if (user == null || user.getId() == null || user.getId().equals(owner.getId())) {
                continue;
            }

            String role = normalizeRole(user.getRole());

            if (!AppUser.ROLE_OWNER.equals(role)) {
                if (user.getOwner() == null
                        || user.getOwner().getId() == null
                        || !user.getOwner().getId().equals(owner.getId())) {
                    user.setOwner(owner);
                    changed++;
                }
            }
        }

        if (changed > 0) {
            userRepository.saveAll(users);
        }

        return changed;
    }

    private int disableExtraOwners(AppUser owner) {
        List<AppUser> owners = userRepository.findByRoleAndActiveTrueOrderByIdAsc(AppUser.ROLE_OWNER);
        int changed = 0;

        for (AppUser otherOwner : owners) {
            if (otherOwner.getId() != null && !otherOwner.getId().equals(owner.getId())) {
                otherOwner.setActive(false);
                changed++;
            }
        }

        if (changed > 0) {
            userRepository.saveAll(owners);
        }

        return changed;
    }

    private String normalizeRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return AppUser.ROLE_OWNER;
        }

        String value = rawRole.trim().toUpperCase(Locale.ROOT);

        if (value.startsWith("ROLE_")) {
            value = value.substring(5);
        }

        if ("ADMIN".equals(value)) {
            return AppUser.ROLE_OWNER;
        }

        return value;
    }
}