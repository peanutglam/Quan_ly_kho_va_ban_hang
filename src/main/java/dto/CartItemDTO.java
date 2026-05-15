package dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class CartItemDTO implements Serializable {
    private Long productId;
    private String productCode;
    private String productName;
    private BigDecimal unitPrice;
    private int quantity;
    private int maxStock;  // tồn kho để validate

    public CartItemDTO() {}

    public CartItemDTO(Long productId, String productCode, String productName,
                       BigDecimal unitPrice, int quantity, int maxStock) {
        this.productId = productId;
        this.productCode = productCode;
        this.productName = productName;
        this.unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
        this.quantity = quantity;
        this.maxStock = maxStock;
    }

    public BigDecimal getSubtotal() {
        return unitPrice == null ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long v) { this.productId = v; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String v) { this.productCode = v; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal v) { this.unitPrice = v; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int v) { this.quantity = v; }
    public int getMaxStock() { return maxStock; }
    public void setMaxStock(int v) { this.maxStock = v; }
}
