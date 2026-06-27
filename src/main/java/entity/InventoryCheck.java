package entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "inventory_checks")
public class InventoryCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private AppUser actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "system_quantity", nullable = false)
    private Integer systemQuantity = 0;

    @Column(name = "actual_quantity", nullable = false)
    private Integer actualQuantity = 0;

    @Column(name = "difference_quantity", nullable = false)
    private Integer differenceQuantity = 0;

    @Column(length = 255)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @PrePersist
    public void prePersist() {
        if (checkedAt == null) {
            checkedAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        }
        if (systemQuantity == null) systemQuantity = 0;
        if (actualQuantity == null) actualQuantity = 0;
        differenceQuantity = actualQuantity - systemQuantity;
    }

    public Long getId() { return id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public AppUser getActor() { return actor; }
    public void setActor(AppUser actor) { this.actor = actor; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getSystemQuantity() { return systemQuantity == null ? 0 : systemQuantity; }
    public void setSystemQuantity(Integer systemQuantity) { this.systemQuantity = systemQuantity == null ? 0 : systemQuantity; }

    public Integer getActualQuantity() { return actualQuantity == null ? 0 : actualQuantity; }
    public void setActualQuantity(Integer actualQuantity) { this.actualQuantity = actualQuantity == null ? 0 : actualQuantity; }

    public Integer getDifferenceQuantity() { return differenceQuantity == null ? 0 : differenceQuantity; }
    public void setDifferenceQuantity(Integer differenceQuantity) { this.differenceQuantity = differenceQuantity == null ? 0 : differenceQuantity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = trim(reason); }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = trim(note); }

    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }

    @Transient
    public String getProductName() { return product == null ? "-" : product.getName(); }

    @Transient
    public String getActorName() { return actor == null ? "Hệ thống" : actor.getFullName(); }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}