package entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Mã sản phẩm không được để trống")
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String code;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    private String category;

    private String brand;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    /**
     * Tồn kho hiện tại = totalQuantity - soldQuantity
     */
    @Min(value = 0, message = "Số lượng tồn không được âm")
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer quantity = 0;

    /**
     * Tổng số lượng đã nhập
     */
    @Min(value = 0, message = "Tổng số lượng không được âm")
    @Column(name = "total_quantity", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer totalQuantity = 0;

    /**
     * Tổng số lượng đã bán
     */
    @Min(value = 0, message = "Số lượng xuất không được âm")
    @Column(name = "sold_quantity", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer soldQuantity = 0;

    /**
     * Tồn kho tính toán
     */
    @Min(value = 0, message = "Tồn kho không được âm")
    @Column(name = "inventory_quantity", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer inventoryQuantity = 0;

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá nhập không hợp lệ")
    @Column(name = "import_price", precision = 15, scale = 2)
    private BigDecimal importPrice = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá bán không hợp lệ")
    @Column(name = "sale_price", precision = 15, scale = 2)
    private BigDecimal salePrice = BigDecimal.ZERO;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "total_import_amount", precision = 15, scale = 2)
    private BigDecimal totalImportAmount = BigDecimal.ZERO;

    @Column(name = "total_sale_amount", precision = 15, scale = 2)
    private BigDecimal totalSaleAmount = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal capital = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal profit = BigDecimal.ZERO;

    @Column(name = "profit_status", length = 20)
    private String profitStatus = "Lãi";

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    /**
     * Soft delete:
     * true = đang hoạt động
     * false = đã ẩn
     */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean active = true;

    @PrePersist
    @PreUpdate
    public void recalculateInventoryFields() {
        int sold = nn(soldQuantity);
        int total = nn(totalQuantity);
        int stock = nn(quantity);

        if (total == 0 && stock > 0) {
            total = stock + sold;
        }

        if (sold > total) {
            total = sold;
        }

        totalQuantity = total;
        soldQuantity = sold;
        inventoryQuantity = Math.max(total - sold, 0);
        quantity = inventoryQuantity;

        BigDecimal ip = money(importPrice);
        BigDecimal sp = money(salePrice);

        BigDecimal totalQty = BigDecimal.valueOf(totalQuantity);
        BigDecimal stockQty = BigDecimal.valueOf(inventoryQuantity);
        BigDecimal soldQty = BigDecimal.valueOf(soldQuantity);

        totalImportAmount = ip.multiply(totalQty);
        totalSaleAmount = sp.multiply(totalQty);
        capital = ip.multiply(stockQty);
        profit = sp.subtract(ip).multiply(soldQty);
        profitStatus = profit.signum() >= 0 ? "Lãi" : "Lỗ";
    }

    public void increaseStock(int amount) {
        if (amount <= 0) {
            return;
        }

        totalQuantity = nn(totalQuantity) + amount;
    }

    public void registerSale(int amount) {
        if (amount <= 0) {
            return;
        }

        soldQuantity = nn(soldQuantity) + amount;
    }

    public void restoreSale(int amount) {
        if (amount <= 0) {
            return;
        }

        soldQuantity = Math.max(nn(soldQuantity) - amount, 0);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = trim(code);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = trim(name);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = trim(category);
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = trim(brand);
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = trim(imageUrl);
    }

    public Integer getQuantity() {
        return nn(quantity);
    }

    public void setQuantity(Integer quantity) {
        this.quantity = nn(quantity);
    }

    public Integer getTotalQuantity() {
        return nn(totalQuantity);
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = nn(totalQuantity);
    }

    public Integer getSoldQuantity() {
        return nn(soldQuantity);
    }

    public void setSoldQuantity(Integer soldQuantity) {
        this.soldQuantity = nn(soldQuantity);
    }

    public Integer getInventoryQuantity() {
        return nn(inventoryQuantity);
    }

    public void setInventoryQuantity(Integer inventoryQuantity) {
        this.inventoryQuantity = nn(inventoryQuantity);
    }

    public BigDecimal getImportPrice() {
        return money(importPrice);
    }

    public void setImportPrice(BigDecimal importPrice) {
        this.importPrice = money(importPrice);
    }

    public BigDecimal getSalePrice() {
        return money(salePrice);
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = money(salePrice);
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public BigDecimal getTotalImportAmount() {
        return money(totalImportAmount);
    }

    public void setTotalImportAmount(BigDecimal totalImportAmount) {
        this.totalImportAmount = money(totalImportAmount);
    }

    public BigDecimal getTotalSaleAmount() {
        return money(totalSaleAmount);
    }

    public void setTotalSaleAmount(BigDecimal totalSaleAmount) {
        this.totalSaleAmount = money(totalSaleAmount);
    }

    public BigDecimal getCapital() {
        return money(capital);
    }

    public void setCapital(BigDecimal capital) {
        this.capital = money(capital);
    }

    public BigDecimal getProfit() {
        return profit == null ? BigDecimal.ZERO : profit;
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit == null ? BigDecimal.ZERO : profit;
    }

    public String getProfitStatus() {
        return profitStatus;
    }

    public void setProfitStatus(String profitStatus) {
        this.profitStatus = trim(profitStatus);
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = trim(description);
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public Boolean getActive() {
        return active == null || active;
    }

    public void setActive(Boolean active) {
        this.active = active == null || active;
    }

    @Transient
    public boolean isInStock() {
        return getQuantity() > 0;
    }

    @Transient
    public boolean hasImage() {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }

    private int nn(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}