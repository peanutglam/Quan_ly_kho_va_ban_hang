package service;

import entity.*;
import dto.DailyReportDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import repository.OrderItemRepository;
import repository.OrderRepository;
import repository.StockImportRepository;

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

    private AppUser owner() { return authService.getWorkspaceOwner(); }
    private AppUser publicOwner() {
        return authService.getSystemOwner();
    }
    public Page<Order> filterOrdersPaged(String keyword, String status, int page, int size) {
        return orderRepository.filterOrdersPaged(owner(),
                keyword == null ? "" : keyword.trim(),
                status == null ? "" : status.trim(),
                PageRequest.of(page, size));
    }

    public List<Order> getAllOrders() {
        return orderRepository.findByUserOrderByIdDesc(owner());
    }

    public Order getById(Long id) {
        return orderRepository.findByIdAndUser(id, owner())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
    }

    public long countOrders() { return orderRepository.countByUser(owner()); }

    public long countByStatus(String status) { return orderRepository.countByUserAndStatus(owner(), status); }

    public BigDecimal totalRevenue() {
        BigDecimal r = orderRepository.sumRevenueByUser(owner());
        return r == null ? BigDecimal.ZERO : r;
    }

    @Transactional
    public void createOrder(String customerName, String customerPhone, String customerAddress,
                            List<Long> productIds, List<Integer> quantities) {
        AppUser ownerUser = owner();
        if (!StringUtils.hasText(customerName)) throw new IllegalArgumentException("Vui lòng nhập tên khách hàng");
        if (!StringUtils.hasText(customerPhone)) throw new IllegalArgumentException("Vui lòng nhập số điện thoại");
        if (productIds == null || productIds.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn sản phẩm");

        Order order = new Order();
        order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerName(customerName);
        order.setCustomerPhone(customerPhone);
        order.setCustomerAddress(customerAddress);
        order.setStatus(STATUS_PENDING);
        order.setUser(ownerUser);
        order = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < productIds.size(); i++) {
            Long pid = productIds.get(i);
            int qty = i < quantities.size() ? quantities.get(i) : 1;
            if (pid == null || qty <= 0) continue;

            Product product = productService.getById(pid, ownerUser);
            BigDecimal originalPrice = product.getSalePrice();
            BigDecimal unitPrice = product.getEffectiveSalePrice();
            BigDecimal costPrice = product.getImportPrice();
            BigDecimal subtotal  = unitPrice.multiply(BigDecimal.valueOf(qty));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(qty);
            item.setOriginalPrice(originalPrice);
            item.setUnitPrice(unitPrice);
            item.setCostPrice(costPrice);
            item.recalculate();
            order.getItems().add(item);
            total = total.add(subtotal);

            productService.decreaseStockForSale(product, qty);
        }

        order.setTotalAmount(total);
        order.setTotalBill(total);
        orderRepository.save(order);
    }

    /**
     * Tạo đơn hàng từ trang bán hàng công khai (không cần đăng nhập).
     * Owner của đơn hàng là owner duy nhất trong hệ thống.
     */
    @Transactional
    public Order createPublicOrder(String customerName, String customerPhone,
                                   String customerAddress, String note,
                                   Map<Long, Integer> cartItems) {
        AppUser ownerUser = publicOwner();
        if (cartItems == null || cartItems.isEmpty()) throw new IllegalArgumentException("Giỏ hàng trống");

        Order order = new Order();
        order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerName(customerName);
        order.setCustomerPhone(customerPhone);
        order.setCustomerAddress(customerAddress);
        order.setStatus(STATUS_PENDING);
        order.setUser(ownerUser);
        order = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : cartItems.entrySet()) {
            Long pid = entry.getKey();
            int qty = entry.getValue();
            if (qty <= 0) continue;

            Product product = productService.getPublicProductById(pid);
            if (product.getQuantity() < qty)
                throw new IllegalArgumentException("Sản phẩm '" + product.getName() + "' chỉ còn " + product.getQuantity());

            BigDecimal originalPrice = product.getSalePrice();
            BigDecimal unitPrice = product.getEffectiveSalePrice();
            BigDecimal costPrice = product.getImportPrice();

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(qty);
            item.setOriginalPrice(originalPrice);
            item.setUnitPrice(unitPrice);
            item.setCostPrice(costPrice);
            item.recalculate();
            order.getItems().add(item);
            total = total.add(item.getSubtotal());

            productService.decreaseStockForSale(product, qty);
        }

        order.setTotalAmount(total);
        order.setTotalBill(total);
        orderRepository.save(order);
        return order;
    }

    @Transactional
    public void updateStatus(Long orderId, String newStatus) {
        Order order = getById(orderId);
        String oldStatus = order.getStatus();
        if (STATUS_CANCELLED.equals(newStatus) && !STATUS_CANCELLED.equals(oldStatus)) {
            restoreOrderStock(order);
        } else if (STATUS_CANCELLED.equals(oldStatus) && !STATUS_CANCELLED.equals(newStatus)) {
            decreaseOrderStock(order);
        }
        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        Order order = getById(id);
        if (!STATUS_CANCELLED.equals(order.getStatus())) restoreOrderStock(order);
        orderRepository.delete(order);
    }

    @Transactional
    public void deleteAll() {
        List<Order> orders = orderRepository.findByUserOrderByIdDesc(owner());
        for (Order o : orders) {
            if (!STATUS_CANCELLED.equals(o.getStatus())) restoreOrderStock(o);
            orderRepository.delete(o);
        }
    }

    public List<Object[]> getBestSellingProducts() {
        return orderItemRepository.findBestSellingProducts(owner(), PageRequest.of(0, 10));
    }

    public Map<String, BigDecimal> revenueByMonth() {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) result.put("Tháng " + i, BigDecimal.ZERO);
        getAllOrders().stream()
                .filter(o -> STATUS_COMPLETED.equals(o.getStatus()) || STATUS_DELIVERED.equals(o.getStatus()))
                .filter(o -> o.getCreatedAt() != null)
                .forEach(o -> {
                    String k = "Tháng " + o.getCreatedAt().getMonthValue();
                    BigDecimal t = o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount();
                    result.put(k, result.get(k).add(t));
                });
        return result;
    }

    public Map<String, Long> orderStatusStatistics() {
        Map<String, Long> r = new LinkedHashMap<>();
        r.put("Chờ xác nhận", countByStatus(STATUS_PENDING));
        r.put("Đang giao", countByStatus(STATUS_SHIPPING));
        r.put("Hoàn thành", countByStatus(STATUS_COMPLETED));
        r.put("Đã giao", countByStatus(STATUS_DELIVERED));
        r.put("Đã hủy", countByStatus(STATUS_CANCELLED));
        return r;
    }

    /** Báo cáo cuối ngày - dùng query count/sum thay vì findAll. */
    public DailyReportDTO getDailyReport(LocalDate date) {
        AppUser ownerUser = owner();
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to   = date.atTime(LocalTime.MAX);

        DailyReportDTO report = new DailyReportDTO();
        report.setDate(date);

        report.setTotalOrders(   orderRepository.countByUserAndDateRange(ownerUser, from, to));
        report.setPendingOrders(  orderRepository.countByUserAndStatusAndDateRange(ownerUser, STATUS_PENDING, from, to));
        report.setShippingOrders( orderRepository.countByUserAndStatusAndDateRange(ownerUser, STATUS_SHIPPING, from, to));
        report.setCompletedOrders(orderRepository.countByUserAndStatusAndDateRange(ownerUser, STATUS_COMPLETED, from, to) +
                orderRepository.countByUserAndStatusAndDateRange(ownerUser, STATUS_DELIVERED, from, to));
        report.setCancelledOrders(orderRepository.countByUserAndStatusAndDateRange(ownerUser, STATUS_CANCELLED, from, to));

        BigDecimal revenue = orderRepository.sumRevenueByUserAndDateRange(ownerUser, from, to);
        report.setRevenue(revenue == null ? BigDecimal.ZERO : revenue);

        BigDecimal cogsBd = orderItemRepository.sumCostOfGoodsByDateRange(ownerUser, from, to);
        BigDecimal cogs   = cogsBd == null ? BigDecimal.ZERO : cogsBd;
        report.setCostOfGoods(cogs);
        report.setGrossProfit(report.getRevenue().subtract(cogs));

        Long qtySold = orderItemRepository.sumQtySoldByDateRange(ownerUser, from, to);
        report.setTotalItemsSold(qtySold == null ? 0 : qtySold);

        BigDecimal importTotal = stockImportRepository.sumImportTotalByDateRange(ownerUser, from, to);
        report.setImportTotal(importTotal == null ? BigDecimal.ZERO : importTotal);

        Long qtyImported = stockImportRepository.sumImportQtyByDateRange(ownerUser, from, to);
        report.setTotalItemsImported(qtyImported == null ? 0 : qtyImported);

        // Chi tiết đơn hàng trong ngày
        List<DailyReportDTO.OrderSummary> orderSummaries = new ArrayList<>();
        List<Order> dayOrders = orderRepository.findByUserAndDateRange(ownerUser, from, to);
        for (Order o : dayOrders) {
            orderSummaries.add(new DailyReportDTO.OrderSummary(
                    o.getOrderCode(), o.getCustomerName(), o.getStatus(),
                    o.getTotalBill(), o.getShippingFee()
            ));
        }
        report.setOrderSummaries(orderSummaries);

        // Chi tiết sản phẩm bán trong ngày
        List<DailyReportDTO.ProductSummary> productSummaries = new ArrayList<>();
        List<Object[]> rows = orderItemRepository.findProductSummaryByDateRange(ownerUser, from, to);
        for (Object[] row : rows) {
            String pname = (String) row[0];
            long qty = row[1] == null ? 0 : ((Number) row[1]).longValue();
            BigDecimal rev = row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2];
            BigDecimal cost = row[3] == null ? BigDecimal.ZERO : (BigDecimal) row[3];
            BigDecimal pft  = row[4] == null ? BigDecimal.ZERO : (BigDecimal) row[4];
            productSummaries.add(new DailyReportDTO.ProductSummary(pname, qty, rev, cost, pft));
        }
        report.setProductSummaries(productSummaries);

        return report;
    }

    private void restoreOrderStock(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null && item.getQuantity() != null) {
                try { productService.restoreStockFromSale(item.getProduct(), item.getQuantity()); }
                catch (Exception ignored) {}
            }
        }
    }

    private void decreaseOrderStock(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null && item.getQuantity() != null) {
                try { productService.decreaseStockForSale(item.getProduct(), item.getQuantity()); }
                catch (Exception ignored) {}
            }
        }
    }
}