package controller;

import entity.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.AuthService;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginForm(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute(AuthService.SESSION_USER_ID) != null) {
            return "redirect:/";
        }

        model.addAttribute("accountType", AuthService.ACCOUNT_TYPE_OWNER);

        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam(defaultValue = AuthService.ACCOUNT_TYPE_OWNER) String accountType,
                        @RequestParam(required = false) String ownerUsername,
                        @RequestParam(required = false) String ownerPassword,
                        @RequestParam(required = false) String employeeUsername,
                        @RequestParam(required = false) String employeePassword,
                        Model model) {
        try {
            String username;
            String password;

            if (AuthService.ACCOUNT_TYPE_EMPLOYEE.equalsIgnoreCase(accountType)) {
                username = employeeUsername;
                password = employeePassword;
            } else {
                username = ownerUsername;
                password = ownerPassword;
                accountType = AuthService.ACCOUNT_TYPE_OWNER;
            }

            authService.login(username, password, accountType);

            return "redirect:/";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("accountType", accountType);

            /*
             * Không truyền lại username/password ra giao diện
             * để tránh form tự hiện lại tài khoản cũ.
             */
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new AppUser());
        }

        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("user") AppUser user,
                           @RequestParam String confirmPassword,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            authService.registerOwner(user, confirmPassword);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đăng ký Owner thành công. Bạn có thể đăng nhập ở mục Owner."
            );

            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("user", user);

            return "auth/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request,
                         HttpServletResponse response) {
        authService.logout(request, response);

        return "redirect:/login";
    }
}