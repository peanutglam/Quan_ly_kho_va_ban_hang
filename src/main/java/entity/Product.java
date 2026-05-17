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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    /** Tồn kho hiện tại = totalQuantity - soldQuantity */
    @Min(value = 0, message = "Số lượng tồn không được âm")
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer quantity = 0;

    /** Tổng số lượng đã nhập (tăng qua phiếu nhập) */
    @Min(value = 0, message = "Tổng số lượng không được âm")
    @Column(name = "total_quantity", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer totalQuantity = 0;

    /** Tổng số lượng đã bán (tăng khi tạo đơn hàng) */
    @Min(value = 0, message = "Số lượng xuất không được âm")
    @Column(name = "sold_quantity", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer soldQuantity = 0;

    /** = totalQuantity - soldQuantity (cập nhật khi save) */
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

    @ManyToOne @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne @JoinColumn(name = "user_id")
    private AppUser user;

    /** Soft delete */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean active = true;

    // ======================================================
    // Lifecycle - chỉ @PrePersist + @PreUpdate, KHÔNG @PostLoad
    // để tránh Hibernate dirty-tracking không cần thiết
    // ======================================================
    @PrePersist
    @PreUpdate
    public void recalculateInventoryFields() {
        int sold  = nn(soldQuantity);
        int total = nn(totalQuantity);
        int stock = nn(quantity);

        if (total == 0 && stock > 0) total = stock + sold;
        if (sold > total) total = sold;

        totalQuantity     = total;
        soldQuantity      = sold;
        inventoryQuantity = Math.max(total - sold, 0);
        quantity          = inventoryQuantity;

        BigDecimal ip  = m(importPrice);
        BigDecimal sp  = m(salePrice);
        BigDecimal qiv = BigDecimal.valueOf(inventoryQuantity);
        BigDecimal qtd = BigDecimal.valueOf(total);
        BigDecimal qsd = BigDecimal.valueOf(sold);

        totalImportAmount = ip.multiply(qtd);
        totalSaleAmount   = sp.multiply(qtd);
        capital           = ip.multiply(qiv);
        profit            = sp.subtract(ip).multiply(qsd);
        profitStatus      = profit.signum() >= 0 ? "Lãi" : "Lỗ";
    }

    // ======================================================
    // Business operations
    // ======================================================
    public void increaseStock(int amount) {
        if (amount <= 0) return;
        totalQuantity = nn(totalQuantity) + amount;
        recalculateInventoryFields();
    }

    public void registerSale(int amount) {
        if (amount <= 0) return;
        soldQuantity = nn(soldQuantity) + amount;
        recalculateInventoryFields();
    }

    public void restoreSale(int amount) {
        if (amount <= 0) return;
        soldQuantity = Math.max(nn(soldQuantity) - amount, 0);
        recalculateInventoryFields();
    }

    // ======================================================
    // Getters & Setters - KHÔNG có side effect trong getter
    // ======================================================
    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String c) { this.code = c != null ? c.trim() : null; }

    public String getName() { return name; }
    public void setName(String n) { this.name = n != null ? n.trim() : null; }

    public String getCategory() { return category; }
    public void setCategory(String c) { this.category = c != null ? c.trim() : null; }

    public String getBrand() { return brand; }
    public void setBrand(String b) { this.brand = b != null ? b.trim() : null; }

    public Integer getQuantity() { return nn(quantity); }
    public void setQuantity(Integer q) { this.quantity = nn(q); }

    public Integer getTotalQuantity() { return nn(totalQuantity); }
    public void setTotalQuantity(Integer t) { this.totalQuantity = nn(t); }

    public Integer getSoldQuantity() { return nn(soldQuantity); }
    public void setSoldQuantity(Integer s) { this.soldQuantity = nn(s); }

    public Integer getInventoryQuantity() { return nn(inventoryQuantity); }

    public BigDecimal getImportPrice() { return m(importPrice); }
    public void setImportPrice(BigDecimal p) { this.importPrice = m(p); }

    public BigDecimal getSalePrice() { return m(salePrice); }
    public void setSalePrice(BigDecimal p) { this.salePrice = m(p); }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate d) { this.expiryDate = d; }

    public BigDecimal getTotalImportAmount() { return m(totalImportAmount); }
    public BigDecimal getTotalSaleAmount()   { return m(totalSaleAmount); }
    public BigDecimal getCapital()           { return m(capital); }
    public BigDecimal getProfit()            { return profit == null ? BigDecimal.ZERO : profit; }
    public String getProfitStatus()          { return profitStatus != null ? profitStatus : "Lãi"; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier s) { this.supplier = s; }

    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d != null ? d.trim() : null; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser u) { this.user = u; }

    public Boolean getActive() { return active == null ? true : active; }
    public void setActive(Boolean a) { this.active = a == null ? true : a; }

    private int nn(Integer v) { return (v == null || v < 0) ? 0 : v; }
    private BigDecimal m(BigDecimal v) { return (v == null || v.signum() < 0) ? BigDecimal.ZERO : v; }
}