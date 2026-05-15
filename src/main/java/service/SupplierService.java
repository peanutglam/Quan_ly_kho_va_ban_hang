package service;

import entity.AppUser;
import entity.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.SupplierRepository;

import java.util.Comparator;
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

    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Supplier::getId, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public long countSuppliers() {
        return supplierRepository.count();
    }

    @Transactional(readOnly = true)
    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhà cung cấp"));
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