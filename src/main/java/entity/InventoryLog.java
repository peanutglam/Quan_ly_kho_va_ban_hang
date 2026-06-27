package entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "inventory_logs")
public class InventoryLog {

    public static final String ACTION_PRODUCT_CREATE = "PRODUCT_CREATE";
    public static final String ACTION_PRODUCT_UPDATE = "PRODUCT_UPDATE";
    public static final String ACTION_PRODUCT_DELETE = "PRODUCT_DELETE";
    public static final String ACTION_IMPORT = "IMPORT";
    public static final String ACTION_ORDER_CREATE = "ORDER_CREATE";
    public static final String ACTION_ORDER_CANCEL = "ORDER_CANCEL";
    public static final String ACTION_ORDER_REOPEN = "ORDER_REOPEN";
    public static final String ACTION_ORDER_DELETE = "ORDER_DELETE";
    public static final String ACTION_INVENTORY_CHECK = "INVENTORY_CHECK";

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

    @Column(name = "action_type", nullable = false, length = 60)
    private String actionType;

    @Column(name = "reference_type", length = 60)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "before_quantity")
    private Integer beforeQuantity = 0;

    @Column(name = "after_quantity")
    private Integer afterQuantity = 0;

    @Column(name = "quantity_change")
    private Integer quantityChange = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        }
        if (beforeQuantity == null) beforeQuantity = 0;
        if (afterQuantity == null) afterQuantity = 0;
        if (quantityChange == null) quantityChange = afterQuantity - beforeQuantity;
    }

    public Long getId() { return id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public AppUser getActor() { return actor; }
    public void setActor(AppUser actor) { this.actor = actor; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = trim(actionType); }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = trim(referenceType); }

    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }

    public Integer getBeforeQuantity() { return beforeQuantity == null ? 0 : beforeQuantity; }
    public void setBeforeQuantity(Integer beforeQuantity) { this.beforeQuantity = beforeQuantity == null ? 0 : beforeQuantity; }

    public Integer getAfterQuantity() { return afterQuantity == null ? 0 : afterQuantity; }
    public void setAfterQuantity(Integer afterQuantity) { this.afterQuantity = afterQuantity == null ? 0 : afterQuantity; }

    public Integer getQuantityChange() { return quantityChange == null ? 0 : quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange == null ? 0 : quantityChange; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = trim(description); }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Transient
    public String getActionDisplayName() {
        return switch (actionType == null ? "" : actionType) {
            case ACTION_PRODUCT_CREATE -> "Thêm sản phẩm";
            case ACTION_PRODUCT_UPDATE -> "Cập nhật sản phẩm";
            case ACTION_PRODUCT_DELETE -> "Ẩn sản phẩm";
            case ACTION_IMPORT -> "Nhập hàng";
            case ACTION_ORDER_CREATE -> "Tạo đơn / bán hàng";
            case ACTION_ORDER_CANCEL -> "Hủy đơn / hoàn tồn";
            case ACTION_ORDER_REOPEN -> "Mở lại đơn / trừ tồn";
            case ACTION_ORDER_DELETE -> "Xóa đơn / hoàn tồn";
            case ACTION_INVENTORY_CHECK -> "Kiểm kê kho";
            default -> actionType;
        };
    }

    @Transient
    public String getProductName() {
        return product == null ? "-" : product.getName();
    }

    @Transient
    public String getActorName() {
        return actor == null ? "Hệ thống" : actor.getFullName();
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}