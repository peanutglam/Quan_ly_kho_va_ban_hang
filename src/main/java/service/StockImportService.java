package service;

import entity.AppUser;
import entity.Product;
import entity.StockImport;
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

    public StockImportService(StockImportRepository stockImportRepository,
                              ProductService productService,
                              SupplierService supplierService,
                              AuthService authService) {
        this.stockImportRepository = stockImportRepository;
        this.productService = productService;
        this.supplierService = supplierService;
        this.authService = authService;
    }

    public List<StockImport> getAllImports() {
        return stockImportRepository.findByUserOrderByIdDesc(authService.getCurrentUser());
    }

    @Transactional
    public void createImport(Long productId, Long supplierId, Integer quantity, BigDecimal importPrice, LocalDate expiryDate, String note) {
        AppUser user = authService.getCurrentUser();
        if (productId == null) throw new IllegalArgumentException("Vui lòng chọn sản phẩm");
        if (supplierId == null) throw new IllegalArgumentException("Vui lòng chọn nhà cung cấp");
        if (quantity == null || quantity <= 0) throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
        if (importPrice == null || importPrice.signum() < 0) throw new IllegalArgumentException("Giá nhập không hợp lệ");

        Product product = productService.getById(productId, user);
        Supplier supplier = supplierService.getSupplierById(supplierId);

        StockImport stockImport = new StockImport();
        stockImport.setProduct(product);
        stockImport.setSupplier(supplier);
        stockImport.setQuantity(quantity);
        stockImport.setImportPrice(importPrice);
        stockImport.setExpiryDate(expiryDate);
        stockImport.setNote(note);
        stockImport.setUser(user);
        stockImportRepository.save(stockImport);

        productService.increaseStock(product, quantity, importPrice, expiryDate);
    }
}
