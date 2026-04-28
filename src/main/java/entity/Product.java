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

    @Min(value = 0, message = "Số lượng không được âm")
    @Column(nullable = false)
    private Integer quantity = 0;

    @Min(value = 0, message = "Tổng số lượng không được âm")
    @Column(name = "total_quantity", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer totalQuantity = 0;

    @Min(value = 0, message = "Số lượng xuất không được âm")
    @Column(name = "sold_quantity", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer soldQuantity = 0;

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

    // Soft delete: false = đã xóa (bị ẩn), true = đang hoạt động
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean active = true;

    @PostLoad
    @PrePersist
    @PreUpdate
    public void recalculateInventoryFields() {
        int stock = nonNegative(quantity);
        int sold = nonNegative(soldQuantity);
        int total = nonNegative(totalQuantity);

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

        BigDecimal importValue = money(importPrice).multiply(BigDecimal.valueOf(totalQuantity));
        BigDecimal saleValue = money(salePrice).multiply(BigDecimal.valueOf(totalQuantity));
        totalImportAmount = importValue;
        totalSaleAmount = saleValue;
        capital = importValue;
        profit = saleValue.subtract(importValue);
        profitStatus = profit.signum() >= 0 ? "Lãi" : "Lỗ";
    }

    public void increaseStock(int amount) {
        if (amount <= 0) return;
        totalQuantity = nonNegative(totalQuantity) + amount;
        recalculateInventoryFields();
    }

    public void registerSale(int amount) {
        if (amount <= 0) return;
        soldQuantity = nonNegative(soldQuantity) + amount;
        recalculateInventoryFields();
    }

    public void restoreSale(int amount) {
        if (amount <= 0) return;
        soldQuantity = Math.max(nonNegative(soldQuantity) - amount, 0);
        recalculateInventoryFields();
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code != null ? code.trim() : null; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name != null ? name.trim() : null; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category != null ? category.trim() : null; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand != null ? brand.trim() : null; }
    public Integer getQuantity() {
        recalculateInventoryFields();
        return quantity;
    }
    public void setQuantity(Integer quantity) {
        this.quantity = nonNegative(quantity);
        if (totalQuantity == null || totalQuantity == 0) {
            this.totalQuantity = this.quantity + nonNegative(soldQuantity);
        }
        recalculateInventoryFields();
    }
    public Integer getTotalQuantity() {
        recalculateInventoryFields();
        return totalQuantity;
    }
    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = nonNegative(totalQuantity);
        recalculateInventoryFields();
    }
    public Integer getSoldQuantity() {
        recalculateInventoryFields();
        return soldQuantity;
    }
    public void setSoldQuantity(Integer soldQuantity) {
        this.soldQuantity = nonNegative(soldQuantity);
        recalculateInventoryFields();
    }
    public Integer getInventoryQuantity() {
        recalculateInventoryFields();
        return inventoryQuantity;
    }
    public BigDecimal getImportPrice() { return importPrice; }
    public void setImportPrice(BigDecimal importPrice) {
        this.importPrice = money(importPrice);
        recalculateInventoryFields();
    }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = money(salePrice);
        recalculateInventoryFields();
    }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public BigDecimal getTotalImportAmount() {
        recalculateInventoryFields();
        return totalImportAmount;
    }
    public BigDecimal getTotalSaleAmount() {
        recalculateInventoryFields();
        return totalSaleAmount;
    }
    public BigDecimal getCapital() {
        recalculateInventoryFields();
        return capital;
    }
    public BigDecimal getProfit() {
        recalculateInventoryFields();
        return profit;
    }
    public String getProfitStatus() {
        recalculateInventoryFields();
        return profitStatus;
    }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description != null ? description.trim() : null; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active == null ? true : active; }

    private int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }
}
