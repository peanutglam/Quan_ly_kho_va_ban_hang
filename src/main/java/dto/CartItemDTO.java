package dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class CartItemDTO implements Serializable {
    private Long productId;
    private String productCode;
    private String productName;
    private BigDecimal originalPrice;
    private BigDecimal unitPrice;
    private boolean promotionActive;
    private BigDecimal discountPercent;
    private int quantity;
    private int maxStock;

    public CartItemDTO() {}

    public CartItemDTO(Long productId, String productCode, String productName,
                       BigDecimal unitPrice, int quantity, int maxStock) {
        this(productId, productCode, productName, unitPrice, unitPrice, false, BigDecimal.ZERO, quantity, maxStock);
    }

    public CartItemDTO(Long productId,
                       String productCode,
                       String productName,
                       BigDecimal originalPrice,
                       BigDecimal unitPrice,
                       boolean promotionActive,
                       BigDecimal discountPercent,
                       int quantity,
                       int maxStock) {
        this.productId = productId;
        this.productCode = productCode;
        this.productName = productName;
        this.originalPrice = money(originalPrice);
        this.unitPrice = money(unitPrice);
        this.promotionActive = promotionActive;
        this.discountPercent = money(discountPercent);
        this.quantity = Math.max(quantity, 0);
        this.maxStock = Math.max(maxStock, 0);
    }

    public BigDecimal getSubtotal() {
        return money(unitPrice).multiply(BigDecimal.valueOf(quantity));
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long v) { this.productId = v; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String v) { this.productCode = v; }

    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }

    public BigDecimal getOriginalPrice() { return money(originalPrice); }
    public void setOriginalPrice(BigDecimal v) { this.originalPrice = money(v); }

    public BigDecimal getUnitPrice() { return money(unitPrice); }
    public void setUnitPrice(BigDecimal v) { this.unitPrice = money(v); }

    public boolean isPromotionActive() { return promotionActive; }
    public void setPromotionActive(boolean promotionActive) { this.promotionActive = promotionActive; }

    public BigDecimal getDiscountPercent() { return money(discountPercent); }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = money(discountPercent); }

    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { this.quantity = Math.max(v, 0); }

    public int getMaxStock() { return maxStock; }
    public void setMaxStock(int v) { this.maxStock = Math.max(v, 0); }

    private BigDecimal money(BigDecimal v) {
        return v == null || v.signum() < 0 ? BigDecimal.ZERO : v;
    }
}