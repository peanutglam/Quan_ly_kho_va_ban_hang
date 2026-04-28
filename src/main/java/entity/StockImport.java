package entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_imports")
public class StockImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_code", nullable = false, unique = true)
    private String importCode;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "import_price", precision = 15, scale = 2)
    private BigDecimal importPrice;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (importCode == null || importCode.isBlank()) {
            importCode = "IMP-" + System.currentTimeMillis();
        }
    }

    public Long getId() { return id; }

    public String getImportCode() { return importCode; }
    public void setImportCode(String importCode) { this.importCode = importCode; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getImportPrice() { return importPrice; }
    public void setImportPrice(BigDecimal importPrice) { this.importPrice = importPrice; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public BigDecimal getTotalAmount() {
        BigDecimal price = importPrice == null ? BigDecimal.ZERO : importPrice;
        int qty = quantity == null ? 0 : quantity;
        return price.multiply(BigDecimal.valueOf(qty));
    }
}
