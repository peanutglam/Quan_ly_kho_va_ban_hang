package service;

import dto.CreateOrderApiItemRequest;
import dto.CreateOrderApiRequest;
import dto.DailyReportDTO;
import entity.AppUser;
import entity.Order;
import entity.OrderItem;
import entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import repository.OrderItemRepository;
import repository.OrderRepository;
import repository.StockImportRepository;
import dto.DailyTrendDTO;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class OrderService {

    public static final String STATUS_PENDING   = "CHỜ_XÁC_NHẬN";
    public static final String STATUS_SHIPPING  = "ĐANG_GIAO";
    public static final String STATUS_COMPLETED = "HOÀN_THÀNH";
    public static final String STATUS_DELIVERED = "ĐÃ_GIAO";
    public static final String STATUS_CANCELLED = "ĐÃ_HỦY";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockImportRepository stockImportRepository;
    private final ProductService productService;
    private final AuthService authService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        StockImportRepository stockImportRepository,
                        ProductService productService,
                        AuthService authService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.stockImportRepository = stockImportRepository;
        this.productService = productService;
        this.authService = authService;
    }

    private AppUser owner() {
        return authService.getWorkspaceOwner();
    }

    private AppUser publicOwner() {
        return authService.getSystemOwner();
    }

    @Transactional(readOnly = true)
    public Page<Order> getOrdersPageForApi(AppUser owner, int page, int size, String keyword, String status) {
        if (owner == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 50));

        String safeKeyword = keyword == null ? "" : keyword.trim();
        String safeStatus = status == null ? "" : status.trim();

        Pageable pageable = PageRequest.of(safePage, safeSize);

        return orderRepository.filterOrdersPaged(owner, safeKeyword, safeStatus, pageable);
    }


    @Transactional
    public Order createOrderFromMobileApi(AppUser owner, CreateOrderApiRequest request) {
        if (owner == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập");
        }

        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu đơn hàng không hợp lệ");
        }

        validateCustomerInfo(request.getCustomerName(), request.getCustomerPhone());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 sản phẩm");
        }

        Order order = new Order();
        order.setUser(owner);
        order.setOrderCode("ORD-APP-" + System.currentTimeMillis());
        order.setCustomerName(request.getCustomerName() == null ? "" : request.getCustomerName().trim());
        order.setCustomerPhone(request.getCustomerPhone() == null ? "" : request.getCustomerPhone().trim());
        order.setCustomerAddress(request.getCustomerAddress() == null ? "" : request.getCustomerAddress().trim());
        order.setShippingFee(money(request.getShippingFee()));
        order.setCustomerDeposit(money(request.getCustomerDeposit()));
        order.setStatus(STATUS_PENDING);

        BigDecimal total = BigDecimal.ZERO;
        int validLineCount = 0;

        for (CreateOrderApiItemRequest itemRequest : request.getItems()) {
            if (itemRequest == null) {
                continue;
            }

            Long productId = itemRequest.getProductId();
            Integer quantityValue = itemRequest.getQuantity();

            if (productId == null) {
                continue;
            }

            int quantity = quantityValue == null ? 0 : quantityValue;

            if (quantity <= 0) {
                continue;
            }

            Product product = productService.getById(productId, owner);

            int stock = product.getQuantity() == null ? 0 : product.getQuantity();

            if (stock < quantity) {
                throw new IllegalArgumentException(
                        "Sản phẩm " + product.getName() + " chỉ còn " + stock + ", không thể xuất " + quantity
                );
            }

            BigDecimal originalPrice = money(product.getSalePrice());
            BigDecimal unitPrice = money(product.getEffectiveSalePrice());

            if (unitPrice.signum() <= 0) {
                unitPrice = originalPrice;
            }

            BigDecimal costPrice = money(product.getImportPrice());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setOriginalPrice(originalPrice);
            item.setUnitPrice(unitPrice);
            item.setCostPrice(costPrice);
            item.recalculate();

            order.getItems().add(item);

            total = total.add(item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal());
            validLineCount++;

            productService.decreaseStockForSale(product, quantity);
        }

        if (validLineCount == 0) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 sản phẩm hợp lệ");
        }

        order.setTotalAmount(total);
        order.setTotalBill(total);
        order.recalculateMoneyFields();

        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order getOrderDetailForApi(AppUser owner, Long id) {
        if (owner == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập");
        }

        if (id == null) {
            throw new IllegalArgumentException("Mã đơn hàng không hợp lệ");
        }

        Order order = orderRepository.findByIdAndUser(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        if (order.getItems() != null) {
            order.getItems().size();

            for (OrderItem item : order.getItems()) {
                if (item == null) {
                    continue;
                }

                item.getQuantity();
                item.getUnitPrice();
                item.getSubtotal();

                if (item.getProduct() != null) {
                    item.getProduct().getId();
                    item.getProduct().getName();
                    item.getProduct().getCode();
                }
            }
        }

        return order;
    }

    @Transactional
    public Order updateStatusForApi(AppUser owner, Long id, String newStatus) {
        if (owner == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập");
        }

        if (id == null) {
            throw new IllegalArgumentException("Mã đơn hàng không hợp lệ");
        }

        String status = normalizeStatus(newStatus);

        Order order = orderRepository.findByIdAndUser(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        String oldStatus = order.getStatus();

        if (STATUS_CANCELLED.equals(status) && !STATUS_CANCELLED.equals(oldStatus)) {
            restoreOrderStock(order);
        } else if (STATUS_CANCELLED.equals(oldStatus) && !STATUS_CANCELLED.equals(status)) {
            decreaseOrderStock(order);
        }

        order.setStatus(status);

        Order saved = orderRepository.save(order);

        if (saved.getItems() != null) {
            saved.getItems().size();

            for (OrderItem item : saved.getItems()) {
                if (item.getProduct() != null) {
                    item.getProduct().getName();
                    item.getProduct().getCode();
                }
            }
        }

        return saved;
    }

    public Page<Order> filterOrdersPaged(String keyword, String status, int page, int size) {
        return orderRepository.filterOrdersPaged(
                owner(),
                keyword == null ? "" : keyword.trim(),
                status == null ? "" : status.trim(),
                PageRequest.of(page, size)
        );
    }

    public List<Order> getAllOrders() {
        return orderRepository.findByUserOrderByIdDesc(owner());
    }

    public Order getById(Long id) {
        return orderRepository.findByIdAndUser(id, owner())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
    }

    public long countOrders() {
        return orderRepository.countByUser(owner());
    }

    public long countByStatus(String status) {
        return orderRepository.countByUserAndStatus(owner(), status);
    }

    public BigDecimal totalRevenue() {
        BigDecimal r = orderRepository.sumRevenueByUser(owner());
        return r == null ? BigDecimal.ZERO : r;
    }

    @Transactional
    public void createOrder(String customerName,
                            String customerPhone,
                            String customerAddress,
                            List<Long> productIds,
                            List<Integer> quantities) {
        createOrderDetailed(
                customerName,
                customerPhone,
                customerAddress,
                STATUS_PENDING,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                productIds,
                quantities,
                Collections.emptyList()
        );
    }

    @Transactional
    public Order createOrderDetailed(String customerName,
                                     String customerPhone,
                                     String customerAddress,
                                     String status,
                                     BigDecimal shippingFee,
                                     BigDecimal customerDeposit,
                                     List<Long> productIds,
                                     List<Integer> quantities,
                                     List<BigDecimal> unitPrices) {
        AppUser ownerUser = owner();

        validateCustomerInfo(customerName, customerPhone);

        Order order = new Order();
        order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerName(customerName == null ? "" : customerName.trim());
        order.setCustomerPhone(customerPhone == null ? "" : customerPhone.trim());
        order.setCustomerAddress(customerAddress == null ? "" : customerAddress.trim());
        order.setStatus(normalizeStatus(status));
        order.setShippingFee(money(shippingFee));
        order.setCustomerDeposit(money(customerDeposit));
        order.setUser(ownerUser);

        rebuildOrderItems(order, ownerUser, order.getStatus(), productIds, quantities, unitPrices, false);

        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrderInfo(Long orderId,
                                 String customerName,
                                 String customerPhone,
                                 String customerAddress,
                                 String status,
                                 BigDecimal shippingFee,
                                 BigDecimal customerDeposit,
                                 List<Long> productIds,
                                 List<Integer> quantities,
                                 List<BigDecimal> unitPrices) {
        validateCustomerInfo(customerName, customerPhone);

        Order order = getById(orderId);
        AppUser ownerUser = order.getUser() == null ? owner() : order.getUser();
        String newStatus = normalizeStatus(status);

        if (!STATUS_CANCELLED.equals(order.getStatus())) {
            restoreOrderStock(order);
        }

        order.getItems().clear();

        order.setCustomerName(customerName == null ? "" : customerName.trim());
        order.setCustomerPhone(customerPhone == null ? "" : customerPhone.trim());
        order.setCustomerAddress(customerAddress == null ? "" : customerAddress.trim());
        order.setStatus(newStatus);
        order.setShippingFee(money(shippingFee));
        order.setCustomerDeposit(money(customerDeposit));

        rebuildOrderItems(order, ownerUser, newStatus, productIds, quantities, unitPrices, false);

        return orderRepository.save(order);
    }

    private void rebuildOrderItems(Order order,
                                   AppUser ownerUser,
                                   String status,
                                   List<Long> productIds,
                                   List<Integer> quantities,
                                   List<BigDecimal> unitPrices,
                                   boolean append) {
        if (!append) {
            order.getItems().clear();
        }

        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một sản phẩm.");
        }

        BigDecimal productTotal = BigDecimal.ZERO;
        int validLineCount = 0;

        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);

            if (productId == null) {
                continue;
            }

            int quantity = 0;

            if (quantities != null && i < quantities.size() && quantities.get(i) != null) {
                quantity = quantities.get(i);
            }

            if (quantity <= 0) {
                continue;
            }

            Product product = productService.getById(productId, ownerUser);

            int stock = product.getQuantity() == null ? 0 : product.getQuantity();

            if (!STATUS_CANCELLED.equals(status) && stock < quantity) {
                throw new IllegalArgumentException(
                        "Sản phẩm " + product.getName() + " chỉ còn " + stock + ", không thể xuất " + quantity
                );
            }

            BigDecimal originalPrice = money(product.getSalePrice());
            BigDecimal unitPrice = originalPrice;

            if (unitPrices != null && i < unitPrices.size()) {
                BigDecimal submittedPrice = money(unitPrices.get(i));

                if (submittedPrice.signum() > 0) {
                    unitPrice = submittedPrice;
                }
            }

            BigDecimal costPrice = money(product.getImportPrice());

            if (!STATUS_CANCELLED.equals(status)) {
                productService.decreaseStockForSale(product, quantity);
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setOriginalPrice(originalPrice);
            item.setUnitPrice(unitPrice);
            item.setCostPrice(costPrice);
            item.recalculate();

            order.getItems().add(item);

            productTotal = productTotal.add(item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal());
            validLineCount++;
        }

        if (validLineCount == 0) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một sản phẩm hợp lệ và nhập số lượng lớn hơn 0.");
        }

        order.setTotalAmount(productTotal);
        order.setTotalBill(productTotal);
        order.recalculateMoneyFields();
    }

    private void validateCustomerInfo(String customerName, String customerPhone) {
        if (!StringUtils.hasText(customerName)) {
            throw new IllegalArgumentException("Vui lòng nhập tên khách hàng");
        }

        if (!StringUtils.hasText(customerPhone)) {
            throw new IllegalArgumentException("Vui lòng nhập số điện thoại");
        }
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return STATUS_PENDING;
        }

        String value = status.trim();

        List<String> allowed = Arrays.asList(
                STATUS_PENDING,
                STATUS_SHIPPING,
                STATUS_COMPLETED,
                STATUS_DELIVERED,
                STATUS_CANCELLED
        );

        if (!allowed.contains(value)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ: " + value);
        }

        return value;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    @Transactional
    public Order createPublicOrder(String customerName,
                                   String customerPhone,
                                   String customerAddress,
                                   String note,
                                   Map<Long, Integer> cartItems) {
        AppUser ownerUser = publicOwner();

        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống");
        }

        Order order = new Order();
        order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerName(customerName == null ? "" : customerName.trim());
        order.setCustomerPhone(customerPhone == null ? "" : customerPhone.trim());
        order.setCustomerAddress(customerAddress == null ? "" : customerAddress.trim());
        order.setStatus(STATUS_PENDING);
        order.setUser(ownerUser);

        order = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : cartItems.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue() == null ? 0 : entry.getValue();

            if (productId == null || quantity <= 0) {
                continue;
            }

            Product product = productService.getPublicProductById(productId);

            int stock = product.getQuantity() == null ? 0 : product.getQuantity();

            if (stock < quantity) {
                throw new IllegalArgumentException(
                        "Sản phẩm '" + product.getName() + "' chỉ còn " + stock
                );
            }

            BigDecimal originalPrice = money(product.getSalePrice());
            BigDecimal unitPrice = money(product.getEffectiveSalePrice());

            if (unitPrice.signum() <= 0) {
                unitPrice = originalPrice;
            }

            BigDecimal costPrice = money(product.getImportPrice());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setOriginalPrice(originalPrice);
            item.setUnitPrice(unitPrice);
            item.setCostPrice(costPrice);
            item.recalculate();

            order.getItems().add(item);

            total = total.add(item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal());

            productService.decreaseStockForSale(product, quantity);
        }

        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng không có sản phẩm hợp lệ");
        }

        order.setTotalAmount(total);
        order.setTotalBill(total);
        order.recalculateMoneyFields();

        return orderRepository.save(order);
    }

    @Transactional
    public void updateStatus(Long orderId, String newStatus) {
        Order order = getById(orderId);
        String status = normalizeStatus(newStatus);
        String oldStatus = order.getStatus();

        if (STATUS_CANCELLED.equals(status) && !STATUS_CANCELLED.equals(oldStatus)) {
            restoreOrderStock(order);
        } else if (STATUS_CANCELLED.equals(oldStatus) && !STATUS_CANCELLED.equals(status)) {
            decreaseOrderStock(order);
        }

        order.setStatus(status);
        orderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        Order order = getById(id);

        if (!STATUS_CANCELLED.equals(order.getStatus())) {
            restoreOrderStock(order);
        }

        orderRepository.delete(order);
    }

    @Transactional
    public void deleteAll() {
        List<Order> orders = orderRepository.findByUserOrderByIdDesc(owner());

        for (Order order : orders) {
            if (!STATUS_CANCELLED.equals(order.getStatus())) {
                restoreOrderStock(order);
            }

            orderRepository.delete(order);
        }
    }

    public List<Object[]> getBestSellingProducts() {
        return orderItemRepository.findBestSellingProducts(owner(), PageRequest.of(0, 10));
    }

    public Map<String, BigDecimal> revenueByMonth() {
        Map<String, BigDecimal> result = new LinkedHashMap<>();

        for (int i = 1; i <= 12; i++) {
            result.put("Tháng " + i, BigDecimal.ZERO);
        }

        getAllOrders().stream()
                .filter(order -> STATUS_COMPLETED.equals(order.getStatus()) || STATUS_DELIVERED.equals(order.getStatus()))
                .filter(order -> order.getCreatedAt() != null)
                .forEach(order -> {
                    String key = "Tháng " + order.getCreatedAt().getMonthValue();
                    BigDecimal total = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
                    result.put(key, result.get(key).add(total));
                });

        return result;
    }

    public Map<String, Long> orderStatusStatistics() {
        Map<String, Long> result = new LinkedHashMap<>();

        result.put("Chờ xác nhận", countByStatus(STATUS_PENDING));
        result.put("Đang giao", countByStatus(STATUS_SHIPPING));
        result.put("Hoàn thành", countByStatus(STATUS_COMPLETED));
        result.put("Đã giao", countByStatus(STATUS_DELIVERED));
        result.put("Đã hủy", countByStatus(STATUS_CANCELLED));

        return result;
    }
    public List<DailyTrendDTO> getLast31DaysTrend(LocalDate selectedDate) {
        AppUser ownerUser = owner();

        LocalDate endDate = selectedDate == null
                ? LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                : selectedDate;

        LocalDate startDate = endDate.minusDays(30);

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();

        List<Object[]> rows = orderRepository.findDailyOrderRevenueTrend(ownerUser, start, endExclusive);

        Map<LocalDate, DailyTrendDTO> resultMap = new LinkedHashMap<>();

        for (int i = 0; i <= 30; i++) {
            LocalDate date = startDate.plusDays(i);
            resultMap.put(date, new DailyTrendDTO(date, 0, BigDecimal.ZERO));
        }

        if (rows != null) {
            for (Object[] row : rows) {
                if (row == null || row.length < 3 || row[0] == null) {
                    continue;
                }

                LocalDate date = convertToLocalDate(row[0]);

                if (date == null || !resultMap.containsKey(date)) {
                    continue;
                }

                long orderCount = row[1] == null ? 0L : ((Number) row[1]).longValue();
                BigDecimal revenue = row[2] == null ? BigDecimal.ZERO : new BigDecimal(row[2].toString());

                resultMap.put(date, new DailyTrendDTO(date, orderCount, revenue));
            }
        }

        return new ArrayList<>(resultMap.values());
    }

    private LocalDate convertToLocalDate(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }

        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }

        if (value instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) value).getTime()).toLocalDate();
        }

        return LocalDate.parse(value.toString());
    }
    public DailyReportDTO getDailyReport(LocalDate date) {
        AppUser ownerUser = owner();

        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(LocalTime.MAX);

        DailyReportDTO report = new DailyReportDTO();
        report.setDate(date);

        report.setTotalOrders(orderRepository.countByUserAndDateRange(ownerUser, from, to));
        report.setPendingOrders(orderRepository.countByUserAndStatusAndDateRange(ownerUser, STATUS_PENDING, from, to));
        report.setShippingOrders(orderRepository.countByUserAndStatusAndDateRange(ownerUser, STATUS_SHIPPING, from, to));

        report.setCompletedOrders(
                orderRepository.countByUserAndStatusAndDateRange(ownerUser, STATUS_COMPLETED, from, to)
                        + orderRepository.countByUserAndStatusAndDateRange(ownerUser, STATUS_DELIVERED, from, to)
        );

        report.setCancelledOrders(orderRepository.countByUserAndStatusAndDateRange(ownerUser, STATUS_CANCELLED, from, to));

        BigDecimal revenue = orderRepository.sumRevenueByUserAndDateRange(ownerUser, from, to);
        report.setRevenue(revenue == null ? BigDecimal.ZERO : revenue);

        BigDecimal cogsBd = orderItemRepository.sumCostOfGoodsByDateRange(ownerUser, from, to);
        BigDecimal cogs = cogsBd == null ? BigDecimal.ZERO : cogsBd;
        report.setCostOfGoods(cogs);
        report.setGrossProfit(report.getRevenue().subtract(cogs));

        Long qtySold = orderItemRepository.sumQtySoldByDateRange(ownerUser, from, to);
        report.setTotalItemsSold(qtySold == null ? 0 : qtySold);

        BigDecimal importTotal = stockImportRepository.sumImportTotalByDateRange(ownerUser, from, to);
        report.setImportTotal(importTotal == null ? BigDecimal.ZERO : importTotal);

        Long qtyImported = stockImportRepository.sumImportQtyByDateRange(ownerUser, from, to);
        report.setTotalItemsImported(qtyImported == null ? 0 : qtyImported);

        List<DailyReportDTO.OrderSummary> orderSummaries = new ArrayList<>();
        List<Order> dayOrders = orderRepository.findByUserAndDateRange(ownerUser, from, to);

        for (Order order : dayOrders) {
            orderSummaries.add(new DailyReportDTO.OrderSummary(
                    order.getOrderCode(),
                    order.getCustomerName(),
                    order.getStatus(),
                    order.getTotalBill(),
                    order.getShippingFee()
            ));
        }

        report.setOrderSummaries(orderSummaries);

        List<DailyReportDTO.ProductSummary> productSummaries = new ArrayList<>();
        List<Object[]> rows = orderItemRepository.findProductSummaryByDateRange(ownerUser, from, to);

        for (Object[] row : rows) {
            String productName = (String) row[0];
            long quantity = row[1] == null ? 0 : ((Number) row[1]).longValue();
            BigDecimal revenueValue = row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2];
            BigDecimal costValue = row[3] == null ? BigDecimal.ZERO : (BigDecimal) row[3];
            BigDecimal profitValue = row[4] == null ? BigDecimal.ZERO : (BigDecimal) row[4];

            productSummaries.add(new DailyReportDTO.ProductSummary(
                    productName,
                    quantity,
                    revenueValue,
                    costValue,
                    profitValue
            ));
        }

        report.setProductSummaries(productSummaries);

        return report;
    }

    private void restoreOrderStock(Order order) {
        if (order == null || order.getItems() == null) {
            return;
        }

        for (OrderItem item : order.getItems()) {
            if (item == null || item.getProduct() == null || item.getQuantity() == null) {
                continue;
            }

            int quantity = item.getQuantity();

            if (quantity <= 0) {
                continue;
            }

            try {
                productService.restoreStockFromSale(item.getProduct(), quantity);
            } catch (Exception ignored) {
            }
        }
    }

    private void decreaseOrderStock(Order order) {
        if (order == null || order.getItems() == null) {
            return;
        }

        for (OrderItem item : order.getItems()) {
            if (item == null || item.getProduct() == null || item.getQuantity() == null) {
                continue;
            }

            int quantity = item.getQuantity();

            if (quantity <= 0) {
                continue;
            }

            productService.decreaseStockForSale(item.getProduct(), quantity);
        }
    }
}