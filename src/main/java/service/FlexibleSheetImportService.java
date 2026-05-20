package service;

import entity.AppUser;
import entity.Order;
import entity.OrderItem;
import entity.Product;
import entity.StockImport;
import entity.Supplier;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.OrderItemRepository;
import repository.OrderRepository;
import repository.ProductRepository;
import repository.StockImportRepository;
import repository.SupplierRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Import Google Sheet linh hoạt.
 *
 * Import sản phẩm:
 * - Mỗi dòng sheet tạo 1 StockImport.
 * - Tự động tăng tồn kho Product.
 * - Không xóa sản phẩm cũ.
 *
 * Import đơn hàng:
 * - Mỗi dòng sheet tạo Order + OrderItem.
 * - Tự động trừ tồn kho.
 */
@Service
public class FlexibleSheetImportService {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockImportRepository stockImportRepository;
    private final AuthService authService;
    private final ProductService productService;

    public FlexibleSheetImportService(ProductRepository productRepository,
                                      SupplierRepository supplierRepository,
                                      OrderRepository orderRepository,
                                      OrderItemRepository orderItemRepository,
                                      StockImportRepository stockImportRepository,
                                      AuthService authService,
                                      ProductService productService) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.stockImportRepository = stockImportRepository;
        this.authService = authService;
        this.productService = productService;
    }

    public List<String> readHeaders(String sheetUrl, String gid) throws Exception {
        List<String> headers = new ArrayList<>();

        BufferedReader reader = openCsv(sheetUrl, gid);
        CSVParser parser = CSVFormat.DEFAULT.builder()
                .setTrim(true)
                .build()
                .parse(reader);

        Iterator<CSVRecord> iterator = parser.iterator();

        if (!iterator.hasNext()) {
            return headers;
        }

        CSVRecord headerRow = iterator.next();

        for (String header : headerRow) {
            if (header != null && !header.trim().isEmpty()) {
                headers.add(header.trim());
            }
        }

        return headers;
    }

    /**
     * Import sản phẩm từ Google Sheet.
     *
     * Lưu ý:
     * - Sheet sản phẩm được hiểu là dữ liệu nhập hàng.
     * - Mỗi dòng có số lượng > 0 sẽ tạo 1 phiếu nhập.
     * - Không reset/xóa dữ liệu cũ.
     */
    @Transactional(rollbackFor = Exception.class)
    public int importProducts(String sheetUrl,
                              String gid,
                              String codeColumn,
                              String nameColumn,
                              String quantityColumn,
                              String totalQuantityColumn,
                              String soldQuantityColumn,
                              String importPriceColumn,
                              String salePriceColumn,
                              String supplierColumn,
                              String expiryDateColumn) throws Exception {

        AppUser owner = authService.getWorkspaceOwner();
        List<Map<String, String>> records = readRowsAsMap(sheetUrl, gid);

        int count = 0;
        String batchCode = String.valueOf(System.currentTimeMillis());
        Set<String> usedCodesInThisImport = new HashSet<>();

        Supplier defaultSupplier = getOrCreateDefaultSupplier(owner);

        for (Map<String, String> row : records) {
            String productName = get(row, nameColumn);

            if (blank(productName)) {
                continue;
            }

            int rowNumber = count + 1;

            String productCode = normalizeCode(get(row, codeColumn));

            if (blank(productCode)) {
                productCode = createUniqueProductCode(batchCode, rowNumber, usedCodesInThisImport);
            }

            if (usedCodesInThisImport.contains(productCode)) {
                productCode = productCode + "-" + rowNumber;
            }

            usedCodesInThisImport.add(productCode);

            Optional<Product> existingProduct = productRepository
                    .findFirstByCodeAndUserOrderByIdAsc(productCode, owner);

            if (existingProduct.isEmpty()) {
                existingProduct = productRepository
                        .findFirstByNameContainingIgnoreCaseAndUserOrderByIdAsc(productName, owner);
            }

            Product product;

            if (existingProduct.isPresent()) {
                product = existingProduct.get();
                product.setActive(true);
            } else {
                product = new Product();
                product.setCode(productCode);
                product.setUser(owner);
                product.setActive(true);
            }

            product.setName(productName);

            if (blank(product.getCategory())) {
                product.setCategory("Mỹ phẩm");
            }

            BigDecimal importPrice = toMoney(get(row, importPriceColumn));
            BigDecimal salePrice = toMoney(get(row, salePriceColumn));

            if (importPrice.signum() > 0) {
                product.setImportPrice(importPrice);
            }

            if (salePrice.signum() > 0) {
                product.setSalePrice(salePrice);
            }

            LocalDate expiryDate = toDate(get(row, expiryDateColumn));

            if (expiryDate != null) {
                product.setExpiryDate(expiryDate);
            }

            int importQty = toPositiveInt(get(row, quantityColumn));

            if (importQty == 0) {
                int totalQuantity = toPositiveInt(get(row, totalQuantityColumn));
                int soldQuantity = toPositiveInt(get(row, soldQuantityColumn));

                importQty = totalQuantity;

                if (soldQuantity > 0 && product.getSoldQuantity() == 0) {
                    product.setSoldQuantity(soldQuantity);
                }
            }

            String supplierName = get(row, supplierColumn);

            Supplier supplier = blank(supplierName)
                    ? defaultSupplier
                    : getOrCreateSupplier(supplierName, owner);

            product.setSupplier(supplier);

            /*
             * Không setTotalQuantity(importQty) trước khi gọi increaseStock,
             * vì increaseStock sẽ tự cộng số lượng nhập.
             * Nếu set trước sẽ bị cộng đôi số lượng.
             */
            product = productRepository.save(product);

            if (importQty > 0) {
                StockImport stockImport = new StockImport();
                stockImport.setProduct(product);
                stockImport.setSupplier(supplier);
                stockImport.setQuantity(importQty);
                stockImport.setImportPrice(importPrice.signum() > 0 ? importPrice : product.getImportPrice());
                stockImport.setExpiryDate(expiryDate);
                stockImport.setNote("Import từ Google Sheet - batch " + batchCode);
                stockImport.setUser(owner);
                stockImport.setImportCode("IMP-" + batchCode + "-" + rowNumber);

                stockImportRepository.save(stockImport);

                productService.increaseStock(
                        product,
                        importQty,
                        importPrice.signum() > 0 ? importPrice : product.getImportPrice(),
                        expiryDate
                );
            }

            count++;
        }

        return count;
    }

    /**
     * Import đơn hàng từ Google Sheet.
     */
    @Transactional(rollbackFor = Exception.class)
    public int importOrders(String sheetUrl,
                            String gid,
                            String orderCodeColumn,
                            String customerNameColumn,
                            String phoneColumn,
                            String addressColumn,
                            String productNameColumn,
                            String quantityColumn,
                            String shippingFeeColumn,
                            String totalBillColumn,
                            String customerDepositColumn,
                            String statusColumn) throws Exception {

        AppUser owner = authService.getWorkspaceOwner();
        List<Map<String, String>> records = readRowsAsMap(sheetUrl, gid);

        int count = 0;
        String batchCode = String.valueOf(System.currentTimeMillis());
        Set<String> usedOrderCodesInThisImport = new HashSet<>();

        for (Map<String, String> row : records) {
            String customerName = get(row, customerNameColumn);
            String productName = get(row, productNameColumn);

            if (blank(customerName) || blank(productName)) {
                continue;
            }

            int rowNumber = count + 1;

            String orderCode = normalizeCode(get(row, orderCodeColumn));

            if (blank(orderCode)) {
                orderCode = "ORD-SHEET-" + batchCode + "-" + rowNumber;
            }

            orderCode = ensureUniqueOrderCode(
                    orderCode,
                    usedOrderCodesInThisImport,
                    batchCode,
                    rowNumber
            );

            usedOrderCodesInThisImport.add(orderCode);

            final String finalProductName = productName;
            final AppUser finalOwner = owner;
            final String finalBatchCode = batchCode;
            final int finalRowNumber = rowNumber;

            Product product = productRepository
                    .findFirstByNameContainingIgnoreCaseAndUserOrderByIdAsc(finalProductName, finalOwner)
                    .orElseGet(() -> createAutoProduct(finalProductName, finalOwner, finalBatchCode, finalRowNumber));

            if (!Boolean.TRUE.equals(product.getActive())) {
                product.setActive(true);
                product = productRepository.save(product);
            }

            int quantity = Math.max(1, toPositiveInt(get(row, quantityColumn)));

            if (product.getQuantity() < quantity) {
                productService.increaseStock(
                        product,
                        quantity - product.getQuantity(),
                        product.getImportPrice(),
                        product.getExpiryDate()
                );

                product = productRepository.findById(product.getId()).orElse(product);
            }

            BigDecimal unitPrice = product.getSalePrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            BigDecimal shippingFee = toMoney(get(row, shippingFeeColumn));
            BigDecimal importedBill = toMoney(get(row, totalBillColumn));
            BigDecimal customerDeposit = toMoney(get(row, customerDepositColumn));

            BigDecimal finalBill = importedBill.signum() > 0
                    ? importedBill
                    : subtotal.add(shippingFee);

            Order order = new Order();
            order.setOrderCode(orderCode);
            order.setCustomerName(customerName);
            order.setCustomerPhone(get(row, phoneColumn));
            order.setCustomerAddress(get(row, addressColumn));
            order.setStatus(normalizeStatus(get(row, statusColumn)));
            order.setShippingFee(shippingFee);
            order.setCustomerDeposit(customerDeposit);
            order.setTotalBill(finalBill);
            order.setTotalAmount(finalBill);
            order.setUser(owner);

            Order savedOrder = orderRepository.save(order);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setOriginalPrice(unitPrice);
            orderItem.setUnitPrice(unitPrice);
            orderItem.setCostPrice(product.getImportPrice());
            orderItem.setSubtotal(subtotal);
            orderItem.recalculate();

            orderItemRepository.save(orderItem);

            productService.decreaseStockForSale(product, quantity);

            savedOrder.recalculateMoneyFields();
            orderRepository.save(savedOrder);

            count++;
        }

        return count;
    }

    private List<Map<String, String>> readRowsAsMap(String sheetUrl, String gid) throws Exception {
        BufferedReader reader = openCsv(sheetUrl, gid);

        CSVParser parser = CSVFormat.DEFAULT.builder()
                .setTrim(true)
                .build()
                .parse(reader);

        Iterator<CSVRecord> iterator = parser.iterator();

        List<Map<String, String>> result = new ArrayList<>();

        if (!iterator.hasNext()) {
            return result;
        }

        CSVRecord headerRow = iterator.next();

        List<String> headers = new ArrayList<>();
        List<Integer> validIndexes = new ArrayList<>();

        for (int i = 0; i < headerRow.size(); i++) {
            String header = headerRow.get(i);

            if (header != null && !header.trim().isEmpty()) {
                headers.add(header.trim());
                validIndexes.add(i);
            }
        }

        while (iterator.hasNext()) {
            CSVRecord dataRow = iterator.next();
            Map<String, String> map = new LinkedHashMap<>();

            for (int i = 0; i < headers.size(); i++) {
                int realIndex = validIndexes.get(i);

                map.put(
                        headers.get(i),
                        realIndex < dataRow.size() ? dataRow.get(realIndex).trim() : ""
                );
            }

            result.add(map);
        }

        return result;
    }

    private BufferedReader openCsv(String sheetUrl, String gid) throws Exception {
        String csvUrl = toCsvUrl(sheetUrl, gid);
        URL url = new URL(csvUrl);

        return new BufferedReader(
                new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)
        );
    }

    private String toCsvUrl(String sheetUrl, String gid) {
        String docId = extractDocId(sheetUrl);
        String gidPart = blank(gid) ? "0" : gid.trim();

        return "https://docs.google.com/spreadsheets/d/"
                + docId
                + "/export?format=csv&gid="
                + gidPart;
    }

    private String extractDocId(String sheetUrl) {
        if (blank(sheetUrl)) {
            throw new IllegalArgumentException("Link sheet không hợp lệ");
        }

        String value = sheetUrl.trim();

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("/spreadsheets/d/([a-zA-Z0-9_-]+)")
                .matcher(value);

        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new IllegalArgumentException("Không thể trích xuất ID từ link Google Sheet");
    }

    private Supplier getOrCreateDefaultSupplier(AppUser owner) {
        return supplierRepository
                .findFirstByNameAndUserOrderByIdAsc("Nhà cung cấp mặc định", owner)
                .orElseGet(() -> {
                    Supplier supplier = new Supplier();
                    supplier.setName("Nhà cung cấp mặc định");
                    supplier.setUser(owner);
                    return supplierRepository.save(supplier);
                });
    }

    private Supplier getOrCreateSupplier(String name, AppUser owner) {
        if (blank(name)) {
            return getOrCreateDefaultSupplier(owner);
        }

        String supplierName = name.trim();

        return supplierRepository
                .findFirstByNameAndUserOrderByIdAsc(supplierName, owner)
                .orElseGet(() -> {
                    Supplier supplier = new Supplier();
                    supplier.setName(supplierName);
                    supplier.setUser(owner);
                    return supplierRepository.save(supplier);
                });
    }

    private Product createAutoProduct(String name, AppUser owner, String batchCode, int rowNumber) {
        Product product = new Product();
        product.setCode("AUTO-" + batchCode + "-" + rowNumber);
        product.setName(name.trim());
        product.setCategory("Mỹ phẩm");
        product.setUser(owner);
        product.setActive(true);

        return productRepository.save(product);
    }

    private String createUniqueProductCode(String batchCode,
                                           int rowNumber,
                                           Set<String> usedCodesInThisImport) {
        String baseCode = "SP-SHEET-" + batchCode + "-" + rowNumber;
        String code = baseCode;
        int attempt = 1;

        while (usedCodesInThisImport.contains(code)
                || productRepository.findFirstByCodeOrderByIdAsc(code).isPresent()) {
            code = baseCode + "-" + attempt;
            attempt++;
        }

        return code;
    }

    private String ensureUniqueOrderCode(String base,
                                         Set<String> usedOrderCodesInThisImport,
                                         String batchCode,
                                         int rowNumber) {
        String candidate = base;
        int attempt = 1;

        while (usedOrderCodesInThisImport.contains(candidate)
                || orderRepository.existsByOrderCode(candidate)) {
            candidate = base + "-" + attempt;
            attempt++;
        }

        return candidate;
    }

    private String get(Map<String, String> row, String column) {
        if (blank(column)) {
            return "";
        }

        return row.getOrDefault(column, "").trim();
    }

    private boolean blank(String value) {
        return value == null
                || value.trim().isEmpty()
                || "-".equals(value.trim());
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Chỉ trả về số nguyên >= 0.
     * Không giữ dấu âm để tránh lỗi tồn kho âm khi import.
     */
    private int toPositiveInt(String value) {
        try {
            if (blank(value)) {
                return 0;
            }

            String cleaned = value
                    .replaceAll("[^0-9.]", "")
                    .trim();

            if (cleaned.isEmpty()) {
                return 0;
            }

            return Math.max(0, (int) Math.round(Double.parseDouble(cleaned)));
        } catch (Exception e) {
            return 0;
        }
    }

    private BigDecimal toMoney(String value) {
        try {
            if (blank(value)) {
                return BigDecimal.ZERO;
            }

            String cleaned = value
                    .replace("₫", "")
                    .replace("đ", "")
                    .replace("Đ", "")
                    .replace("VND", "")
                    .replace("vnd", "")
                    .replace(",", "")
                    .replace(".", "")
                    .replaceAll("[^0-9]", "")
                    .trim();

            if (cleaned.isEmpty()) {
                return BigDecimal.ZERO;
            }

            BigDecimal result = new BigDecimal(cleaned);

            return result.signum() < 0 ? BigDecimal.ZERO : result;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate toDate(String value) {
        if (blank(value)) {
            return null;
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private String normalizeStatus(String value) {
        if (blank(value)) {
            return "CHỜ_XÁC_NHẬN";
        }

        String status = value.toLowerCase().trim();

        if (status.contains("hủy") || status.contains("huỷ") || status.contains("cancel")) {
            return "ĐÃ_HỦY";
        }

        if (status.contains("hoàn thành") || status.contains("complete")) {
            return "HOÀN_THÀNH";
        }

        if (status.contains("đang giao") || status.contains("shipping")) {
            return "ĐANG_GIAO";
        }

        if (status.contains("đã giao") || status.contains("delivered")) {
            return "ĐÃ_GIAO";
        }

        return "CHỜ_XÁC_NHẬN";
    }
}