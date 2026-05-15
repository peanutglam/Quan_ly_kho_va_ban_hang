package service;

import entity.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Nhiệm vụ 1: Import Google Sheet = Tạo phiếu nhập (StockImport) + cập nhật Product.
 *
 * Mỗi dòng sheet sản phẩm → 1 StockImport record + tăng tồn kho Product.
 * Import đơn hàng → tạo Order/OrderItem.
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
        CSVParser parser = CSVFormat.DEFAULT.builder().setTrim(true).build().parse(reader);
        Iterator<CSVRecord> it = parser.iterator();
        if (!it.hasNext()) return headers;
        for (String h : it.next()) {
            if (h != null && !h.trim().isEmpty()) headers.add(h.trim());
        }
        return headers;
    }

    /**
     * Import sản phẩm từ sheet → tạo StockImport + cập nhật Product.
     * KHÔNG xóa sản phẩm cũ. Chỉ thêm/cập nhật.
     * @Transactional rollbackFor = Exception.class để an toàn.
     */
    @Transactional(rollbackFor = Exception.class)
    public int importProducts(String sheetUrl, String gid,
                              String codeColumn, String nameColumn,
                              String quantityColumn,
                              String totalQuantityColumn, String soldQuantityColumn,
                              String importPriceColumn, String salePriceColumn,
                              String supplierColumn, String expiryDateColumn) throws Exception {
        AppUser owner = authService.getWorkspaceOwner();
        List<Map<String, String>> records = readRowsAsMap(sheetUrl, gid);

        int count = 0;
        String batchCode = String.valueOf(System.currentTimeMillis());
        Set<String> usedCodes = new HashSet<>();

        // Tìm/tạo nhà cung cấp mặc định
        Supplier defaultSupplier = getOrCreateDefaultSupplier(owner);

        for (Map<String, String> row : records) {
            String name = get(row, nameColumn);
            if (blank(name)) continue;

            // --- Tìm/tạo sản phẩm ---
            String code = normalizeCode(get(row, codeColumn));
            Product product;

            if (blank(code)) {
                code = "SP-" + batchCode + "-" + (count + 1);
            }

            // Đảm bảo code unique trong batch
            if (usedCodes.contains(code)) {
                code = code + "-" + (count + 1);
            }
            usedCodes.add(code);

            // Tìm sản phẩm có sẵn của owner
            Optional<Product> existing = productRepository.findByCodeAndUser(code, owner);
            if (!existing.isPresent()) {
                existing = productRepository.findFirstByNameContainingIgnoreCaseAndUser(name, owner);
            }

            if (existing.isPresent()) {
                product = existing.get();
                product.setActive(true);  // kích hoạt lại nếu bị ẩn
            } else {
                product = new Product();
                product.setCode(code);
                product.setUser(owner);
            }

            // Cập nhật thông tin cơ bản sản phẩm
            product.setName(name);
            product.setCategory(blank(get(row, "")) ? product.getCategory() : "Mỹ phẩm");

            BigDecimal importPrice = toMoney(get(row, importPriceColumn));
            BigDecimal salePrice   = toMoney(get(row, salePriceColumn));
            if (importPrice.signum() > 0) product.setImportPrice(importPrice);
            if (salePrice.signum() > 0)   product.setSalePrice(salePrice);

            LocalDate expiryDate = toDate(get(row, expiryDateColumn));
            if (expiryDate != null) product.setExpiryDate(expiryDate);

            // --- Tính số lượng nhập cho phiếu nhập ---
            int importQty = toPositiveInt(get(row, quantityColumn));

            // Nếu không có quantityColumn nhưng có totalQuantity/soldQuantity → dùng để tính tồn
            if (importQty == 0) {
                int total = toPositiveInt(get(row, totalQuantityColumn));
                int sold  = toPositiveInt(get(row, soldQuantityColumn));
                // Số lượng nhập = tổng nhập (totalQuantity là tổng đã nhập)
                importQty = total > 0 ? total : 0;

                // Đặt soldQuantity nếu chưa có
                if (sold > 0 && product.getSoldQuantity() == 0) {
                    product.setSoldQuantity(sold);
                }
            }

            // --- Nhà cung cấp ---
            String supplierName = get(row, supplierColumn);
            Supplier supplier = blank(supplierName)
                    ? defaultSupplier
                    : getOrCreateSupplier(supplierName, owner);
            product.setSupplier(supplier);

            // Lưu sản phẩm (tạo mới hoặc cập nhật)
            // Nếu là sản phẩm mới, khởi tạo totalQuantity
            if (product.getId() == null && importQty > 0) {
                product.setTotalQuantity(importQty);
            }
            product = productRepository.save(product);

            // --- Tạo StockImport nếu có số lượng nhập ---
            if (importQty > 0) {
                StockImport si = new StockImport();
                si.setProduct(product);
                si.setSupplier(supplier);
                si.setQuantity(importQty);
                si.setImportPrice(importPrice.signum() > 0 ? importPrice : product.getImportPrice());
                si.setExpiryDate(expiryDate);
                si.setNote("Import từ Google Sheet - batch " + batchCode);
                si.setUser(owner);
                si.setImportCode("IMP-" + batchCode + "-" + (count + 1));
                stockImportRepository.save(si);

                // Tăng tồn kho qua productService để đảm bảo logic đúng
                productService.increaseStock(product, importQty,
                        importPrice.signum() > 0 ? importPrice : product.getImportPrice(),
                        expiryDate);
                // Reload product sau khi save
                product = productRepository.findById(product.getId()).orElse(product);
            }

            count++;
        }

        return count;
    }

    /**
     * Import đơn hàng từ sheet → tạo Order/OrderItem.
     * Liên kết với Product hiện có qua tên/mã.
     */
    @Transactional(rollbackFor = Exception.class)
    public int importOrders(String sheetUrl, String gid,
                            String orderCodeColumn, String customerNameColumn,
                            String phoneColumn, String addressColumn,
                            String productNameColumn, String quantityColumn,
                            String shippingFeeColumn, String totalBillColumn,
                            String customerDepositColumn, String statusColumn) throws Exception {
        AppUser owner = authService.getWorkspaceOwner();
        List<Map<String, String>> records = readRowsAsMap(sheetUrl, gid);

        int count = 0;
        String batchCode = String.valueOf(System.currentTimeMillis());
        Set<String> usedOrderCodes = new HashSet<>();

        for (Map<String, String> row : records) {
            String customerName = get(row, customerNameColumn);
            String productName  = get(row, productNameColumn);
            if (blank(customerName) || blank(productName)) continue;

            String orderCode = normalizeCode(get(row, orderCodeColumn));
            if (blank(orderCode)) {
                orderCode = "ORD-SHEET-" + batchCode + "-" + (count + 1);
            }
            orderCode = ensureUniqueOrderCode(orderCode, usedOrderCodes, batchCode, count + 1);
            usedOrderCodes.add(orderCode);

            // Tìm sản phẩm theo tên
            final String finalProductName = productName;
            final AppUser finalOwner = owner;
            final String finalBatchCode = batchCode;
            final int rowIndex = count + 1;

            Product product = productRepository
                    .findFirstByNameContainingIgnoreCaseAndUser(finalProductName, finalOwner)
                    .orElseGet(() -> createAutoProduct(finalProductName, finalOwner, finalBatchCode, rowIndex));

            int qty = Math.max(1, toPositiveInt(get(row, quantityColumn)));

            // Đảm bảo đủ tồn kho
            if (product.getQuantity() < qty) {
                productService.increaseStock(product, qty - product.getQuantity(),
                        product.getImportPrice(), product.getExpiryDate());
                product = productRepository.findById(product.getId()).orElse(product);
            }

            BigDecimal unitPrice    = product.getSalePrice();
            BigDecimal subtotal     = unitPrice.multiply(BigDecimal.valueOf(qty));
            BigDecimal shippingFee  = toMoney(get(row, shippingFeeColumn));
            BigDecimal importedBill = toMoney(get(row, totalBillColumn));
            BigDecimal deposit      = toMoney(get(row, customerDepositColumn));
            BigDecimal finalBill    = importedBill.signum() > 0 ? importedBill : subtotal.add(shippingFee);

            Order order = new Order();
            order.setOrderCode(orderCode);
            order.setCustomerName(customerName);
            order.setCustomerPhone(get(row, phoneColumn));
            order.setCustomerAddress(get(row, addressColumn));
            order.setStatus(normalizeStatus(get(row, statusColumn)));
            order.setShippingFee(shippingFee);
            order.setCustomerDeposit(deposit);
            order.setTotalBill(finalBill);
            order.setTotalAmount(finalBill);
            order.setUser(owner);
            Order savedOrder = orderRepository.save(order);

            OrderItem item = new OrderItem();
            item.setOrder(savedOrder);
            item.setProduct(product);
            item.setQuantity(qty);
            item.setOriginalPrice(unitPrice);
            item.setUnitPrice(unitPrice);
            item.setCostPrice(product.getImportPrice());
            item.setSubtotal(subtotal);
            item.recalculate();
            orderItemRepository.save(item);

            productService.decreaseStockForSale(product, qty);
            savedOrder.recalculateMoneyFields();
            orderRepository.save(savedOrder);

            count++;
        }
        return count;
    }

    // =====================================================
    // private helpers
    // =====================================================

    private List<Map<String, String>> readRowsAsMap(String sheetUrl, String gid) throws Exception {
        BufferedReader reader = openCsv(sheetUrl, gid);
        CSVParser parser = CSVFormat.DEFAULT.builder().setTrim(true).build().parse(reader);
        Iterator<CSVRecord> it = parser.iterator();
        List<Map<String, String>> result = new ArrayList<>();
        if (!it.hasNext()) return result;
        CSVRecord headerRow = it.next();
        List<String> headers = new ArrayList<>();
        List<Integer> validIdx = new ArrayList<>();
        for (int i = 0; i < headerRow.size(); i++) {
            String h = headerRow.get(i);
            if (h != null && !h.trim().isEmpty()) {
                headers.add(h.trim());
                validIdx.add(i);
            }
        }
        while (it.hasNext()) {
            CSVRecord dataRow = it.next();
            Map<String, String> map = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                int ri = validIdx.get(i);
                map.put(headers.get(i), ri < dataRow.size() ? dataRow.get(ri).trim() : "");
            }
            result.add(map);
        }
        return result;
    }

    private BufferedReader openCsv(String sheetUrl, String gid) throws Exception {
        String csvUrl = toCsvUrl(sheetUrl, gid);
        URL url = new URL(csvUrl);
        return new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
    }

    private String toCsvUrl(String sheetUrl, String gid) {
        String docId = extractDocId(sheetUrl);
        String gidPart = (gid == null || gid.trim().isEmpty()) ? "0" : gid.trim();
        return "https://docs.google.com/spreadsheets/d/" + docId + "/export?format=csv&gid=" + gidPart;
    }

    private String extractDocId(String sheetUrl) {
        if (sheetUrl == null || sheetUrl.trim().isEmpty()) throw new IllegalArgumentException("Link sheet không hợp lệ");
        sheetUrl = sheetUrl.trim();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("/spreadsheets/d/([a-zA-Z0-9_-]+)").matcher(sheetUrl);
        if (m.find()) return m.group(1);
        throw new IllegalArgumentException("Không thể trích xuất ID từ link Google Sheet");
    }

    private Supplier getOrCreateDefaultSupplier(AppUser owner) {
        return supplierRepository.findByNameAndUser("Nhà cung cấp mặc định", owner)
                .orElseGet(() -> {
                    Supplier s = new Supplier();
                    s.setName("Nhà cung cấp mặc định");
                    s.setUser(owner);
                    return supplierRepository.save(s);
                });
    }

    private Supplier getOrCreateSupplier(String name, AppUser owner) {
        if (blank(name)) return getOrCreateDefaultSupplier(owner);
        return supplierRepository.findByNameAndUser(name.trim(), owner)
                .orElseGet(() -> {
                    Supplier s = new Supplier();
                    s.setName(name.trim());
                    s.setUser(owner);
                    return supplierRepository.save(s);
                });
    }

    private Product createAutoProduct(String name, AppUser owner, String batch, int idx) {
        Product p = new Product();
        p.setCode("AUTO-" + batch + "-" + idx);
        p.setName(name.trim());
        p.setUser(owner);
        p.setActive(true);
        return productRepository.save(p);
    }

    private String ensureUniqueOrderCode(String base, Set<String> used, String batch, int idx) {
        String candidate = base;
        int attempt = 1;
        while (used.contains(candidate) || orderRepository.existsByOrderCode(candidate)) {
            candidate = base + "-" + attempt++;
        }
        return candidate;
    }

    // ---- parsers ----
    private String get(Map<String, String> row, String col) {
        if (blank(col)) return "";
        return row.getOrDefault(col, "").trim();
    }

    private boolean blank(String v) { return v == null || v.trim().isEmpty() || "-".equals(v.trim()); }

    private String normalizeCode(String v) { return v == null ? "" : v.trim(); }

    /** Chỉ trả về số nguyên >= 0. Không giữ dấu âm. */
    private int toPositiveInt(String value) {
        try {
            if (blank(value)) return 0;
            String cleaned = value.replaceAll("[^0-9.]", "").trim();
            if (cleaned.isEmpty()) return 0;
            return Math.max(0, (int) Math.round(Double.parseDouble(cleaned)));
        } catch (Exception e) { return 0; }
    }

    private BigDecimal toMoney(String value) {
        try {
            if (blank(value)) return BigDecimal.ZERO;
            String cleaned = value.replaceAll("[₫₩đ.,]", "").replaceAll("[^0-9]", "").trim();
            if (cleaned.isEmpty()) return BigDecimal.ZERO;
            BigDecimal result = new BigDecimal(cleaned);
            return result.signum() < 0 ? BigDecimal.ZERO : result;
        } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private LocalDate toDate(String value) {
        if (blank(value)) return null;
        List<DateTimeFormatter> fmts = List.of(
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        );
        for (DateTimeFormatter f : fmts) {
            try { return LocalDate.parse(value.trim(), f); } catch (Exception ignored) {}
        }
        return null;
    }

    private String normalizeStatus(String s) {
        if (blank(s)) return "CHỜ_XÁC_NHẬN";
        String v = s.toLowerCase();
        if (v.contains("hủy") || v.contains("huỷ")) return "ĐÃ_HỦY";
        if (v.contains("giao") || v.contains("delivered")) return "ĐÃ_GIAO";
        if (v.contains("hoàn thành") || v.contains("complete")) return "HOÀN_THÀNH";
        if (v.contains("đang giao") || v.contains("shipping")) return "ĐANG_GIAO";
        return "CHỜ_XÁC_NHẬN";
    }
}
