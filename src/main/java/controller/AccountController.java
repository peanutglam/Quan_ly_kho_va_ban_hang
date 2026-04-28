package controller;

import entity.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.AuthService;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final AuthService authService;

    public AccountController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public String listAccounts(Model model) {
        authService.requireRole(AppUser.ROLE_OWNER);

        model.addAttribute("currentUser", authService.getCurrentUser());
        model.addAttribute("workspaceOwner", authService.getWorkspaceOwner());
        model.addAttribute("users", authService.getUsersInCurrentWorkspace());

        return "account/list";
    }

    @GetMapping("/create")
    public String createEmployeeForm(Model model) {
        authService.requireRole(AppUser.ROLE_OWNER);

        if (!model.containsAttribute("user")) {
            AppUser user = new AppUser();
            user.setRole(AppUser.ROLE_STAFF);
            model.addAttribute("user", user);
        }

        model.addAttribute("workspaceOwner", authService.getWorkspaceOwner());
        return "account/form";
    }

    @PostMapping("/create")
    public String createEmployee(@ModelAttribute("user") AppUser user,
                                 @RequestParam String confirmPassword,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            authService.createEmployeeAccount(user, confirmPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo tài khoản Employee thành công");
            return "redirect:/account";
        } catch (IllegalArgumentException | SecurityException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("workspaceOwner", authService.getWorkspaceOwner());
            return "account/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            authService.deleteEmployee(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã khóa/xóa tài khoản Employee khỏi web của bạn");
        } catch (IllegalArgumentException | SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/account";
    }

    @GetMapping("/change-password")
    public String changePasswordForm(Model model) {
        model.addAttribute("currentUser", authService.getCurrentUser());
        model.addAttribute("workspaceOwner", authService.getWorkspaceOwner());
        return "account/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Model model) {
        model.addAttribute("currentUser", authService.getCurrentUser());
        model.addAttribute("workspaceOwner", authService.getWorkspaceOwner());

        try {
            authService.changePassword(oldPassword, newPassword, confirmPassword);
            model.addAttribute("successMessage", "Đổi mật khẩu thành công");
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }

        return "account/change-password";
    }

    @GetMapping("/delete-my-account")
    public String deleteMyAccountForm(Model model) {
        model.addAttribute("currentUser", authService.getCurrentUser());
        model.addAttribute("workspaceOwner", authService.getWorkspaceOwner());
        return "account/delete-my-account";
    }

    @PostMapping("/delete-my-account")
    public String deleteMyAccount(@RequestParam String password,
                                  HttpServletRequest request,
                                  HttpServletResponse response,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            authService.deleteCurrentAccount(password, request, response);
            redirectAttributes.addFlashAttribute("successMessage", "Tài khoản của bạn đã được xóa/khóa");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("currentUser", authService.getCurrentUser());
            model.addAttribute("workspaceOwner", authService.getWorkspaceOwner());
            model.addAttribute("errorMessage", e.getMessage());
            return "account/delete-my-account";
        }
    }
}