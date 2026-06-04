package dto;

import entity.Product;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProductApiResponse {

    private Long id;
    private String code;
    private String name;
    private String category;
    private String brand;
    private String description;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal salePrice;
    private BigDecimal effectiveSalePrice;
    private Boolean promotionActive;
    private BigDecimal discountPercentDisplay;
    private LocalDate expiryDate;

    public ProductApiResponse() {
    }

    public ProductApiResponse(Product product) {
        if (product == null) {
            return;
        }

        this.id = product.getId();
        this.code = product.getCode();
        this.name = product.getName();
        this.category = product.getCategory();
        this.brand = product.getBrand();
        this.description = cleanDescription(product.getDescription());
        this.imageUrl = product.getImageUrl();
        this.quantity = product.getQuantity();
        this.salePrice = product.getSalePrice();
        this.effectiveSalePrice = product.getEffectiveSalePrice();
        this.promotionActive = product.isPromotionCurrentlyActive();
        this.discountPercentDisplay = product.getDiscountPercentDisplay();
        this.expiryDate = product.getExpiryDate();
    }

    private String cleanDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "Sản phẩm hiện chưa có mô tả chi tiết.";
        }

        String value = description.trim();

        if (value.toLowerCase().contains("google sheet")
                || value.toLowerCase().contains("import linh hoạt")) {
            return "Sản phẩm hiện chưa có mô tả chi tiết.";
        }

        return value;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public BigDecimal getEffectiveSalePrice() {
        return effectiveSalePrice;
    }

    public Boolean getPromotionActive() {
        return promotionActive;
    }

    public BigDecimal getDiscountPercentDisplay() {
        return discountPercentDisplay;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }
}