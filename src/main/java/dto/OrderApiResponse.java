package dto;

import entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderApiResponse {

    private Long id;
    private String orderCode;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal customerDeposit;
    private LocalDateTime createdAt;

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
        this.totalAmount = order.getTotalAmount();
        this.shippingFee = order.getShippingFee();
        this.customerDeposit = order.getCustomerDeposit();
        this.createdAt = order.getCreatedAt();
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

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public BigDecimal getCustomerDeposit() {
        return customerDeposit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}