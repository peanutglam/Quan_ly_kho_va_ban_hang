package config;

import entity.AppUser;
import entity.Order;
import entity.Product;
import entity.StockImport;
import entity.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import repository.OrderRepository;
import repository.ProductRepository;
import repository.StockImportRepository;
import repository.SupplierRepository;
import repository.UserRepository;

import java.util.ArrayList;
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
        AppUser owner = findCanonicalOwner();

        if (owner == null) {
            return;
        }

        owner = normalizeOwner(owner);

        repairProducts(owner);
        repairOrders(owner);
        repairSuppliers(owner);
        repairStockImports(owner);
        repairEmployees(owner);
        disableExtraOwners(owner);

        System.out.println("Đã đồng bộ dữ liệu về Owner chính: id=" + owner.getId()
                + ", username=" + owner.getUsername());
    }

    private AppUser findCanonicalOwner() {
        String username = ownerUsername == null
                ? "owner"
                : ownerUsername.trim().toLowerCase(Locale.ROOT);

        return userRepository.findFirstByUsernameOrderByIdAsc(username)
                .or(() -> userRepository.findFirstByUsernameAndActiveTrueOrderByIdAsc(username))
                .or(() -> userRepository.findFirstByRoleAndActiveTrueOrderByIdAsc(AppUser.ROLE_OWNER))
                .or(() -> userRepository.findFirstByRoleOrderByIdAsc(AppUser.ROLE_OWNER))
                .orElse(null);
    }

    private AppUser normalizeOwner(AppUser owner) {
        boolean changed = false;

        if (!AppUser.ROLE_OWNER.equals(normalizeRole(owner.getRole()))) {
            owner.setRole(AppUser.ROLE_OWNER);
            changed = true;
        }

        if (owner.getOwner() != null) {
            owner.setOwner(null);
            changed = true;
        }

        if (Boolean.FALSE.equals(owner.getActive())) {
            owner.setActive(true);
            changed = true;
        }

        if (changed) {
            return userRepository.save(owner);
        }

        return owner;
    }

    private void repairProducts(AppUser owner) {
        List<Product> changedProducts = new ArrayList<>();

        for (Product product : productRepository.findAll()) {
            if (!sameUser(product.getUser(), owner)) {
                product.setUser(owner);
                changedProducts.add(product);
            }
        }

        if (!changedProducts.isEmpty()) {
            productRepository.saveAll(changedProducts);
        }
    }

    private void repairOrders(AppUser owner) {
        List<Order> changedOrders = new ArrayList<>();

        for (Order order : orderRepository.findAll()) {
            if (!sameUser(order.getUser(), owner)) {
                order.setUser(owner);
                changedOrders.add(order);
            }
        }

        if (!changedOrders.isEmpty()) {
            orderRepository.saveAll(changedOrders);
        }
    }

    private void repairSuppliers(AppUser owner) {
        List<Supplier> changedSuppliers = new ArrayList<>();

        for (Supplier supplier : supplierRepository.findAll()) {
            if (!sameUser(supplier.getUser(), owner)) {
                supplier.setUser(owner);
                changedSuppliers.add(supplier);
            }
        }

        if (!changedSuppliers.isEmpty()) {
            supplierRepository.saveAll(changedSuppliers);
        }
    }

    private void repairStockImports(AppUser owner) {
        List<StockImport> changedImports = new ArrayList<>();

        for (StockImport stockImport : stockImportRepository.findAll()) {
            if (!sameUser(stockImport.getUser(), owner)) {
                stockImport.setUser(owner);
                changedImports.add(stockImport);
            }
        }

        if (!changedImports.isEmpty()) {
            stockImportRepository.saveAll(changedImports);
        }
    }

    private void repairEmployees(AppUser owner) {
        List<AppUser> changedUsers = new ArrayList<>();

        for (AppUser user : userRepository.findAll()) {
            if (user.getId() == null || user.getId().equals(owner.getId())) {
                continue;
            }

            String role = normalizeRole(user.getRole());

            if (!AppUser.ROLE_OWNER.equals(role) && !sameUser(user.getOwner(), owner)) {
                user.setOwner(owner);
                changedUsers.add(user);
            }
        }

        if (!changedUsers.isEmpty()) {
            userRepository.saveAll(changedUsers);
        }
    }

    private void disableExtraOwners(AppUser owner) {
        List<AppUser> changedUsers = new ArrayList<>();

        for (AppUser otherOwner : userRepository.findByRoleAndActiveTrueOrderByIdAsc(AppUser.ROLE_OWNER)) {
            if (otherOwner.getId() != null
                    && !otherOwner.getId().equals(owner.getId())
                    && Boolean.TRUE.equals(otherOwner.getActive())) {
                otherOwner.setActive(false);
                changedUsers.add(otherOwner);
            }
        }

        if (!changedUsers.isEmpty()) {
            userRepository.saveAll(changedUsers);
        }
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

    private boolean sameUser(AppUser a, AppUser b) {
        return a != null
                && b != null
                && a.getId() != null
                && b.getId() != null
                && a.getId().equals(b.getId());
    }
}