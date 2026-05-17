package entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false)
    private String orderCode;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_address")
    private String customerAddress;

    /*
     * Tổng tiền sản phẩm hoặc tổng bill cuối cùng.
     * Giữ field này để không làm vỡ code cũ.
     */
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /*
     * Phí ship import từ Google Sheet.
     */
    @Column(name = "shipping_fee", precision = 15, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    /*
     * Tổng bill cuối cùng import từ Google Sheet.
     * Nếu Sheet không có tổng bill thì hệ thống tự tính = tổng sản phẩm + phí ship.
     */
    @Column(name = "total_bill", precision = 15, scale = 2)
    private BigDecimal totalBill = BigDecimal.ZERO;

    /*
     * Số tiền khách đã cọc.
     */
    @Column(name = "customer_deposit", precision = 15, scale = 2)
    private BigDecimal customerDeposit = BigDecimal.ZERO;

    /*
     * Số tiền còn lại cần thu = tổng bill - khách cọc.
     */
    @Column(name = "remaining_amount", precision = 15, scale = 2)
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    @Transient
    private boolean totalBillExplicit;

    @Column(nullable = false)
    private String status = "CHỜ_XÁC_NHẬN";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    @PostLoad
    public void postLoad() {
        totalBillExplicit = totalBill != null && totalBill.signum() > 0;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null || status.isBlank()) {
            status = "CHỜ_XÁC_NHẬN";
        }

        recalculateMoneyFields();
    }

    @PreUpdate
    public void preUpdate() {
        recalculateMoneyFields();
    }

    public void recalculateMoneyFields() {
        totalAmount = money(totalAmount);
        shippingFee = money(shippingFee);
        totalBill = money(totalBill);
        customerDeposit = money(customerDeposit);

        if (!totalBillExplicit || totalBill.signum() == 0) {
            totalBill = totalAmount.add(shippingFee);
        }

        if (totalAmount.signum() == 0) {
            totalAmount = totalBill;
        }

        remainingAmount = totalBill.subtract(customerDeposit);

        if (remainingAmount.signum() < 0) {
            remainingAmount = BigDecimal.ZERO;
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    public Long getId() {
        return id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount == null ? BigDecimal.ZERO : totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = money(totalAmount);
        recalculateMoneyFields();
    }

    public BigDecimal getShippingFee() {
        return shippingFee == null ? BigDecimal.ZERO : shippingFee;
    }

    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = money(shippingFee);
        recalculateMoneyFields();
    }

    public BigDecimal getTotalBill() {
        recalculateMoneyFields();
        return totalBill;
    }

    public void setTotalBill(BigDecimal totalBill) {
        this.totalBill = money(totalBill);
        this.totalBillExplicit = this.totalBill.signum() > 0;
        recalculateMoneyFields();
    }

    public BigDecimal getCustomerDeposit() {
        return customerDeposit == null ? BigDecimal.ZERO : customerDeposit;
    }

    public void setCustomerDeposit(BigDecimal customerDeposit) {
        this.customerDeposit = money(customerDeposit);
        recalculateMoneyFields();
    }

    public BigDecimal getRemainingAmount() {
        recalculateMoneyFields();
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = money(remainingAmount);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }
}