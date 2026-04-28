package service;

import entity.AppUser;
import entity.Supplier;
import org.springframework.stereotype.Service;
import repository.SupplierRepository;

import java.util.List;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final AuthService authService;

    public SupplierService(SupplierRepository supplierRepository, AuthService authService) {
        this.supplierRepository = supplierRepository;
        this.authService = authService;
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findByUserOrderByIdDesc(authService.getCurrentUser());
    }

    public Supplier getSupplierById(Long id) {
        AppUser user = authService.getCurrentUser();
        return supplierRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà cung cấp"));
    }

    public Supplier saveSupplier(Supplier supplier) {
        if (supplier.getUser() == null) supplier.setUser(authService.getCurrentUser());
        return supplierRepository.save(supplier);
    }

    public void deleteSupplier(Long id) {
        Supplier supplier = getSupplierById(id);
        supplierRepository.delete(supplier);
    }
}
