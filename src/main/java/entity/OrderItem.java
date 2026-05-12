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

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal subtotal;

    public OrderItem() {
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
    @Transient
    public BigDecimal getDisplaySubtotal() {
        if (subtotal != null && subtotal.signum() > 0) {
            return subtotal;
        }

        if (order != null && order.getTotalAmount() != null && order.getTotalAmount().signum() > 0) {
            return order.getTotalAmount();
        }

        BigDecimal price = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        int qty = quantity == null || quantity <= 0 ? 1 : quantity;

        return price.multiply(BigDecimal.valueOf(qty));
    }

    @Transient
    public BigDecimal getDisplayUnitPrice() {
        if (unitPrice != null && unitPrice.signum() > 0) {
            return unitPrice;
        }

        int qty = quantity == null || quantity <= 0 ? 1 : quantity;
        BigDecimal displaySubtotal = getDisplaySubtotal();

        if (displaySubtotal.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        return displaySubtotal.divide(BigDecimal.valueOf(qty), 0, RoundingMode.HALF_UP);
    }
}