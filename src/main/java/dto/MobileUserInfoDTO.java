package dto;

import entity.AppUser;

public class MobileUserInfoDTO {

    private Long id;
    private String fullName;
    private String username;
    private String role;
    private String roleDisplayName;

    private boolean canViewProducts;
    private boolean canManageProducts;
    private boolean canManageImports;
    private boolean canManageOrders;
    private boolean canViewReports;
    private boolean canManageEmployees;
    private boolean canManageCustomers;
    private boolean canInventoryCheck;

    public MobileUserInfoDTO() {
    }

    public static MobileUserInfoDTO fromUser(AppUser user) {
        MobileUserInfoDTO dto = new MobileUserInfoDTO();

        String role = normalizeRole(user.getRole());

        dto.id = user.getId();
        dto.fullName = user.getFullName();
        dto.username = user.getUsername();
        dto.role = role;
        dto.roleDisplayName = user.getRoleDisplayName();

        boolean owner = AppUser.ROLE_OWNER.equals(role);
        boolean staff = AppUser.ROLE_STAFF.equals(role);
        boolean sale = AppUser.ROLE_SALE.equals(role);

        dto.canViewProducts = owner || staff || sale;
        dto.canManageProducts = owner;
        dto.canManageImports = owner || staff;
        dto.canManageOrders = owner || sale;
        dto.canViewReports = owner;
        dto.canManageEmployees = owner;
        dto.canManageCustomers = owner;
        dto.canInventoryCheck = owner || staff;

        return dto;
    }

    private static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }

        String value = role.trim().toUpperCase();

        if (value.startsWith("ROLE_")) {
            value = value.substring(5);
        }

        return value;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName == null ? "" : fullName;
    }

    public String getUsername() {
        return username == null ? "" : username;
    }

    public String getRole() {
        return role == null ? "" : role;
    }

    public String getRoleDisplayName() {
        return roleDisplayName == null ? "" : roleDisplayName;
    }

    public boolean isCanViewProducts() {
        return canViewProducts;
    }

    public boolean isCanManageProducts() {
        return canManageProducts;
    }

    public boolean isCanManageImports() {
        return canManageImports;
    }

    public boolean isCanManageOrders() {
        return canManageOrders;
    }

    public boolean isCanViewReports() {
        return canViewReports;
    }

    public boolean isCanManageEmployees() {
        return canManageEmployees;
    }

    public boolean isCanManageCustomers() {
        return canManageCustomers;
    }

    public boolean isCanInventoryCheck() {
        return canInventoryCheck;
    }
}