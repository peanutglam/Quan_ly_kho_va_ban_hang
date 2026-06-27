package entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "app_users")
public class AppUser {

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_STAFF = "STAFF";
    public static final String ROLE_SALE = "SALE";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 120)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(nullable = false, length = 30)
    private String role = ROLE_OWNER;

    /*
     * OWNER: owner = null
     * EMPLOYEE: owner trỏ tới tài khoản OWNER đã tạo nhân viên đó
     * CUSTOMER: owner trỏ tới OWNER của cửa hàng mà khách đăng ký
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    /*
     * Xóa mềm để tránh lỗi khóa ngoại với sản phẩm, đơn hàng, phiếu nhập cũ
     */
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        }

        if (active == null) {
            active = true;
        }

        role = normalizeRole(role);
    }

    @PreUpdate
    public void preUpdate() {
        role = normalizeRole(role);

        if (active == null) {
            active = true;
        }
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName != null ? fullName.trim() : null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username != null ? username.trim().toLowerCase() : null;
    }

    public String getPassword() {
        return password;
    }

    /*
     * Không hiển thị mật khẩu thật cho Owner.
     * Field password chỉ lưu chuỗi đã mã hóa.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null && !email.isBlank() ? email.trim().toLowerCase() : null;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone != null && !phone.isBlank() ? phone.trim() : null;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address != null && !address.isBlank() ? address.trim() : null;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = normalizeRole(role);
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }

    public Boolean getActive() {
        return active == null ? true : active;
    }

    public void setActive(Boolean active) {
        this.active = active == null ? true : active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isOwnerAccount() {
        return ROLE_OWNER.equals(normalizeRole(role));
    }

    public boolean isCustomerAccount() {
        return ROLE_CUSTOMER.equals(normalizeRole(role));
    }

    public boolean isEmployeeAccount() {
        String normalizedRole = normalizeRole(role);
        return ROLE_STAFF.equals(normalizedRole) || ROLE_SALE.equals(normalizedRole);
    }

    public AppUser getWorkspaceOwner() {
        if (isOwnerAccount()) {
            return this;
        }

        return owner;
    }

    public String getRoleDisplayName() {
        return switch (normalizeRole(role)) {
            case ROLE_OWNER -> "Owner / Chủ shop";
            case ROLE_STAFF -> "Nhân viên kho";
            case ROLE_SALE -> "Nhân viên bán hàng";
            case ROLE_CUSTOMER -> "Khách hàng";
            default -> role;
        };
    }

    public String getPasswordDisplayText() {
        return password == null || password.isBlank() ? "Chưa có mật khẩu" : "Đã mã hóa";
    }

    private String normalizeRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return ROLE_OWNER;
        }

        String value = rawRole.trim().toUpperCase();

        if (value.startsWith("ROLE_")) {
            value = value.substring(5);
        }

        if ("ADMIN".equals(value)) {
            return ROLE_OWNER;
        }

        return value;
    }
}