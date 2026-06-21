package dto;

import entity.Order;
import entity.OrderItem;
import entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderApiResponse {

    private Long id;
    private String orderCode;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private String status;

    private BigDecimal totalAmount;
    private BigDecimal totalBill;
    private BigDecimal shippingFee;
    private BigDecimal customerDeposit;
    private BigDecimal remainingAmount;

    private LocalDateTime createdAt;

    private List<OrderItemApiResponse> items = new ArrayList<>();

    public OrderApiResponse() {
    }

    public OrderApiResponse(Order order) {
        if (order == null) {
            return;
        }

        this.id = order.getId();
        this.orderCode = order.getOrderCode();
        this.customerName = order.getCustomerName();
        this.customerPhone = order.getCustomerPhone();
        this.customerAddress = order.getCustomerAddress();
        this.status = order.getStatus();

        this.totalAmount = safe(order.getTotalAmount());
        this.totalBill = safe(order.getTotalBill());
        this.shippingFee = safe(order.getShippingFee());
        this.customerDeposit = safe(order.getCustomerDeposit());
        this.remainingAmount = safe(order.getRemainingAmount());

        this.createdAt = order.getCreatedAt();

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item != null) {
                    this.items.add(new OrderItemApiResponse(item));
                }
            }
        }
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public Long getId() {
        return id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getTotalBill() {
        return totalBill;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public BigDecimal getCustomerDeposit() {
        return customerDeposit;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<OrderItemApiResponse> getItems() {
        return items;
    }

    public static class OrderItemApiResponse {

        private Long id;
        private Long productId;
        private String productName;
        private String productCode;
        private Integer quantity;

        private BigDecimal originalPrice;
        private BigDecimal unitPrice;
        private BigDecimal costPrice;
        private BigDecimal subtotal;
        private BigDecimal profit;

        public OrderItemApiResponse() {
        }

        public OrderItemApiResponse(OrderItem item) {
            this.id = item.getId();

            Product product = item.getProduct();

            if (product != null) {
                this.productId = product.getId();
                this.productName = product.getName();
                this.productCode = product.getCode();
            } else {
                this.productName = "Sản phẩm";
            }

            this.quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            this.originalPrice = safe(item.getOriginalPrice());
            this.unitPrice = safe(item.getUnitPrice());
            this.costPrice = safe(item.getCostPrice());
            this.subtotal = safe(item.getSubtotal());
            this.profit = safe(item.getProfit());

            if (this.subtotal.compareTo(BigDecimal.ZERO) == 0
                    && this.unitPrice.compareTo(BigDecimal.ZERO) > 0
                    && this.quantity > 0) {
                this.subtotal = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
            }
        }

        private BigDecimal safe(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }

        public Long getId() {
            return id;
        }

        public Long getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public String getProductCode() {
            return productCode;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public BigDecimal getOriginalPrice() {
            return originalPrice;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public BigDecimal getCostPrice() {
            return costPrice;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public BigDecimal getProfit() {
            return profit;
        }
    }
}