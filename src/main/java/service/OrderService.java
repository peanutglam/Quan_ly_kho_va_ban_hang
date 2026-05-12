package service;

import entity.AppUser;
import entity.Order;
import entity.OrderItem;
import entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import repository.OrderItemRepository;
import repository.OrderRepository;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    public static final String STATUS_PENDING = "CHỜ_XÁC_NHẬN";
    public static final String STATUS_SHIPPING = "ĐANG_GIAO";
    public static final String STATUS_COMPLETED = "HOÀN_THÀNH";
    public static final String STATUS_DELIVERED = "ĐÃ_GIAO";
    public static final String STATUS_CANCELLED = "ĐÃ_HỦY";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;
    private final AuthService authService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        ProductService productService,
                        AuthService authService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productService = productService;
        this.authService = authService;
    }

    /*
     * Đề tài hiện tại: 1 ứng dụng = 1 cửa hàng.
     * Vì vậy phần đọc/hiển thị đơn hàng không phụ thuộc user_id nữa.
     * Điều này tránh lỗi vừa login dashboard hiện 0 nhưng DB vẫn có dữ liệu.
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return allOrdersSortedDesc();
    }

    @Transactional(readOnly = true)
    public Page<Order> filterOrdersPaged(String keyword,
                                         String status,
                                         int page,
                                         int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 30 : Math.min(size, 30);

        String kw = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : "";
        String st = StringUtils.hasText(status) ? status.trim() : "";

        List<Order> filtered = allOrdersSortedDesc();

        if (StringUtils.hasText(kw)) {
            filtered = filtered.stream()
                    .filter(o ->
                            containsIgnoreCase(o.getOrderCode(), kw)
                                    || containsIgnoreCase(o.getCustomerName(), kw)
                                    || containsIgnoreCase(o.getCustomerPhone(), kw)
                                    || containsIgnoreCase(o.getCustomerAddress(), kw)
                    )
                    .toList();
        }

        if (StringUtils.hasText(st)) {
            filtered = filtered.stream()
                    .filter(o -> st.equals(o.getStatus()))
                    .toList();
        }

        int start = safePage * safeSize;
        Pageable pageable = PageRequest.of(safePage, safeSize);

        if (start >= filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }

        int end = Math.min(start + safeSize, filtered.size());

        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public Order getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
    }

    @Transactional(readOnly = true)
    public long countOrders() {
        return orderRepository.count();
    }

    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return 0;
        }

        return allOrdersSortedDesc().stream()
                .filter(o -> status.equals(o.getStatus()))
                .count();
    }

    @Transactional(readOnly = true)
    public BigDecimal totalRevenue() {
        return allOrdersSortedDesc().stream()
                .filter(o -> STATUS_COMPLETED.equals(o.getStatus()) || STATUS_DELIVERED.equals(o.getStatus()))
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void createOrder(String customerName,
                            String customerPhone,
                            String customerAddress,
                            List<Long> productIds,
                            List<Integer> quantities) {
        AppUser owner = authService.getWorkspaceOwner();

        if (!StringUtils.hasText(customerName)) {
            throw new IllegalArgumentException("Vui lòng nhập tên khách hàng");
        }

        if (!StringUtils.hasText(customerPhone)) {
            throw new IllegalArgumentException("Vui lòng nhập số điện thoại");
        }

        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một sản phẩm");
        }

        Map<Long, Integer> requestedItems = new LinkedHashMap<>();

        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            Integer quantity = quantities != null && quantities.size() > i ? quantities.get(i) : 0;

            if (productId == null || quantity == null || quantity <= 0) {
                continue;
            }

            requestedItems.merge(productId, quantity, Integer::sum);
        }

        if (requestedItems.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số lượng bán hợp lệ");
        }

        List<OrderLine> orderLines = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : requestedItems.entrySet()) {
            Product product = productService.getById(entry.getKey(), owner);
            int quantity = entry.getValue();

            if (product.getQuantity() < quantity) {
                throw new IllegalArgumentException(
                        "Sản phẩm '" + product.getName() + "' không đủ tồn kho, hiện còn " + product.getQuantity()
                );
            }

            orderLines.add(new OrderLine(product, quantity));
        }

        Order order = new Order();
        order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerName(customerName.trim());
        order.setCustomerPhone(customerPhone.trim());
        order.setCustomerAddress(customerAddress == null ? "" : customerAddress.trim());
        order.setStatus(STATUS_PENDING);
        order.setUser(owner);

        BigDecimal total = BigDecimal.ZERO;

        for (OrderLine line : orderLines) {
            Product product = line.product();
            int quantity = line.quantity();

            BigDecimal unitPrice = product.getSalePrice() == null ? BigDecimal.ZERO : product.getSalePrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);

            order.getItems().add(item);

            total = total.add(subtotal);

            productService.decreaseStockForSale(product, quantity);
        }

        order.setTotalAmount(total);
        order.setTotalBill(total);
        order.recalculateMoneyFields();

        orderRepository.save(order);
    }

    @Transactional
    public void updateStatus(Long orderId, String newStatus) {
        if (!StringUtils.hasText(newStatus)) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ");
        }

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

        if (!STATUS_CANCELLED.equals(order.getStatus())) {
            restoreOrderStock(order);
        }

        orderRepository.delete(order);
    }

    @Transactional
    public void deleteAll() {
        List<Order> orders = allOrdersSortedDesc();

        for (Order order : orders) {
            if (!STATUS_CANCELLED.equals(order.getStatus())) {
                restoreOrderStock(order);
            }

            orderRepository.delete(order);
        }
    }

    @Transactional(readOnly = true)
    public List<Object[]> getBestSellingProducts() {
        Map<Long, BestSellingStat> statMap = new LinkedHashMap<>();

        for (Order order : allOrdersSortedDesc()) {
            if (STATUS_CANCELLED.equals(order.getStatus())) {
                continue;
            }

            if (order.getItems() == null) {
                continue;
            }

            for (OrderItem item : order.getItems()) {
                if (item == null || item.getProduct() == null) {
                    continue;
                }

                Product product = item.getProduct();
                Long productId = product.getId();

                if (productId == null) {
                    continue;
                }

                int quantity = item.getQuantity() == null ? 0 : item.getQuantity();

                BigDecimal revenue = BigDecimal.ZERO;

                if (item.getSubtotal() != null && item.getSubtotal().signum() > 0) {
                    revenue = item.getSubtotal();
                } else if (item.getDisplaySubtotal() != null) {
                    revenue = item.getDisplaySubtotal();
                } else if (order.getTotalAmount() != null) {
                    revenue = order.getTotalAmount();
                }

                BestSellingStat stat = statMap.computeIfAbsent(
                        productId,
                        id -> new BestSellingStat(product.getName(), 0L, BigDecimal.ZERO)
                );

                stat.quantity += quantity;
                stat.revenue = stat.revenue.add(revenue);
            }
        }

        return statMap.values()
                .stream()
                .sorted(
                        Comparator.comparingLong(BestSellingStat::quantity).reversed()
                                .thenComparing(BestSellingStat::revenue, Comparator.reverseOrder())
                )
                .map(stat -> new Object[]{stat.productName, stat.quantity, stat.revenue})
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> revenueByMonth() {
        Map<String, BigDecimal> result = new LinkedHashMap<>();

        for (int i = 1; i <= 12; i++) {
            result.put("Tháng " + i, BigDecimal.ZERO);
        }

        allOrdersSortedDesc().stream()
                .filter(o -> STATUS_COMPLETED.equals(o.getStatus()) || STATUS_DELIVERED.equals(o.getStatus()))
                .filter(o -> o.getCreatedAt() != null)
                .forEach(o -> {
                    String key = "Tháng " + o.getCreatedAt().getMonthValue();
                    BigDecimal total = o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount();
                    result.put(key, result.get(key).add(total));
                });

        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> orderStatusStatistics() {
        Map<String, Long> result = new LinkedHashMap<>();

        result.put("Chờ xác nhận", countByStatus(STATUS_PENDING));
        result.put("Đang giao", countByStatus(STATUS_SHIPPING));
        result.put("Hoàn thành", countByStatus(STATUS_COMPLETED));
        result.put("Đã giao", countByStatus(STATUS_DELIVERED));
        result.put("Đã hủy", countByStatus(STATUS_CANCELLED));

        return result;
    }

    private List<Order> allOrdersSortedDesc() {
        return orderRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Order::getId, Comparator.nullsLast(Long::compareTo)).reversed())
                .toList();
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private void restoreOrderStock(Order order) {
        if (order == null || order.getItems() == null) {
            return;
        }

        for (OrderItem item : order.getItems()) {
            if (item == null || item.getProduct() == null) {
                continue;
            }

            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();

            if (quantity <= 0) {
                continue;
            }

            productService.restoreStockFromSale(item.getProduct(), quantity);
        }
    }

    private void decreaseOrderStock(Order order) {
        if (order == null || order.getItems() == null) {
            return;
        }

        for (OrderItem item : order.getItems()) {
            if (item == null || item.getProduct() == null) {
                continue;
            }

            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();

            if (quantity <= 0) {
                continue;
            }

            Product product = item.getProduct();

            if (product.getQuantity() < quantity) {
                throw new IllegalArgumentException(
                        "Không thể khôi phục trạng thái đơn hàng vì sản phẩm '" +
                                product.getName() +
                                "' không đủ tồn kho. Hiện còn " +
                                product.getQuantity()
                );
            }

            productService.decreaseStockForSale(product, quantity);
        }
    }

    private record OrderLine(Product product, int quantity) {
    }

    private static class BestSellingStat {
        private final String productName;
        private long quantity;
        private BigDecimal revenue;

        private BestSellingStat(String productName, long quantity, BigDecimal revenue) {
            this.productName = productName;
            this.quantity = quantity;
            this.revenue = revenue;
        }

        private long quantity() {
            return quantity;
        }

        private BigDecimal revenue() {
            return revenue;
        }
    }
}