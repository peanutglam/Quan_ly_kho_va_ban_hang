package entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    public static final String STATUS_PENDING = "CHỜ_XÁC_NHẬN";
    public static final String STATUS_SHIPPING = "ĐANG_GIAO";
    public static final String STATUS_DELIVERED = "ĐÃ_GIAO";
    public static final String STATUS_COMPLETED = "HOÀN_THÀNH";
    public static final String STATUS_CANCELLED = "ĐÃ_HỦY";

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
     * Tài khoản khách hàng đã đăng nhập.
     * Có thể null với đơn khách đặt ẩn danh hoặc import từ Google Sheet.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_account_id")
    private AppUser customerAccount;

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

    @Column(nullable = false)
    private String status = STATUS_PENDING;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    /*
     * Owner của cửa hàng.
     */
    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        }

        if (status == null || status.isBlank()) {
            status = STATUS_PENDING;
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

        if (totalBill.signum() == 0) {
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
        this.orderCode = trim(orderCode);
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = trim(customerName);
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = trim(customerPhone);
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = trim(customerAddress);
    }

    public AppUser getCustomerAccount() {
        return customerAccount;
    }

    public void setCustomerAccount(AppUser customerAccount) {
        this.customerAccount = customerAccount;
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

    public String getStatusDisplayName() {
        return status == null || status.isBlank() ? STATUS_PENDING : status;
    }

    public void setStatus(String status) {
        this.status = trim(status);
    }

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public boolean isCancelled() {
        return STATUS_CANCELLED.equals(status);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}