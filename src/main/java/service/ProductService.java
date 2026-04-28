package service;

import entity.AppUser;
import entity.Product;
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

    public ProductService(ProductRepository productRepository,
                          OrderItemRepository orderItemRepository,
                          StockImportRepository stockImportRepository) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.stockImportRepository = stockImportRepository;
    }

    @Transactional
    public List<Product> getAllProducts(String keyword, AppUser user) {
        synchronizeProductStatistics(user);
        if (!StringUtils.hasText(keyword)) {
            return productRepository.findByUserAndActiveTrueOrderByIdDesc(user);
        }
        return productRepository.searchByUserAndKeyword(user, keyword.trim());
    }

    @Transactional
    public List<Product> filterProducts(AppUser user, String keyword, String stockStatus, String expiryStatus) {
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
            products = products.stream().filter(p -> p.getExpiryDate() != null && p.getExpiryDate().isBefore(today)).toList();
        } else if ("EXPIRING_SOON".equals(expiryStatus)) {
            products = products.stream().filter(p ->
                    p.getExpiryDate() != null &&
                            !p.getExpiryDate().isBefore(today) &&
                            !p.getExpiryDate().isAfter(today.plusDays(30))).toList();
        }

        return products;
    }

    public Product getById(Long id, AppUser user) {
        return productRepository.findByIdAndUserAndActiveTrue(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm hoặc bạn không có quyền truy cập"));
    }

    @Transactional
    public Product create(Product product, AppUser user) {
        validateProduct(product);
        if (productRepository.existsByCodeAndUserAndActiveTrue(product.getCode(), user)) {
            throw new IllegalArgumentException("Mã sản phẩm đã tồn tại trong tài khoản này");
        }
        product.setUser(user);
        product.setActive(true);
        if (product.getTotalQuantity() == 0 && product.getQuantity() > 0) {
            product.setTotalQuantity(product.getQuantity());
        }
        product.recalculateInventoryFields();
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, Product updatedProduct, AppUser user) {
        validateProduct(updatedProduct);
        Product existing = getById(id, user);
        if (productRepository.existsByCodeAndUserAndActiveTrueAndIdNot(updatedProduct.getCode(), user, id)) {
            throw new IllegalArgumentException("Mã sản phẩm đã tồn tại trong tài khoản này");
        }

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
        List<Product> all = productRepository.findByUserAndActiveTrue(user);
        all.forEach(product -> product.setActive(false));
        productRepository.saveAll(all);
    }

    @Transactional
    public void increaseStock(Product product, int amount, BigDecimal importPrice, LocalDate expiryDate) {
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
            throw new IllegalArgumentException("Sản phẩm '" + product.getName() + "' không đủ tồn kho (còn " + product.getQuantity() + ")");
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

    @Transactional
    public void synchronizeProductStatistics(AppUser user) {
        List<Product> products = productRepository.findByUser(user);
        if (products.isEmpty()) return;

        Map<Long, Long> soldQtyMap = getSoldQtyMap(user);
        Map<Long, Long> importedQtyMap = getTotalImportedMap(user);

        for (Product product : products) {
            long sold = soldQtyMap.getOrDefault(product.getId(), 0L);
            long imported = importedQtyMap.getOrDefault(product.getId(), 0L);

            int currentStock = product.getQuantity();
            int total = Math.max(product.getTotalQuantity(), currentStock + safeLongToInt(sold));
            if (imported > 0) {
                total = Math.max(total, safeLongToInt(imported));
            }
            product.setSoldQuantity(safeLongToInt(sold));
            product.setTotalQuantity(Math.max(total, product.getSoldQuantity()));
            product.recalculateInventoryFields();
        }
        productRepository.saveAll(products);
    }

    public long countProducts(AppUser user) {
        return productRepository.findByUserAndActiveTrue(user).size();
    }

    public List<Product> getExpiringProducts(AppUser user) {
        LocalDate today = LocalDate.now();
        return productRepository.findByUserAndActiveTrueAndExpiryDateBetween(user, today, today.plusDays(30));
    }

    public List<Product> getLowStockProducts(AppUser user) {
        return productRepository.findByUserAndActiveTrueAndQuantityLessThanEqual(user, 5)
                .stream().filter(p -> p.getQuantity() > 0).toList();
    }

    public List<Product> getTopLowStock(AppUser user) {
        return productRepository.findTop5ByUserAndActiveTrueOrderByQuantityAsc(user);
    }

    public Map<Long, Long> getSoldQtyMap(AppUser user) {
        Map<Long, Long> map = new HashMap<>();
        List<Object[]> rows = orderItemRepository.findSoldQtyPerProduct(user);
        for (Object[] row : rows) {
            Long productId = (Long) row[0];
            Long qty = row[1] == null ? 0L : ((Number) row[1]).longValue();
            map.put(productId, qty);
        }
        return map;
    }

    public Map<Long, Long> getTotalImportedMap(AppUser user) {
        Map<Long, Long> map = new HashMap<>();
        List<Object[]> rows = stockImportRepository.findTotalImportedPerProduct(user);
        for (Object[] row : rows) {
            Long productId = (Long) row[0];
            Long qty = row[1] == null ? 0L : ((Number) row[1]).longValue();
            map.put(productId, qty);
        }
        return map;
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
        if (value <= 0) return 0;
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}
