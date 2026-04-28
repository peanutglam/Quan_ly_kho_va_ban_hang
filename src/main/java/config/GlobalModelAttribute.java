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
            return currentUser != null
                    && currentUser.getRole() != null
                    && "OWNER".equalsIgnoreCase(currentUser.getRole());
        } catch (Exception e) {
            return false;
        }
    }

    @ModelAttribute("isEmployee")
    public boolean isEmployee() {
        try {
            AppUser currentUser = authService.getCurrentUser();
            return currentUser != null
                    && currentUser.getRole() != null
                    && !"OWNER".equalsIgnoreCase(currentUser.getRole());
        } catch (Exception e) {
            return false;
        }
    }
}