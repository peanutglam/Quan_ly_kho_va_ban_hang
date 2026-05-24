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