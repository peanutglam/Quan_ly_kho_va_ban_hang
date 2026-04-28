package service;

import entity.AppUser;
import entity.Order;
import entity.OrderItem;
import entity.Product;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import repository.OrderItemRepository;
import repository.OrderRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public List<Order> getAllOrders() {
        AppUser owner = authService.getWorkspaceOwner();
        return orderRepository.findByUserOrderByIdDesc(owner);
    }

    public List<Order> filterOrders(String keyword, String status) {
        List<Order> orders = getAllOrders();

        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase();

            orders = orders.stream().filter(o ->
                    (o.getOrderCode() != null && o.getOrderCode().toLowerCase().contains(kw)) ||
                            (o.getCustomerName() != null && o.getCustomerName().toLowerCase().contains(kw)) ||
                            (o.getCustomerPhone() != null && o.getCustomerPhone().toLowerCase().contains(kw))
            ).toList();
        }

        if (StringUtils.hasText(status)) {
            orders = orders.stream()
                    .filter(o -> status.equals(o.getStatus()))
                    .toList();
        }

        return orders;
    }

    public Order getById(Long id) {
        AppUser owner = authService.getWorkspaceOwner();

        return orderRepository.findByIdAndUser(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng trong web của Owner này"));
    }

    public long countOrders() {
        AppUser owner = authService.getWorkspaceOwner();
        return orderRepository.countByUser(owner);
    }

    public long countByStatus(String status) {
        AppUser owner = authService.getWorkspaceOwner();
        return orderRepository.countByUserAndStatus(owner, status);
    }

    public BigDecimal totalRevenue() {
        return getAllOrders().stream()
                .filter(o -> STATUS_COMPLETED.equals(o.getStatus()) || STATUS_DELIVERED.equals(o.getStatus()))
                .map(Order::getTotalAmount)
                .filter(t -> t != null)
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
        orderRepository.save(order);
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

        if (!STATUS_CANCELLED.equals(order.getStatus())) {
            restoreOrderStock(order);
        }

        orderRepository.delete(order);
    }

    @Transactional
    public void deleteAll() {
        AppUser owner = authService.getWorkspaceOwner();

        List<Order> orders = orderRepository.findByUserOrderByIdDesc(owner);

        for (Order order : orders) {
            if (!STATUS_CANCELLED.equals(order.getStatus())) {
                restoreOrderStock(order);
            }

            orderRepository.delete(order);
        }
    }

    public List<Object[]> getBestSellingProducts() {
        AppUser owner = authService.getWorkspaceOwner();
        return orderItemRepository.findBestSellingProducts(owner, PageRequest.of(0, 10));
    }

    public Map<String, BigDecimal> revenueByMonth() {
        Map<String, BigDecimal> result = new LinkedHashMap<>();

        for (int i = 1; i <= 12; i++) {
            result.put("Tháng " + i, BigDecimal.ZERO);
        }

        getAllOrders().stream()
                .filter(o -> STATUS_COMPLETED.equals(o.getStatus()) || STATUS_DELIVERED.equals(o.getStatus()))
                .filter(o -> o.getCreatedAt() != null)
                .forEach(o -> {
                    String key = "Tháng " + o.getCreatedAt().getMonthValue();
                    BigDecimal total = o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount();
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

    private void restoreOrderStock(Order order) {
        for (OrderItem item : order.getItems()) {
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            productService.restoreStockFromSale(item.getProduct(), quantity);
        }
    }

    private void decreaseOrderStock(Order order) {
        for (OrderItem item : order.getItems()) {
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            productService.decreaseStockForSale(item.getProduct(), quantity);
        }
    }

    private record OrderLine(Product product, int quantity) {
    }
}