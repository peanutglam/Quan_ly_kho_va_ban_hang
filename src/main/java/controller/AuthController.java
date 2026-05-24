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
    public String loginForm(HttpServletRequest request,
                            HttpServletResponse response) {
        disableCache(response);

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AuthService.SESSION_USER_ID) != null) {
            return redirectAfterLogin();
        }

        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam(required = false) String username,
                        @RequestParam(required = false) String password,
                        HttpServletResponse response,
                        Model model) {
        disableCache(response);

        try {
            authService.login(username, password);
            return redirectAfterLogin();
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/login";
        }
    }

    @GetMapping("/customer/login")
    public String customerLoginForm(HttpServletRequest request,
                                    HttpServletResponse response) {
        disableCache(response);

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AuthService.SESSION_USER_ID) != null) {
            return redirectAfterLogin();
        }

        return "auth/customer-login";
    }

    @PostMapping("/customer/login")
    public String customerLogin(@RequestParam(required = false) String username,
                                @RequestParam(required = false) String password,
                                HttpServletResponse response,
                                Model model) {
        disableCache(response);

        try {
            authService.login(username, password);
            return redirectAfterLogin();
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/customer-login";
        }
    }

    @GetMapping("/customer/register")
    public String customerRegisterForm(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new AppUser());
        }

        return "auth/customer-register";
    }

    @PostMapping("/customer/register")
    public String customerRegister(@ModelAttribute("user") AppUser user,
                                   @RequestParam String confirmPassword,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            authService.registerCustomer(user, confirmPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký tài khoản mua hàng thành công. Bạn có thể đăng nhập ngay.");
            return "redirect:/customer/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("user", user);
            return "auth/customer-register";
        }
    }

    @GetMapping("/register")
    public String registerDisabled(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Hệ thống không hỗ trợ đăng ký Owner công khai. Khách mua hàng vui lòng dùng mục đăng ký tài khoản khách hàng."
        );

        return "redirect:/customer/register";
    }

    @PostMapping("/register")
    public String registerPostDisabled(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Không hỗ trợ đăng ký Owner công khai. Vui lòng đăng ký tài khoản khách hàng hoặc đăng nhập bằng tài khoản được cấp."
        );

        return "redirect:/customer/register";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request,
                         HttpServletResponse response) {
        authService.logout(request, response);
        disableCache(response);

        return "redirect:/shop";
    }

    private String redirectAfterLogin() {
        try {
            AppUser user = authService.getCurrentUser();
            if (user != null && AppUser.ROLE_CUSTOMER.equalsIgnoreCase(normalizeRole(user.getRole()))) {
                return "redirect:/shop";
            }
        } catch (Exception ignored) {
        }

        return "redirect:/dashboard";
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }

        String value = role.trim().toUpperCase();
        return value.startsWith("ROLE_") ? value.substring(5) : value;
    }

    private void disableCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}