package service;

import entity.AppUser;
import entity.Product;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import repository.OrderItemRepository;
import repository.ProductRepository;
import repository.StockImportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

    /**
     * Lấy danh sách sản phẩm - KHÔNG chạy sync để tránh performance issue.
     */
    public List<Product> getAllProducts(String keyword, AppUser user) {
        user = workspaceOwner(user);
        if (!StringUtils.hasText(keyword)) {
            return productRepository.findByUserAndActiveTrueOrderByIdDesc(user);
        }
        return productRepository.searchByUserAndKeyword(user, keyword.trim());
    }
    public Page<Product> filterProductsPage(AppUser user,
                                            String keyword,
                                            String stockStatus,
                                            String expiryStatus,
                                            int page,
                                            int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 30 : Math.min(size, 30);

        List<Product> filteredProducts = filterProducts(user, keyword, stockStatus, expiryStatus);

        /*
         * Đẩy sản phẩm hết hàng xuống cuối.
         * Sản phẩm còn hàng lên trước.
         */
        filteredProducts = filteredProducts.stream()
                .sorted(
                        java.util.Comparator
                                .comparing((Product p) -> p.getQuantity() == null || p.getQuantity() <= 0)
                                .thenComparing(
                                        p -> p.getId() == null ? 0L : p.getId(),
                                        java.util.Comparator.reverseOrder()
                                )
                )
                .toList();

        int start = safePage * safeSize;
        Pageable pageable = PageRequest.of(safePage, safeSize);

        if (start >= filteredProducts.size()) {
            return new PageImpl<>(List.of(), pageable, filteredProducts.size());
        }

        int end = Math.min(start + safeSize, filteredProducts.size());

        return new PageImpl<>(
                filteredProducts.subList(start, end),
                pageable,
                filteredProducts.size()
        );
    }
    public List<Product> filterProducts(AppUser user, String keyword, String stockStatus, String expiryStatus) {
        user = workspaceOwner(user);
        List<Product> products = getAllProducts(keyword, user);

        if ("OUT_OF_STOCK".equals(stockStatus)) {
            products = products.stream().filter(p -> p.getQuantity() == 0).toList();
        } else if ("LOW_STOCK".equals(stockStatus)) {
            products = products.stream().filter(p -> p.getQuantity() > 0 && p.getQuantity() <= 5).toList();
        } else if ("AVAILABLE".equals(stockStatus)) {
            products = products.stream().filter(p -> p.getQuantity() > 5).toList();
        }

        LocalDate today = LocalDate.now();
        if ("EXPIRED".equals(expiryStatus)) {
            products = products.stream()
                    .filter(p -> p.getExpiryDate() != null && p.getExpiryDate().isBefore(today)).toList();
        } else if ("EXPIRING_SOON".equals(expiryStatus)) {
            products = products.stream()
                    .filter(p -> p.getExpiryDate() != null
                            && !p.getExpiryDate().isBefore(today)
                            && !p.getExpiryDate().isAfter(today.plusDays(30))).toList();
        }
        return products;
    }

    public Product getById(Long id, AppUser user) {
        user = workspaceOwner(user);
        return productRepository.findByIdAndUserAndActiveTrue(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
    }

    @Transactional
    public Product create(Product product, AppUser user) {
        user = workspaceOwner(user);
        validateProduct(product);
        if (productRepository.existsByCodeAndUserAndActiveTrue(product.getCode(), user)) {
            throw new IllegalArgumentException("Mã sản phẩm đã tồn tại");
        }
        product.setUser(user);
        product.setActive(true);
        if (product.getTotalQuantity() == 0 && product.getQuantity() > 0) {
            product.setTotalQuantity(product.getQuantity() + product.getSoldQuantity());
        }
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, Product updated, AppUser user) {
        user = workspaceOwner(user);
        validateProduct(updated);
        Product existing = getById(id, user);
        if (productRepository.existsByCodeAndUserAndActiveTrueAndIdNot(updated.getCode(), user, id)) {
            throw new IllegalArgumentException("Mã sản phẩm đã tồn tại");
        }
        existing.setCode(updated.getCode());
        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setBrand(updated.getBrand());
        existing.setImportPrice(updated.getImportPrice());
        existing.setSalePrice(updated.getSalePrice());
        existing.setExpiryDate(updated.getExpiryDate());
        existing.setSupplier(updated.getSupplier());
        existing.setDescription(updated.getDescription());
        int newTotal = updated.getTotalQuantity();
        if (newTotal > 0 && newTotal >= existing.getSoldQuantity()) {
            existing.setTotalQuantity(newTotal);
        }
        return productRepository.save(existing);
    }

    @Transactional
    public String delete(Long id, AppUser user) {
        user = workspaceOwner(user);
        Product product = getById(id, user);
        product.setActive(false);
        productRepository.save(product);
        return "Đã ẩn sản phẩm. Lịch sử đơn hàng và phiếu nhập vẫn được giữ.";
    }

    @Transactional
    public void deleteAll(AppUser user) {
        user = workspaceOwner(user);
        List<Product> all = productRepository.findByUserAndActiveTrue(user);
        all.forEach(p -> p.setActive(false));
        productRepository.saveAll(all);
    }

    @Transactional
    public void increaseStock(Product product, int amount, BigDecimal importPrice, LocalDate expiryDate) {
        if (amount <= 0) throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
        product.increaseStock(amount);
        if (importPrice != null && importPrice.signum() > 0) product.setImportPrice(importPrice);
        if (expiryDate != null) product.setExpiryDate(expiryDate);
        productRepository.save(product);
    }

    @Transactional
    public void decreaseStockForSale(Product product, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Số lượng bán phải lớn hơn 0");
        if (product.getQuantity() < amount) {
            throw new IllegalArgumentException(
                    "Sản phẩm '" + product.getName() + "' không đủ tồn kho, hiện còn " + product.getQuantity());
        }
        product.registerSale(amount);
        productRepository.save(product);
    }

    @Transactional
    public void restoreStockFromSale(Product product, int amount) {
        if (product == null || amount <= 0) return;
        product.restoreSale(amount);
        productRepository.save(product);
    }

    public long countProducts(AppUser user) {
        return productRepository.countByUserAndActiveTrue(workspaceOwner(user));
    }

    public List<Product> getExpiringProducts(AppUser user) {
        user = workspaceOwner(user);
        LocalDate today = LocalDate.now();
        return productRepository.findByUserAndActiveTrueAndExpiryDateBetween(user, today, today.plusDays(30));
    }

    public List<Product> getLowStockProducts(AppUser user) {
        user = workspaceOwner(user);
        return productRepository.findByUserAndActiveTrueAndQuantityLessThanEqual(user, 5)
                .stream().filter(p -> p.getQuantity() > 0).toList();
    }

    public List<Product> getTopLowStock(AppUser user) {
        user = workspaceOwner(user);
        return productRepository.findTop5ByUserAndActiveTrueOrderByQuantityAsc(user);
    }

    public Map<Long, Long> getSoldQtyMap(AppUser user) {
        user = workspaceOwner(user);
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : orderItemRepository.findSoldQtyPerProduct(user)) {
            map.put((Long) row[0], row[1] == null ? 0L : ((Number) row[1]).longValue());
        }
        return map;
    }

    public Map<Long, Long> getTotalImportedMap(AppUser user) {
        user = workspaceOwner(user);
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : stockImportRepository.findTotalImportedPerProduct(user)) {
            map.put((Long) row[0], row[1] == null ? 0L : ((Number) row[1]).longValue());
        }
        return map;
    }

    /** Chỉ gọi khi cần thiết (từ trang admin), không gọi ngầm. */
    @Transactional
    public int synchronizeProductStatistics(AppUser user) {
        user = workspaceOwner(user);
        List<Product> products = productRepository.findByUser(user);
        if (products.isEmpty()) return 0;
        Map<Long, Long> soldMap     = getSoldQtyMap(user);
        Map<Long, Long> importedMap = getTotalImportedMap(user);
        for (Product p : products) {
            int finalSold  = (int) Math.min(soldMap.getOrDefault(p.getId(), (long) p.getSoldQuantity()), Integer.MAX_VALUE);
            int finalTotal = (int) Math.min(importedMap.getOrDefault(p.getId(), (long) p.getTotalQuantity()), Integer.MAX_VALUE);
            finalTotal = Math.max(finalTotal, p.getQuantity() + finalSold);
            p.setSoldQuantity(finalSold);
            p.setTotalQuantity(Math.max(finalTotal, finalSold));
        }
        productRepository.saveAll(products);
        return products.size();
    }

    // Public products for shop (không cần auth)
    // Public products for shop (không cần auth)
    @Transactional(readOnly = true)
    public List<Product> getPublicProducts(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return productRepository
                    .findAllByActiveTrueAndQuantityGreaterThanOrderByQuantityDescIdDesc(0);
        }

        return productRepository.searchPublicProducts(keyword.trim());
    }
    @Transactional(readOnly = true)
    public Product getPublicProductById(Long id) {
        return productRepository.findById(id)
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .filter(p -> p.getQuantity() != null && p.getQuantity() > 0)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại hoặc đã hết hàng"));
    }
    private AppUser workspaceOwner(AppUser user) {
        if (user == null) return authService.getWorkspaceOwner();
        return authService.getWorkspaceOwner(user);
    }

    private void validateProduct(Product p) {
        if (p == null) throw new IllegalArgumentException("Dữ liệu sản phẩm không hợp lệ");
        if (!StringUtils.hasText(p.getCode())) throw new IllegalArgumentException("Mã sản phẩm không được để trống");
        if (!StringUtils.hasText(p.getName())) throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        if (p.getImportPrice() != null && p.getImportPrice().signum() < 0) throw new IllegalArgumentException("Giá nhập không hợp lệ");
        if (p.getSalePrice() != null && p.getSalePrice().signum() < 0) throw new IllegalArgumentException("Giá bán không hợp lệ");
    }
}