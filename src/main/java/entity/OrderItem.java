package entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private Integer quantity;

    /** Giá bán gốc (Product.salePrice tại thời điểm tạo đơn). */
    @Column(name = "original_price", precision = 15, scale = 2)
    private BigDecimal originalPrice = BigDecimal.ZERO;

    /** Giá bán thực tế sau giảm giá/chỉnh sửa. Mặc định = originalPrice. */
    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /** Giảm giá (số tiền). discountAmount = originalPrice - unitPrice >= 0. */
    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** Giá vốn tại thời điểm bán (Product.importPrice). Dùng cho báo cáo lãi/lỗ. */
    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    /** Thành tiền = unitPrice * quantity. */
    @Column(precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Lợi nhuận dòng = (unitPrice - costPrice) * quantity. */
    @Column(precision = 15, scale = 2)
    private BigDecimal profit = BigDecimal.ZERO;

    public OrderItem() {}

    // -------- business helpers --------

    /** Gọi sau khi set quantity/unitPrice/costPrice để tính lại subtotal và profit. */
    public void recalculate() {
        int qty = quantity == null || quantity <= 0 ? 0 : quantity;
        BigDecimal up = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        BigDecimal cp = costPrice == null ? BigDecimal.ZERO : costPrice;
        BigDecimal op = originalPrice == null ? up : originalPrice;
        BigDecimal da = op.subtract(up);
        discountAmount = da.signum() < 0 ? BigDecimal.ZERO : da;
        subtotal = up.multiply(BigDecimal.valueOf(qty));
        profit   = up.subtract(cp).multiply(BigDecimal.valueOf(qty));
    }

    @Transient
    public BigDecimal getDisplaySubtotal() {
        if (subtotal != null && subtotal.signum() > 0) return subtotal;
        BigDecimal price = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        int qty = quantity == null || quantity <= 0 ? 1 : quantity;
        return price.multiply(BigDecimal.valueOf(qty));
    }

    @Transient
    public BigDecimal getDisplayUnitPrice() {
        if (unitPrice != null && unitPrice.signum() > 0) return unitPrice;
        int qty = quantity == null || quantity <= 0 ? 1 : quantity;
        BigDecimal sub = getDisplaySubtotal();
        if (sub.signum() <= 0) return BigDecimal.ZERO;
        return sub.divide(BigDecimal.valueOf(qty), 0, RoundingMode.HALF_UP);
    }

    // -------- getters / setters --------
    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public void setOrder(Order o) { this.order = o; }
    public Product getProduct() { return product; }
    public void setProduct(Product p) { this.product = p; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer q) { this.quantity = q; }
    public BigDecimal getOriginalPrice() { return m(originalPrice); }
    public void setOriginalPrice(BigDecimal p) { this.originalPrice = m(p); }
    public BigDecimal getUnitPrice() { return m(unitPrice); }
    public void setUnitPrice(BigDecimal p) { this.unitPrice = m(p); }
    public BigDecimal getDiscountAmount() { return m(discountAmount); }
    public void setDiscountAmount(BigDecimal d) { this.discountAmount = m(d); }
    public BigDecimal getCostPrice() { return m(costPrice); }
    public void setCostPrice(BigDecimal p) { this.costPrice = m(p); }
    public BigDecimal getSubtotal() { return m(subtotal); }
    public void setSubtotal(BigDecimal s) { this.subtotal = m(s); }
    public BigDecimal getProfit() { return profit == null ? BigDecimal.ZERO : profit; }
    public void setProfit(BigDecimal p) { this.profit = p; }

    private BigDecimal m(BigDecimal v) { return (v == null || v.signum() < 0) ? BigDecimal.ZERO : v; }
}