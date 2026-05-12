package service;

import entity.AppUser;
import entity.Order;
import entity.OrderItem;
import entity.Product;
import entity.Supplier;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.OrderItemRepository;
import repository.OrderRepository;
import repository.ProductRepository;
import repository.SupplierRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class FlexibleSheetImportService {

    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AuthService authService;
    private final ProductService productService;

    public FlexibleSheetImportService(ProductRepository productRepository,
                                      SupplierRepository supplierRepository,
                                      OrderRepository orderRepository,
                                      OrderItemRepository orderItemRepository,
                                      AuthService authService,
                                      ProductService productService) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.authService = authService;
        this.productService = productService;
    }

    public List<String> readHeaders(String sheetUrl, String gid) throws Exception {
        BufferedReader reader = openCsv(sheetUrl, gid);

        CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setTrim(true)
                .build()
                .parse(reader);

        Iterator<CSVRecord> iterator = parser.iterator();

        if (!iterator.hasNext()) {
            return new ArrayList<>();
        }

        CSVRecord headerRow = iterator.next();

        List<String> headers = new ArrayList<>();

        for (String header : headerRow) {
            if (header != null && !header.trim().isEmpty()) {
                headers.add(header.trim());
            }
        }

        return headers;
    }
    private Product findReusableProductByCode(String code, AppUser owner) {
        if (blank(code)) {
            return null;
        }

        List<Product> products = productRepository.findAllByCodeOrderByIdAsc(code);

        if (products == null || products.isEmpty()) {
            return null;
        }

        Product selected = null;

        for (Product product : products) {
            if (product.getUser() != null
                    && product.getUser().getId() != null
                    && owner.getId() != null
                    && product.getUser().getId().equals(owner.getId())) {
                selected = product;
                break;
            }
        }

        if (selected == null) {
            selected = products.get(0);
        }

        selected.setUser(owner);
        selected.setActive(true);

        for (Product product : products) {
            if (product.getId() == null || selected.getId() == null) {
                continue;
            }

            if (!product.getId().equals(selected.getId())) {
                product.setUser(owner);
                product.setActive(false);
                productRepository.save(product);
            }
        }

        return selected;
    }
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
        Set<Long> importedProductIds = new HashSet<>();

        List<Product> oldProducts = productRepository.findByUserAndActiveTrue(owner);

        for (Map<String, String> row : records) {
            String name = get(row, nameColumn);

            if (blank(name)) {
                continue;
            }

            String rawCode = get(row, codeColumn);
            String code = normalizeCode(rawCode);

            Product product;

            if (blank(code)) {
                code = createUniqueProductCode(batchCode, count + 1, usedCodesInThisImport);
                product = new Product();
            } else {
                Product existingProduct = findReusableProductByCode(code, owner);

                if (existingProduct != null) {
                    product = existingProduct;
                } else {
                    code = ensureUniqueProductCode(code, usedCodesInThisImport);
                    product = new Product();
                }
            }

            usedCodesInThisImport.add(code);

            int stockQuantity = toInt(get(row, quantityColumn));
            int totalQuantity = toInt(get(row, totalQuantityColumn));
            int soldQuantity = toInt(get(row, soldQuantityColumn));

            if (totalQuantity <= 0) {
                totalQuantity = stockQuantity + soldQuantity;
            }

            if (soldQuantity > totalQuantity) {
                totalQuantity = soldQuantity;
            }

            product.setCode(code);
            product.setName(name);
            product.setCategory("Mỹ phẩm");
            product.setBrand(get(row, supplierColumn));
            product.setTotalQuantity(totalQuantity);
            product.setSoldQuantity(soldQuantity);
            product.setImportPrice(toMoney(get(row, importPriceColumn)));
            product.setSalePrice(toMoney(get(row, salePriceColumn)));
            product.setExpiryDate(toDate(get(row, expiryDateColumn)));
            product.setDescription("Import linh hoạt từ Google Sheet");
            product.setUser(owner);
            product.setActive(true);

            Supplier supplier = getOrCreateSupplier(get(row, supplierColumn), owner);
            product.setSupplier(supplier);

            product.recalculateInventoryFields();

            Product savedProduct = productRepository.save(product);

            if (savedProduct.getId() != null) {
                importedProductIds.add(savedProduct.getId());
            }

            count++;
        }

        for (Product oldProduct : oldProducts) {
            if (oldProduct.getId() != null && !importedProductIds.contains(oldProduct.getId())) {
                oldProduct.setActive(false);
                productRepository.save(oldProduct);
            }
        }

        return count;
    }

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

        clearOrdersForUser(owner);

        for (Map<String, String> row : records) {
            String customerName = get(row, customerNameColumn);
            String productName = get(row, productNameColumn);

            if (blank(customerName) || blank(productName)) {
                continue;
            }

            String rawOrderCode = get(row, orderCodeColumn);
            String orderCode = normalizeCode(rawOrderCode);

            int rowIndex = count + 1;

            if (blank(orderCode)) {
                orderCode = createUniqueOrderCode(batchCode, rowIndex, usedOrderCodesInThisImport);
            } else {
                orderCode = ensureUniqueOrderCode(orderCode, batchCode, rowIndex, usedOrderCodesInThisImport);
            }

            usedOrderCodesInThisImport.add(orderCode);

            Product product = productRepository
                    .findFirstByNameContainingIgnoreCaseAndUserAndActiveTrue(productName, owner)
                    .orElseGet(() -> createAutoProduct(productName, owner, batchCode, rowIndex));

            int quantity = toInt(get(row, quantityColumn));

            if (quantity <= 0) {
                quantity = 1;
            }

            if (product.getQuantity() < quantity) {
                productService.increaseStock(
                        product,
                        quantity - product.getQuantity(),
                        product.getImportPrice(),
                        product.getExpiryDate()
                );
            }

            BigDecimal unitPrice = product.getSalePrice() == null
                    ? BigDecimal.ZERO
                    : product.getSalePrice();

            BigDecimal productSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            BigDecimal shippingFee = toMoney(get(row, shippingFeeColumn));
            BigDecimal importedTotalBill = toMoney(get(row, totalBillColumn));
            BigDecimal customerDeposit = toMoney(get(row, customerDepositColumn));

            BigDecimal finalTotalBill = importedTotalBill.signum() > 0
                    ? importedTotalBill
                    : productSubtotal.add(shippingFee);

            Order order = new Order();
            order.setOrderCode(orderCode);
            order.setCustomerName(customerName);
            order.setCustomerPhone(get(row, phoneColumn));
            order.setCustomerAddress(get(row, addressColumn));
            order.setStatus(normalizeStatus(get(row, statusColumn)));
            order.setUser(owner);

            /*
             * Các field này yêu cầu Order.java đã có:
             * shippingFee, totalBill, customerDeposit, remainingAmount.
             */
            order.setShippingFee(shippingFee);
            order.setCustomerDeposit(customerDeposit);
            order.setTotalBill(finalTotalBill);
            order.setTotalAmount(finalTotalBill);

            Order savedOrder = orderRepository.save(order);

            OrderItem item = new OrderItem();
            item.setOrder(savedOrder);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setSubtotal(productSubtotal);

            orderItemRepository.save(item);

            productService.decreaseStockForSale(product, quantity);

            savedOrder.recalculateMoneyFields();
            orderRepository.save(savedOrder);

            count++;
        }

        return count;
    }

    private List<Map<String, String>> readRowsAsMap(String sheetUrl, String gid) throws Exception {
        BufferedReader reader = openCsv(sheetUrl, gid);

        CSVParser parser = CSVFormat.DEFAULT
                .builder()
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
                String value = realIndex < dataRow.size() ? dataRow.get(realIndex) : "";

                map.put(headers.get(i), value == null ? "" : value.trim());
            }

            result.add(map);
        }

        return result;
    }

    private BufferedReader openCsv(String sheetUrl, String gid) throws Exception {
        String sheetId = extractSheetId(sheetUrl);

        String csvUrl = "https://docs.google.com/spreadsheets/d/"
                + sheetId
                + "/gviz/tq?tqx=out:csv&gid="
                + gid;

        return new BufferedReader(
                new InputStreamReader(new URL(csvUrl).openStream(), StandardCharsets.UTF_8)
        );
    }

    private String extractSheetId(String url) {
        try {
            String[] parts = url.split("/d/");
            return parts[1].split("/")[0];
        } catch (Exception e) {
            throw new IllegalArgumentException("Link Google Sheet không hợp lệ");
        }
    }

    private Supplier getOrCreateSupplier(String name, AppUser owner) {
        if (blank(name)) {
            name = "Chưa xác định";
        }

        String supplierName = name.trim();

        List<Supplier> suppliers = supplierRepository.findAllByNameAndUserOrderByIdAsc(supplierName, owner);

        if (suppliers != null && !suppliers.isEmpty()) {
            return suppliers.get(0);
        }

        Supplier supplier = new Supplier();
        supplier.setName(supplierName);
        supplier.setPhone("");
        supplier.setEmail("");
        supplier.setAddress("");
        supplier.setUser(owner);

        return supplierRepository.save(supplier);
    }

    private Product createAutoProduct(String productName,
                                      AppUser owner,
                                      String batchCode,
                                      int rowIndex) {
        Product product = new Product();

        String code = createUniqueProductCode(batchCode, rowIndex, new HashSet<>());

        product.setCode(code);
        product.setName(productName);
        product.setCategory("Mỹ phẩm");
        product.setBrand("Chưa xác định");
        product.setTotalQuantity(0);
        product.setSoldQuantity(0);
        product.setImportPrice(BigDecimal.ZERO);
        product.setSalePrice(BigDecimal.ZERO);
        product.setDescription("Tự tạo từ Sheet đơn hàng");
        product.setUser(owner);
        product.setActive(true);

        product.recalculateInventoryFields();

        return productRepository.save(product);
    }

    private void clearOrdersForUser(AppUser owner) {
        List<Order> orders = orderRepository.findByUserOrderByIdDesc(owner);

        for (Order order : orders) {
            if (!OrderService.STATUS_CANCELLED.equals(order.getStatus())) {
                for (OrderItem item : order.getItems()) {
                    int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
                    productService.restoreStockFromSale(item.getProduct(), quantity);
                }
            }

            orderRepository.delete(order);
        }

        orderRepository.flush();
    }

    private String createUniqueProductCode(String batchCode,
                                           int rowIndex,
                                           Set<String> usedCodesInThisImport) {
        String base = "SP-SHEET-" + batchCode + "-" + rowIndex;
        return ensureUniqueProductCode(base, usedCodesInThisImport);
    }

    private String ensureUniqueProductCode(String baseCode,
                                           Set<String> usedCodesInThisImport) {
        String cleanBase = normalizeCode(baseCode);

        if (blank(cleanBase)) {
            cleanBase = "SP-SHEET-" + System.currentTimeMillis();
        }

        String candidate = cleanBase;
        int index = 1;

        while (usedCodesInThisImport.contains(candidate) || productRepository.existsByCode(candidate)) {
            candidate = cleanBase + "-" + index;
            index++;
        }

        return candidate;
    }

    private String createUniqueOrderCode(String batchCode,
                                         int rowIndex,
                                         Set<String> usedOrderCodesInThisImport) {
        String base = "ORD-SHEET-" + batchCode + "-" + rowIndex;
        return ensureUniqueOrderCode(base, batchCode, rowIndex, usedOrderCodesInThisImport);
    }

    private String ensureUniqueOrderCode(String baseCode,
                                         String batchCode,
                                         int rowIndex,
                                         Set<String> usedOrderCodesInThisImport) {
        String cleanBase = normalizeCode(baseCode);

        if (blank(cleanBase)) {
            cleanBase = "ORD-SHEET-" + batchCode + "-" + rowIndex;
        }

        String candidate = cleanBase;
        int index = 1;

        while (usedOrderCodesInThisImport.contains(candidate) || orderRepository.existsByOrderCode(candidate)) {
            candidate = cleanBase + "-" + index;
            index++;
        }

        return candidate;
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private String get(Map<String, String> row, String column) {
        if (blank(column)) {
            return "";
        }

        return row.getOrDefault(column, "").trim();
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty() || "-".equals(value.trim());
    }

    private int toInt(String value) {
        try {
            if (blank(value)) {
                return 0;
            }

            value = value
                    .replace(",", ".")
                    .replaceAll("[^0-9.\\-]", "");

            if (blank(value) || "-".equals(value)) {
                return 0;
            }

            int result = (int) Math.round(Double.parseDouble(value));

            return Math.max(result, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    private BigDecimal toMoney(String value) {
        try {
            if (blank(value)) {
                return BigDecimal.ZERO;
            }

            value = value
                    .replace("₩", "")
                    .replace("đ", "")
                    .replace("₫", "")
                    .replace(".", "")
                    .replace(",", "")
                    .replaceAll("[^0-9]", "");

            if (blank(value)) {
                return BigDecimal.ZERO;
            }

            return new BigDecimal(value);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate toDate(String value) {
        try {
            if (blank(value)) {
                return null;
            }

            value = value.trim();

            List<DateTimeFormatter> formatters = List.of(
                    DateTimeFormatter.ofPattern("d/M/yyyy"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd")
            );

            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDate.parse(value, formatter);
                } catch (Exception ignored) {
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeStatus(String status) {
        if (blank(status)) {
            return OrderService.STATUS_PENDING;
        }

        String s = status.toLowerCase();

        if (s.contains("hủy") || s.contains("huỷ")) {
            return OrderService.STATUS_CANCELLED;
        }

        if (s.contains("giao")) {
            return OrderService.STATUS_DELIVERED;
        }

        if (s.contains("hoàn thành")) {
            return OrderService.STATUS_COMPLETED;
        }

        if (s.contains("đang")) {
            return OrderService.STATUS_SHIPPING;
        }

        return OrderService.STATUS_PENDING;
    }

}