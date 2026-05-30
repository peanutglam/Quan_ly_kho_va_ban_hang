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
    private String imageUrl;
    private String description;
    private Integer quantity;
    private Integer totalQuantity;
    private Integer soldQuantity;
    private BigDecimal importPrice;
    private BigDecimal salePrice;
    private BigDecimal effectiveSalePrice;
    private boolean promotionActive;
    private BigDecimal discountPercent;
    private BigDecimal discountAmount;
    private LocalDate expiryDate;

    public static ProductApiResponse from(Product product) {
        ProductApiResponse response = new ProductApiResponse();
        response.setId(product.getId());
        response.setCode(product.getCode());
        response.setName(product.getName());
        response.setCategory(product.getCategory());
        response.setBrand(product.getBrand());
        response.setImageUrl(product.getImageUrl());
        response.setDescription(product.getPublicDescription());
        response.setQuantity(product.getQuantity());
        response.setTotalQuantity(product.getTotalQuantity());
        response.setSoldQuantity(product.getSoldQuantity());
        response.setImportPrice(product.getImportPrice());
        response.setSalePrice(product.getSalePrice());
        response.setEffectiveSalePrice(product.getEffectiveSalePrice());
        response.setPromotionActive(product.isPromotionCurrentlyActive());
        response.setDiscountPercent(product.getDiscountPercentDisplay());
        response.setDiscountAmount(product.getDiscountAmount());
        response.setExpiryDate(product.getExpiryDate());
        return response;
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    public Integer getSoldQuantity() { return soldQuantity; }
    public void setSoldQuantity(Integer soldQuantity) { this.soldQuantity = soldQuantity; }

    public BigDecimal getImportPrice() { return importPrice; }
    public void setImportPrice(BigDecimal importPrice) { this.importPrice = importPrice; }

    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }

    public BigDecimal getEffectiveSalePrice() { return effectiveSalePrice; }
    public void setEffectiveSalePrice(BigDecimal effectiveSalePrice) { this.effectiveSalePrice = effectiveSalePrice; }

    public boolean isPromotionActive() { return promotionActive; }
    public void setPromotionActive(boolean promotionActive) { this.promotionActive = promotionActive; }

    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
}
