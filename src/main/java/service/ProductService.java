package service;

import entity.AppUser;
import entity.Product;
import entity.ProductImage;
import entity.InventoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import repository.OrderItemRepository;
import repository.ProductRepository;
import repository.StockImportRepository;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockImportRepository stockImportRepository;
    private final AuthService authService;
    private final InventoryLogService inventoryLogService;

    public ProductService(ProductRepository productRepository,
                          OrderItemRepository orderItemRepository,
                          StockImportRepository stockImportRepository,
                          AuthService authService,
                          InventoryLogService inventoryLogService) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.stockImportRepository = stockImportRepository;
        this.authService = authService;
        this.inventoryLogService = inventoryLogService;
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

        Product product = productRepository.findByIdAndUserAndActiveTrue(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong web của Owner này"));

        product.getImages().size();

        return product;
    }

    @Transactional(readOnly = true)
    public Product getPublicProductById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Sản phẩm không hợp lệ");
        }

        Product product = productRepository.findByIdAndActiveTrueAndQuantityGreaterThan(id, 0)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại hoặc đã hết hàng"));

        product.getImages().size();

        return product;
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

        syncProductImagesFromCurrentImageUrl(product);
        product.recalculateInventoryFields();

        Product saved = productRepository.save(product);
        inventoryLogService.log(
                owner,
                safeCurrentUser(),
                saved,
                InventoryLog.ACTION_PRODUCT_CREATE,
                0,
                saved.getQuantity(),
                "PRODUCT",
                saved.getId(),
                "Thêm sản phẩm: " + saved.getName()
        );
        return saved;
    }

    @Transactional
    public Product update(Long id, Product updatedProduct, AppUser user) {
        AppUser owner = workspaceOwner(user);

        validateProduct(updatedProduct);

        Product existing = getById(id, owner);
        int beforeQuantity = existing.getQuantity() == null ? 0 : existing.getQuantity();

        if (productRepository.existsByCodeAndUserAndActiveTrueAndIdNot(updatedProduct.getCode(), owner, id)) {
            throw new IllegalArgumentException("Mã sản phẩm đã tồn tại trong web của Owner này");
        }

        existing.setCode(updatedProduct.getCode());
        existing.setName(updatedProduct.getName());
        existing.setCategory(updatedProduct.getCategory());
        existing.setBrand(updatedProduct.getBrand());
        existing.setImageUrl(updatedProduct.getImageUrl());

        existing.getImages().clear();

        for (ProductImage image : updatedProduct.getImages()) {
            image.setProduct(existing);
            existing.getImages().add(image);
        }

        ProductImage mainImage = existing.getMainImageObject();

        if (mainImage != null) {
            existing.setImageUrl(mainImage.getImageUrl());
        }

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

        Product saved = productRepository.save(existing);
        inventoryLogService.log(
                owner,
                safeCurrentUser(),
                saved,
                InventoryLog.ACTION_PRODUCT_UPDATE,
                beforeQuantity,
                saved.getQuantity(),
                "PRODUCT",
                saved.getId(),
                "Cập nhật sản phẩm: " + saved.getName()
        );
        return saved;
    }

    @Transactional
    public void syncProductUploadedImages(Product product,
                                          List<String> existingImageUrls,
                                          List<MultipartFile> imageFiles,
                                          List<Integer> imagePositionXs,
                                          List<Integer> imagePositionYs,
                                          Integer mainImageIndex) {
        if (product == null) {
            return;
        }

        product.getImages().clear();

        int maxSize = maxSize(existingImageUrls, imageFiles, imagePositionXs, imagePositionYs);
        int sort = 0;

        for (int i = 0; i < maxSize; i++) {
            String imageUrl = getStringValue(existingImageUrls, i);
            MultipartFile file = getFileValue(imageFiles, i);

            if (file != null && !file.isEmpty()) {
                imageUrl = saveUploadedProductImage(file);
            }

            if (!StringUtils.hasText(imageUrl)) {
                continue;
            }

            ProductImage image = new ProductImage();
            image.setImageUrl(imageUrl);
            image.setPositionX(getPositionValue(imagePositionXs, i));
            image.setPositionY(getPositionValue(imagePositionYs, i));
            image.setSortOrder(sort++);
            image.setMainImage(mainImageIndex != null && mainImageIndex == i);
            image.setProduct(product);

            product.getImages().add(image);
        }

        boolean hasMainImage = product.getImages()
                .stream()
                .anyMatch(ProductImage::getMainImage);

        if (!hasMainImage && !product.getImages().isEmpty()) {
            product.getImages().get(0).setMainImage(true);
        }

        ProductImage main = product.getMainImageObject();

        if (main != null) {
            product.setImageUrl(main.getImageUrl());
        } else {
            product.setImageUrl(null);
        }
    }

    private String saveUploadedProductImage(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = getSafeExtension(originalFilename);
            String filename = UUID.randomUUID() + extension;

            Path uploadDir = Paths.get("uploads", "products");
            Files.createDirectories(uploadDir);

            Path targetPath = uploadDir.resolve(filename);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/products/" + filename;
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể lưu ảnh sản phẩm: " + e.getMessage());
        }
    }

    private String getSafeExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return ".jpg";
        }

        String lower = filename.toLowerCase();

        if (lower.endsWith(".png")) {
            return ".png";
        }

        if (lower.endsWith(".webp")) {
            return ".webp";
        }

        if (lower.endsWith(".jpeg")) {
            return ".jpeg";
        }

        if (lower.endsWith(".jpg")) {
            return ".jpg";
        }

        return ".jpg";
    }

    private int maxSize(List<String> existingImageUrls,
                        List<MultipartFile> imageFiles,
                        List<Integer> imagePositionXs,
                        List<Integer> imagePositionYs) {
        int max = 0;

        if (existingImageUrls != null) {
            max = Math.max(max, existingImageUrls.size());
        }

        if (imageFiles != null) {
            max = Math.max(max, imageFiles.size());
        }

        if (imagePositionXs != null) {
            max = Math.max(max, imagePositionXs.size());
        }

        if (imagePositionYs != null) {
            max = Math.max(max, imagePositionYs.size());
        }

        return max;
    }

    private String getStringValue(List<String> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return "";
        }

        String value = values.get(index);
        return value == null ? "" : value.trim();
    }

    private MultipartFile getFileValue(List<MultipartFile> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return null;
        }

        return values.get(index);
    }

    private int getPositionValue(List<Integer> values, int index) {
        if (values == null || index < 0 || index >= values.size() || values.get(index) == null) {
            return 50;
        }

        int value = values.get(index);

        if (value < 0) {
            return 0;
        }

        if (value > 100) {
            return 100;
        }

        return value;
    }

    private void syncProductImagesFromCurrentImageUrl(Product product) {
        if (product == null) {
            return;
        }

        if (!product.getImages().isEmpty()) {
            ProductImage main = product.getMainImageObject();

            if (main != null) {
                product.setImageUrl(main.getImageUrl());
            }

            return;
        }

        if (!StringUtils.hasText(product.getImageUrl())) {
            return;
        }

        ProductImage image = new ProductImage();
        image.setImageUrl(product.getImageUrl());
        image.setPositionX(50);
        image.setPositionY(50);
        image.setSortOrder(0);
        image.setMainImage(true);
        image.setProduct(product);

        product.getImages().add(image);
    }

    @Transactional
    public String delete(Long id, AppUser user) {
        AppUser owner = workspaceOwner(user);

        Product product = getById(id, owner);

        int beforeQuantity = product.getQuantity() == null ? 0 : product.getQuantity();
        product.setActive(false);
        Product saved = productRepository.save(product);

        inventoryLogService.log(
                owner,
                safeCurrentUser(),
                saved,
                InventoryLog.ACTION_PRODUCT_DELETE,
                beforeQuantity,
                beforeQuantity,
                "PRODUCT",
                saved.getId(),
                "Ẩn sản phẩm: " + saved.getName()
        );

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
    public StockChangeResult increaseStock(Product product,
                                           int amount,
                                           BigDecimal importPrice,
                                           LocalDate expiryDate) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
        }

        Product managedProduct = getManagedProduct(product);

        int beforeQuantity = safeQuantity(managedProduct.getQuantity());

        managedProduct.increaseStock(amount);

        if (importPrice != null) {
            managedProduct.setImportPrice(importPrice);
        }

        if (expiryDate != null) {
            managedProduct.setExpiryDate(expiryDate);
        }

        managedProduct.recalculateInventoryFields();

        Product savedProduct = productRepository.save(managedProduct);

        int afterQuantity = safeQuantity(savedProduct.getQuantity());

        return new StockChangeResult(savedProduct, beforeQuantity, afterQuantity);
    }

    @Transactional
    public StockChangeResult decreaseStockForSale(Product product, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số lượng bán phải lớn hơn 0");
        }

        Product managedProduct = getManagedProduct(product);

        int beforeQuantity = safeQuantity(managedProduct.getQuantity());

        if (beforeQuantity < amount) {
            throw new IllegalArgumentException(
                    "Sản phẩm '" + managedProduct.getName() + "' không đủ tồn kho, hiện còn "
                            + beforeQuantity + ", không thể xuất " + amount
            );
        }

        managedProduct.registerSale(amount);

        Product savedProduct = productRepository.save(managedProduct);

        int afterQuantity = safeQuantity(savedProduct.getQuantity());

        return new StockChangeResult(savedProduct, beforeQuantity, afterQuantity);
    }

    @Transactional
    public StockChangeResult restoreStockFromSale(Product product, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số lượng hoàn tồn phải lớn hơn 0");
        }

        Product managedProduct = getManagedProduct(product);

        int beforeQuantity = safeQuantity(managedProduct.getQuantity());

        managedProduct.restoreSale(amount);

        Product savedProduct = productRepository.save(managedProduct);

        int afterQuantity = safeQuantity(savedProduct.getQuantity());

        return new StockChangeResult(savedProduct, beforeQuantity, afterQuantity);
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
                                               String prefix,
                                               boolean saleOnly,
                                               int page,
                                               int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 60);

        String kw = StringUtils.hasText(keyword) ? keyword.trim() : "";
        String safeCategory = StringUtils.hasText(category) ? category.trim() : "";
        String safePrefix = StringUtils.hasText(prefix) ? prefix.trim() : "";

        return productRepository.searchPublicProductsPaged(
                kw,
                safeCategory,
                safePrefix,
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

    private AppUser safeCurrentUser() {
        try {
            return authService.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
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
    private int safeQuantity(Integer value) {
        return value == null ? 0 : value;
    }

    private Product getManagedProduct(Product product) {
        if (product == null || product.getId() == null) {
            throw new IllegalArgumentException("Sản phẩm không hợp lệ");
        }

        return productRepository.findById(product.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
    }

    public static class StockChangeResult {
        private final Product product;
        private final int beforeQuantity;
        private final int afterQuantity;
        private final int quantityChange;

        public StockChangeResult(Product product, int beforeQuantity, int afterQuantity) {
            this.product = product;
            this.beforeQuantity = beforeQuantity;
            this.afterQuantity = afterQuantity;
            this.quantityChange = afterQuantity - beforeQuantity;
        }

        public Product getProduct() {
            return product;
        }

        public int getBeforeQuantity() {
            return beforeQuantity;
        }

        public int getAfterQuantity() {
            return afterQuantity;
        }

        public int getQuantityChange() {
            return quantityChange;
        }
    }

}