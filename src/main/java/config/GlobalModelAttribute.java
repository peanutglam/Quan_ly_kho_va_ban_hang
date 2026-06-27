package config;

import entity.AppUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import service.AuthService;

@ControllerAdvice
public class GlobalModelAttribute {

    private final AuthService authService;

    public GlobalModelAttribute(AuthService authService) {
        this.authService = authService;
    }

    @ModelAttribute("currentUser")
    public AppUser currentUser() {
        try {
            return authService.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }

    @ModelAttribute("workspaceOwner")
    public AppUser workspaceOwner() {
        try {
            return authService.getWorkspaceOwner();
        } catch (Exception e) {
            return null;
        }
    }

    @ModelAttribute("isOwner")
    public boolean isOwner() {
        try {
            AppUser currentUser = authService.getCurrentUser();
            return currentUser != null && isOwnerRole(currentUser.getRole());
        } catch (Exception e) {
            return false;
        }
    }

    @ModelAttribute("isEmployee")
    public boolean isEmployee() {
        try {
            AppUser currentUser = authService.getCurrentUser();
            return currentUser != null && !isOwnerRole(currentUser.getRole()) && !isCustomerRole(currentUser.getRole());
        } catch (Exception e) {
            return false;
        }
    }

    @ModelAttribute("isCustomer")
    public boolean isCustomer() {
        try {
            AppUser currentUser = authService.getCurrentUser();
            return currentUser != null && isCustomerRole(currentUser.getRole());
        } catch (Exception e) {
            return false;
        }
    }


    @ModelAttribute("isStockStaff")
    public boolean isStockStaff() {
        return safePermission(() -> authService.hasRole(AppUser.ROLE_STAFF));
    }

    @ModelAttribute("isSaleStaff")
    public boolean isSaleStaff() {
        return safePermission(() -> authService.hasRole(AppUser.ROLE_SALE));
    }

    @ModelAttribute("canViewProducts")
    public boolean canViewProducts() {
        return safePermission(authService::canViewProducts);
    }

    @ModelAttribute("canManageProducts")
    public boolean canManageProducts() {
        return safePermission(authService::canManageProducts);
    }

    @ModelAttribute("canManageOrders")
    public boolean canManageOrders() {
        return safePermission(authService::canManageOrders);
    }

    @ModelAttribute("canManageSuppliers")
    public boolean canManageSuppliers() {
        return safePermission(authService::canManageSuppliers);
    }

    @ModelAttribute("canManageImports")
    public boolean canManageImports() {
        return safePermission(authService::canManageImports);
    }

    @ModelAttribute("canInventoryCheck")
    public boolean canInventoryCheck() {
        return safePermission(authService::canInventoryCheck);
    }

    @ModelAttribute("canViewInventoryLogs")
    public boolean canViewInventoryLogs() {
        return safePermission(authService::canViewInventoryLogs);
    }

    @ModelAttribute("canViewReports")
    public boolean canViewReports() {
        return safePermission(authService::canViewReports);
    }

    @ModelAttribute("canManageShopConfig")
    public boolean canManageShopConfig() {
        return safePermission(authService::canManageShopConfig);
    }

    @ModelAttribute("canManageAccounts")
    public boolean canManageAccounts() {
        return safePermission(authService::canManageAccounts);
    }

    private boolean safePermission(BooleanSupplier supplier) {
        try {
            return supplier.getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }

    private boolean isOwnerRole(String role) {
        String value = normalize(role);
        return "OWNER".equals(value) || "ADMIN".equals(value);
    }

    private boolean isCustomerRole(String role) {
        return "CUSTOMER".equals(normalize(role));
    }

    private String normalize(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }

        String value = role.trim().toUpperCase();
        return value.startsWith("ROLE_") ? value.substring(5) : value;
    }
}