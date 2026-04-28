package service;

import entity.AppUser;
import entity.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.SupplierRepository;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final AuthService authService;

    public SupplierService(SupplierRepository supplierRepository,
                           AuthService authService) {
        this.supplierRepository = supplierRepository;
        this.authService = authService;
    }

    public List<Supplier> getAllSuppliers() {
        AppUser owner = authService.getWorkspaceOwner();
        return supplierRepository.findByUserOrderByIdDesc(owner);
    }

    public Supplier getSupplierById(Long id) {
        AppUser owner = authService.getWorkspaceOwner();

        return supplierRepository.findByIdAndUser(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà cung cấp trong web của Owner này"));
    }

    @Transactional
    public Supplier saveSupplier(Supplier supplier) {
        AppUser owner = authService.getWorkspaceOwner();

        supplier.setUser(owner);

        return supplierRepository.save(supplier);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = getSupplierById(id);
        supplierRepository.delete(supplier);
    }
}