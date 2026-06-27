package service;

import entity.AppUser;
import entity.Product;
import entity.StockImport;
import entity.InventoryLog;
import entity.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.StockImportRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class StockImportService {

    private final StockImportRepository stockImportRepository;
    private final ProductService productService;
    private final SupplierService supplierService;
    private final AuthService authService;
    private final InventoryLogService inventoryLogService;

    public StockImportService(StockImportRepository stockImportRepository,
                              ProductService productService,
                              SupplierService supplierService,
                              AuthService authService,
                              InventoryLogService inventoryLogService) {
        this.stockImportRepository = stockImportRepository;
        this.productService = productService;
        this.supplierService = supplierService;
        this.authService = authService;
        this.inventoryLogService = inventoryLogService;
    }

    @Transactional(readOnly = true)
    public List<StockImport> getAllImports() {
        AppUser owner = authService.getWorkspaceOwner();
        return stockImportRepository.findByUserOrderByIdDesc(owner);
    }

    public long countImports() {
        AppUser owner = authService.getWorkspaceOwner();
        return stockImportRepository.countByUser(owner);
    }

    @Transactional
    public void createImport(Long productId, Long supplierId, Integer quantity,
                             BigDecimal importPrice, LocalDate expiryDate, String note) {
        AppUser owner = authService.getWorkspaceOwner();
        if (productId == null)  throw new IllegalArgumentException("Vui lòng chọn sản phẩm");
        if (supplierId == null) throw new IllegalArgumentException("Vui lòng chọn nhà cung cấp");
        if (quantity == null || quantity <= 0) throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
        if (importPrice == null || importPrice.signum() < 0) throw new IllegalArgumentException("Giá nhập không hợp lệ");

        Product product   = productService.getById(productId, owner);
        Supplier supplier = supplierService.getSupplierById(supplierId);

        StockImport si = new StockImport();
        si.setProduct(product);
        si.setSupplier(supplier);
        si.setQuantity(quantity);
        si.setImportPrice(importPrice);
        si.setExpiryDate(expiryDate);
        si.setNote(note);
        si.setUser(owner);
        StockImport savedImport = stockImportRepository.save(si);

        ProductService.StockChangeResult stockChange =
                productService.increaseStock(product, quantity, importPrice, expiryDate);

        inventoryLogService.log(
                owner,
                authService.getCurrentUser(),
                stockChange.getProduct(),
                InventoryLog.ACTION_IMPORT,
                stockChange.getBeforeQuantity(),
                stockChange.getAfterQuantity(),
                "STOCK_IMPORT",
                savedImport.getId(),
                "Nhập hàng: " + stockChange.getProduct().getName() + " +" + quantity
        );
    }

    public StockImport getById(Long id) {
        return stockImportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu nhập #" + id));
    }
}