package service;

import entity.AppUser;
import entity.InventoryCheck;
import entity.InventoryLog;
import entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.InventoryCheckRepository;
import repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryCheckService {

    private final InventoryCheckRepository inventoryCheckRepository;
    private final ProductRepository productRepository;
    private final InventoryLogService inventoryLogService;
    private final AuthService authService;

    public InventoryCheckService(InventoryCheckRepository inventoryCheckRepository,
                                 ProductRepository productRepository,
                                 InventoryLogService inventoryLogService,
                                 AuthService authService) {
        this.inventoryCheckRepository = inventoryCheckRepository;
        this.productRepository = productRepository;
        this.inventoryLogService = inventoryLogService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public Page<InventoryCheck> getChecks(int page, int size) {
        authService.requireRole("OWNER", "STAFF");
        AppUser owner = authService.getWorkspaceOwner();
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        return inventoryCheckRepository.findByUserOrderByCheckedAtDescIdDesc(owner, PageRequest.of(safePage, safeSize));
    }

    @Transactional
    public int performInventoryCheck(List<Long> productIds,
                                     List<Integer> actualQuantities,
                                     List<String> reasons,
                                     List<String> notes) {
        authService.requireRole("OWNER", "STAFF");
        AppUser actor = authService.getCurrentUser();
        AppUser owner = authService.getWorkspaceOwner();

        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Không có sản phẩm để kiểm kê");
        }

        List<InventoryCheck> checks = new ArrayList<>();

        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            if (productId == null) {
                continue;
            }

            Integer actual = getInt(actualQuantities, i);
            if (actual == null) {
                continue;
            }
            if (actual < 0) {
                throw new IllegalArgumentException("Số lượng thực tế không được âm");
            }

            Product product = productRepository.findByIdAndUserAndActiveTrue(productId, owner)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm cần kiểm kê"));

            int systemQuantity = product.getQuantity() == null ? 0 : product.getQuantity();
            int difference = actual - systemQuantity;

            InventoryCheck check = new InventoryCheck();
            check.setUser(owner);
            check.setActor(actor);
            check.setProduct(product);
            check.setSystemQuantity(systemQuantity);
            check.setActualQuantity(actual);
            check.setDifferenceQuantity(difference);
            check.setReason(getString(reasons, i));
            check.setNote(getString(notes, i));
            checks.add(inventoryCheckRepository.save(check));

            if (difference != 0) {
                // Product.recalculateInventoryFields() tính quantity = totalQuantity - soldQuantity.
                // Vì vậy khi kiểm kê theo số lượng thực tế, cần cập nhật lại totalQuantity = thực tế + đã bán.
                int soldQuantity = product.getSoldQuantity() == null ? 0 : product.getSoldQuantity();
                product.setTotalQuantity(actual + soldQuantity);
                product.setQuantity(actual);
                product.recalculateInventoryFields();
                productRepository.save(product);
            }

            inventoryLogService.log(
                    owner,
                    actor,
                    product,
                    InventoryLog.ACTION_INVENTORY_CHECK,
                    systemQuantity,
                    actual,
                    "INVENTORY_CHECK",
                    check.getId(),
                    "Kiểm kê kho: " + product.getName()
                            + " | hệ thống " + systemQuantity
                            + " | thực tế " + actual
                            + " | chênh lệch " + difference
            );
        }

        if (checks.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập ít nhất một dòng kiểm kê hợp lệ");
        }

        return checks.size();
    }

    private Integer getInt(List<Integer> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return null;
        }
        return values.get(index);
    }

    private String getString(List<String> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return null;
        }
        String value = values.get(index);
        return value == null || value.isBlank() ? null : value.trim();
    }
}