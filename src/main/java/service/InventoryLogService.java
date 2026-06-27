package service;

import entity.AppUser;
import entity.InventoryLog;
import entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import repository.InventoryLogRepository;

@Service
public class InventoryLogService {

    private final InventoryLogRepository inventoryLogRepository;
    private final AuthService authService;

    public InventoryLogService(InventoryLogRepository inventoryLogRepository,
                               AuthService authService) {
        this.inventoryLogRepository = inventoryLogRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public Page<InventoryLog> getLogs(int page, int size) {
        authService.requireRole("OWNER", "STAFF", "SALE");
        AppUser owner = authService.getWorkspaceOwner();
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 30 : Math.min(size, 100);
        return inventoryLogRepository.findByUserOrderByCreatedAtDescIdDesc(owner, PageRequest.of(safePage, safeSize));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void log(AppUser owner,
                    AppUser actor,
                    Product product,
                    String actionType,
                    Integer beforeQuantity,
                    Integer afterQuantity,
                    String referenceType,
                    Long referenceId,
                    String description) {
        if (owner == null || actionType == null || actionType.isBlank()) {
            return;
        }

        int before = beforeQuantity == null ? 0 : beforeQuantity;
        int after = afterQuantity == null ? before : afterQuantity;

        InventoryLog log = new InventoryLog();
        log.setUser(owner);
        log.setActor(actor);
        log.setProduct(product);
        log.setActionType(actionType);
        log.setBeforeQuantity(before);
        log.setAfterQuantity(after);
        log.setQuantityChange(after - before);
        log.setReferenceType(referenceType);
        log.setReferenceId(referenceId);
        log.setDescription(description);

        inventoryLogRepository.save(log);
    }
}