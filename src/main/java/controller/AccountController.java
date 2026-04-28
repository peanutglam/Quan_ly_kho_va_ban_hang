package controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import service.AuthService;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final AuthService authService;

    public AccountController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/change-password")
    public String changePasswordForm() {
        return "account/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Model model) {
        try {
            authService.changePassword(oldPassword, newPassword, confirmPassword);
            model.addAttribute("successMessage", "Đổi mật khẩu thành công");
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "account/change-password";
    }
}
