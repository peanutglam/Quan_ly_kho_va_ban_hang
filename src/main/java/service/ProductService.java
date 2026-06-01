package service;

import entity.AppUser;
import entity.Product;
import org.springframework.data.domain.Page;
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

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        AppUser owner = authService.getWorkspaceOwner();
        return productRepository.findByUserAndActiveTrueOrderByIdDesc(owner);
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts(String keyword, AppUser user) {
        AppUser owner = workspaceOwner(user);

        if (StringUtils.hasText(keyword)) {
            return productRepository.searchByUserAndKeyword(owner, keyword.trim());
        }

        return productRepository.findByUserAndActiveTrueOrderByIdDesc(owner);
    }

    @Transactional(readOnly = true)
    public List<Product> filterProducts(AppUser user,
                                        String keyword,
                                        String stockStatus,
                                        String expiryStatus) {
        AppUser owner = workspaceOwner(user);

        List<Product> products = getAllProducts(keyword, owner);

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
        AppUser owner = workspaceOwner(user);

        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 30 : Math.min(size, 30);

        Pageable pageable = PageRequest.of(safePage, safeSize);

        String kw = StringUtils.hasText(keyword) ? keyword.trim() : "";
        String stock = StringUtils.hasText(stockStatus) ? stockStatus.trim() : "";
        String expiry = StringUtils.hasText(expiryStatus) ? expiryStatus.trim() : "";

        LocalDate today = LocalDate.now();

        return productRepository.filterProductsPaged(
                owner,
                kw,
                stock,
                expiry,
                today,
                today.plusDays(30),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Product getById(Long id, AppUser user) {
        AppUser owner = workspaceOwner(user);

        return productRepository.findByIdAndUserAndActiveTrue(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong web của Owner này"));
    }

    @Transactional(readOnly = true)
    public Product getPublicProductById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Sản phẩm không hợp lệ");
        }

        return productRepository.findByIdAndActiveTrueAndQuantityGreaterThan(id, 0)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại hoặc đã hết hàng"));
    }

    @Transactional
    public Product create(Product product, AppUser user) {
        AppUser owner = workspaceOwner(user);

        validateProduct(product);

        if (productRepository.existsByCodeAndUserAndActiveTrue(product.getCode(), owner)) {
            throw new IllegalArgumentException("Mã sản phẩm đã tồn tại trong web của Owner này");
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

        if (productRepository.existsByCodeAndUserAndActiveTrueAndIdNot(updatedProduct.getCode(), owner, id)) {
            throw new IllegalArgumentException("Mã sản phẩm đã tồn tại trong web của Owner này");
        }

        existing.setCode(updatedProduct.getCode());
        existing.setName(updatedProduct.getName());
        existing.setCategory(updatedProduct.getCategory());
        existing.setBrand(updatedProduct.getBrand());
        existing.setImageUrl(updatedProduct.getImageUrl());
        existing.setPromotionEnabled(updatedProduct.getPromotionEnabled());
        existing.setPromotionPercent(updatedProduct.getPromotionPercent());
        existing.setPromotionPrice(updatedProduct.getPromotionPrice());
        existing.setPromotionStartDate(updatedProduct.getPromotionStartDate());
        existing.setPromotionEndDate(updatedProduct.getPromotionEndDate());
        existing.setTotalQuantity(updatedProduct.getTotalQuantity());
        existing.setImportPrice(updatedProduct.getImportPrice());
        existing.setSalePrice(updatedProduct.getSalePrice());
        existing.setExpiryDate(updatedProduct.getExpiryDate());
        existing.setSupplier(updatedProduct.getSupplier());
        existing.setDescription(updatedProduct.getDescription());

        existing.recalculateInventoryFields();

        return productRepository.save(existing);
    }

    @Transactional
    public String delete(Long id, AppUser user) {
        AppUser owner = workspaceOwner(user);

        Product product = getById(id, owner);

        product.setActive(false);
        productRepository.save(product);

        return "Đã ẩn sản phẩm. Đơn hàng và phiếu nhập cũ vẫn được giữ an toàn.";
    }

    @Transactional
    public void deleteAll(AppUser user) {
        AppUser owner = workspaceOwner(user);

        List<Product> all = productRepository.findByUserAndActiveTrue(owner);

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

    @Transactional
    public void synchronizeProductStatistics(AppUser user) {
        AppUser owner = workspaceOwner(user);

        List<Product> products = productRepository.findByUser(owner);

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

            product.setSoldQuantity(finalSold);
            product.setTotalQuantity(finalTotal);
            product.recalculateInventoryFields();
        }

        productRepository.saveAll(products);
    }

    @Transactional(readOnly = true)
    public long countProducts(AppUser user) {
        AppUser owner = workspaceOwner(user);
        return productRepository.countByUserAndActiveTrue(owner);
    }

    @Transactional(readOnly = true)
    public List<Product> getExpiringProducts(AppUser user) {
        AppUser owner = workspaceOwner(user);
        LocalDate today = LocalDate.now();

        return productRepository.findTop20ByUserAndActiveTrueAndExpiryDateBetweenOrderByExpiryDateAscIdDesc(
                owner,
                today,
                today.plusDays(30)
        );
    }

    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts(AppUser user) {
        AppUser owner = workspaceOwner(user);

        return productRepository.findTop20ByUserAndActiveTrueAndQuantityGreaterThanAndQuantityLessThanEqualOrderByQuantityAscIdDesc(
                owner,
                0,
                5
        );
    }

    @Transactional(readOnly = true)
    public List<Product> getTopLowStock(AppUser user) {
        AppUser owner = workspaceOwner(user);
        return productRepository.findTop5ByUserAndActiveTrueOrderByQuantityAsc(owner);
    }

    @Transactional(readOnly = true)
    public List<Product> getPublicProducts(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return productRepository.findAllByActiveTrueAndQuantityGreaterThanOrderByQuantityDescIdDesc(0);
        }

        return productRepository.searchPublicProducts(keyword.trim());
    }

    @Transactional(readOnly = true)
    public Page<Product> getPublicProductsPage(String keyword,
                                               String category,
                                               boolean saleOnly,
                                               int page,
                                               int size) {
        int safePage = Math.max(page, 0);
        int safeSize = 20;

        String kw = StringUtils.hasText(keyword) ? keyword.trim() : "";
        String safeCategory = StringUtils.hasText(category) ? category.trim() : "";

        return productRepository.searchPublicProductsPaged(
                kw,
                safeCategory,
                saleOnly,
                LocalDate.now(),
                PageRequest.of(safePage, safeSize)
        );
    }

    @Transactional(readOnly = true)
    public List<String> getPublicCategories() {
        return productRepository.findPublicCategories();
    }

    @Transactional(readOnly = true)
    public List<Product> getPublicSaleProducts(int limit) {
        int safeLimit = limit <= 0 ? 4 : Math.min(limit, 8);
        return productRepository.findActivePromotionProducts(LocalDate.now(), PageRequest.of(0, safeLimit));
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