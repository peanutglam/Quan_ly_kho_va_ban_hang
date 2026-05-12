package service;

import entity.AppUser;
import entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import repository.OrderItemRepository;
import repository.ProductRepository;
import repository.StockImportRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockImportRepository stockImportRepository;
    private final AuthService authService;

    public ProductService(ProductRepository productRepository,
                          OrderItemRepository orderItemRepository,
                          StockImportRepository stockImportRepository,
                          AuthService authService) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.stockImportRepository = stockImportRepository;
        this.authService = authService;
    }

    /*
     * Đề tài hiện tại: 1 ứng dụng = 1 cửa hàng = 1 Owner chính.
     * Vì vậy phần đọc/hiển thị sản phẩm không phụ thuộc user_id nữa.
     * Điều này tránh lỗi logout/login lại web query sai user_id làm tưởng như mất dữ liệu.
     */
    @Transactional(readOnly = true)
    public List<Product> getAllProducts(String keyword, AppUser user) {
        List<Product> products = activeProducts();

        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase();

            products = products.stream()
                    .filter(p ->
                            containsIgnoreCase(p.getCode(), kw)
                                    || containsIgnoreCase(p.getName(), kw)
                                    || containsIgnoreCase(p.getBrand(), kw)
                                    || containsIgnoreCase(p.getCategory(), kw)
                    )
                    .toList();
        }

        return products;
    }

    @Transactional(readOnly = true)
    public List<Product> filterProducts(AppUser user,
                                        String keyword,
                                        String stockStatus,
                                        String expiryStatus) {
        List<Product> products = getAllProducts(keyword, user);

        if ("OUT_OF_STOCK".equals(stockStatus)) {
            products = products.stream()
                    .filter(p -> p.getQuantity() == 0)
                    .toList();
        } else if ("LOW_STOCK".equals(stockStatus)) {
            products = products.stream()
                    .filter(p -> p.getQuantity() > 0 && p.getQuantity() <= 5)
                    .toList();
        } else if ("AVAILABLE".equals(stockStatus)) {
            products = products.stream()
                    .filter(p -> p.getQuantity() > 5)
                    .toList();
        }

        LocalDate today = LocalDate.now();

        if ("EXPIRED".equals(expiryStatus)) {
            products = products.stream()
                    .filter(p -> p.getExpiryDate() != null && p.getExpiryDate().isBefore(today))
                    .toList();
        } else if ("EXPIRING_SOON".equals(expiryStatus)) {
            products = products.stream()
                    .filter(p ->
                            p.getExpiryDate() != null
                                    && !p.getExpiryDate().isBefore(today)
                                    && !p.getExpiryDate().isAfter(today.plusDays(30))
                    )
                    .toList();
        }

        return products;
    }

    @Transactional(readOnly = true)
    public Page<Product> filterProductsPage(AppUser user,
                                            String keyword,
                                            String stockStatus,
                                            String expiryStatus,
                                            int page,
                                            int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 30 : Math.min(size, 30);

        List<Product> filtered = filterProducts(user, keyword, stockStatus, expiryStatus);

        int start = safePage * safeSize;

        if (start >= filtered.size()) {
            Pageable emptyPageable = PageRequest.of(safePage, safeSize);
            return new PageImpl<>(List.of(), emptyPageable, filtered.size());
        }

        int end = Math.min(start + safeSize, filtered.size());

        Pageable pageable = PageRequest.of(safePage, safeSize);

        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public Product getById(Long id, AppUser user) {
        return productRepository.findById(id)
                .filter(this::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
    }

    @Transactional
    public Product create(Product product, AppUser user) {
        AppUser owner = workspaceOwner(user);

        validateProduct(product);

        if (activeProductCodeExists(product.getCode(), null)) {
            throw new IllegalArgumentException("Mã sản phẩm đã tồn tại trong hệ thống");
        }

        product.setUser(owner);
        product.setActive(true);

        if (product.getTotalQuantity() == 0 && product.getQuantity() > 0) {
            product.setTotalQuantity(product.getQuantity());
        }

        product.recalculateInventoryFields();

        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, Product updatedProduct, AppUser user) {
        AppUser owner = workspaceOwner(user);

        validateProduct(updatedProduct);

        Product existing = getById(id, owner);

        if (activeProductCodeExists(updatedProduct.getCode(), id)) {
            throw new IllegalArgumentException("Mã sản phẩm đã tồn tại trong hệ thống");
        }

        existing.setUser(owner);
        existing.setCode(updatedProduct.getCode());
        existing.setName(updatedProduct.getName());
        existing.setCategory(updatedProduct.getCategory());
        existing.setBrand(updatedProduct.getBrand());
        existing.setTotalQuantity(updatedProduct.getTotalQuantity());
        existing.setImportPrice(updatedProduct.getImportPrice());
        existing.setSalePrice(updatedProduct.getSalePrice());
        existing.setExpiryDate(updatedProduct.getExpiryDate());
        existing.setSupplier(updatedProduct.getSupplier());
        existing.setDescription(updatedProduct.getDescription());
        existing.setActive(true);

        existing.recalculateInventoryFields();

        return productRepository.save(existing);
    }

    @Transactional
    public String delete(Long id, AppUser user) {
        Product product = getById(id, user);

        product.setActive(false);
        productRepository.save(product);

        return "Đã ẩn sản phẩm. Đơn hàng và phiếu nhập cũ vẫn được giữ an toàn.";
    }

    @Transactional
    public void deleteAll(AppUser user) {
        List<Product> all = activeProducts();

        all.forEach(product -> product.setActive(false));

        productRepository.saveAll(all);
    }

    @Transactional
    public void increaseStock(Product product,
                              int amount,
                              BigDecimal importPrice,
                              LocalDate expiryDate) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
        }

        product.increaseStock(amount);

        if (importPrice != null) {
            product.setImportPrice(importPrice);
        }

        if (expiryDate != null) {
            product.setExpiryDate(expiryDate);
        }

        productRepository.save(product);
    }

    @Transactional
    public void decreaseStockForSale(Product product, int amount) {
        if (product == null) {
            throw new IllegalArgumentException("Sản phẩm không hợp lệ");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Số lượng bán phải lớn hơn 0");
        }

        if (product.getQuantity() < amount) {
            throw new IllegalArgumentException(
                    "Sản phẩm '" + product.getName() + "' không đủ tồn kho, hiện còn " + product.getQuantity()
            );
        }

        product.registerSale(amount);

        productRepository.save(product);
    }

    @Transactional
    public void restoreStockFromSale(Product product, int amount) {
        if (product == null || amount <= 0) {
            return;
        }

        product.restoreSale(amount);

        productRepository.save(product);
    }

    /*
     * Không gọi tự động ở Dashboard/List vì dễ gây update hàng loạt.
     * Chỉ dùng khi cần chủ động đồng bộ thủ công.
     */
    @Transactional
    public void synchronizeProductStatistics(AppUser user) {
        AppUser owner = workspaceOwner(user);

        List<Product> products = productRepository.findAll();

        if (products.isEmpty()) {
            return;
        }

        Map<Long, Long> soldQtyMap = getSoldQtyMap(owner);
        Map<Long, Long> importedQtyMap = getTotalImportedMap(owner);

        for (Product product : products) {
            long soldFromOrders = soldQtyMap.getOrDefault(product.getId(), 0L);
            long imported = importedQtyMap.getOrDefault(product.getId(), 0L);

            int currentSold = product.getSoldQuantity() == null ? 0 : product.getSoldQuantity();
            int currentTotal = product.getTotalQuantity() == null ? 0 : product.getTotalQuantity();
            int currentStock = product.getQuantity() == null ? 0 : product.getQuantity();

            int finalSold = Math.max(currentSold, safeLongToInt(soldFromOrders));
            int finalTotal = Math.max(currentTotal, currentStock + finalSold);

            if (imported > 0) {
                finalTotal = Math.max(finalTotal, safeLongToInt(imported));
            }

            if (finalSold > finalTotal) {
                finalTotal = finalSold;
            }

            product.setUser(owner);
            product.setSoldQuantity(finalSold);
            product.setTotalQuantity(finalTotal);
            product.recalculateInventoryFields();
        }

        productRepository.saveAll(products);
    }

    @Transactional(readOnly = true)
    public long countProducts(AppUser user) {
        return activeProducts().size();
    }

    @Transactional(readOnly = true)
    public List<Product> getExpiringProducts(AppUser user) {
        LocalDate today = LocalDate.now();

        return activeProducts().stream()
                .filter(p -> p.getExpiryDate() != null)
                .filter(p -> !p.getExpiryDate().isBefore(today))
                .filter(p -> !p.getExpiryDate().isAfter(today.plusDays(30)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts(AppUser user) {
        return activeProducts().stream()
                .filter(p -> p.getQuantity() > 0 && p.getQuantity() <= 5)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Product> getTopLowStock(AppUser user) {
        return activeProducts().stream()
                .sorted(Comparator.comparing(Product::getQuantity))
                .limit(5)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getSoldQtyMap(AppUser user) {
        AppUser owner = workspaceOwner(user);

        Map<Long, Long> map = new HashMap<>();

        List<Object[]> rows = orderItemRepository.findSoldQtyPerProduct(owner);

        for (Object[] row : rows) {
            Long productId = (Long) row[0];
            Long qty = row[1] == null ? 0L : ((Number) row[1]).longValue();

            map.put(productId, qty);
        }

        return map;
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getTotalImportedMap(AppUser user) {
        AppUser owner = workspaceOwner(user);

        Map<Long, Long> map = new HashMap<>();

        List<Object[]> rows = stockImportRepository.findTotalImportedPerProduct(owner);

        for (Object[] row : rows) {
            Long productId = (Long) row[0];
            Long qty = row[1] == null ? 0L : ((Number) row[1]).longValue();

            map.put(productId, qty);
        }

        return map;
    }

    private List<Product> activeProducts() {
        return productRepository.findAll()
                .stream()
                .filter(this::isActive)
                .sorted(Comparator.comparing(Product::getId, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList();
    }

    private boolean isActive(Product product) {
        return product != null && Boolean.TRUE.equals(product.getActive());
    }

    private boolean activeProductCodeExists(String code, Long exceptId) {
        if (!StringUtils.hasText(code)) {
            return false;
        }

        String normalizedCode = code.trim();

        return productRepository.findAll()
                .stream()
                .filter(this::isActive)
                .filter(p -> p.getCode() != null && p.getCode().trim().equalsIgnoreCase(normalizedCode))
                .anyMatch(p -> exceptId == null || !exceptId.equals(p.getId()));
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private AppUser workspaceOwner(AppUser user) {
        if (user == null) {
            return authService.getWorkspaceOwner();
        }

        return authService.getWorkspaceOwner(user);
    }

    private void validateProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Dữ liệu sản phẩm không hợp lệ");
        }

        if (!StringUtils.hasText(product.getCode())) {
            throw new IllegalArgumentException("Mã sản phẩm không được để trống");
        }

        if (!StringUtils.hasText(product.getName())) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }

        if (product.getImportPrice() != null && product.getImportPrice().signum() < 0) {
            throw new IllegalArgumentException("Giá nhập không hợp lệ");
        }

        if (product.getSalePrice() != null && product.getSalePrice().signum() < 0) {
            throw new IllegalArgumentException("Giá bán không hợp lệ");
        }
    }

    private int safeLongToInt(long value) {
        if (value <= 0) {
            return 0;
        }

        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}