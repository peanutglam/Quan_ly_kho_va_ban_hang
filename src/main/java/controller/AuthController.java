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
import service.CustomerAccountService;

@Controller
public class AuthController {

    private final AuthService authService;
    private final CustomerAccountService customerAccountService;

    public AuthController(AuthService authService,
                          CustomerAccountService customerAccountService) {
        this.authService = authService;
        this.customerAccountService = customerAccountService;
    }

    /*
     * Trang đăng nhập quản trị.
     * Mỗi lần mở /login sẽ xóa session cũ để không tự vào lại tài khoản trước.
     */
    @GetMapping("/login")
    public String loginForm(HttpServletRequest request,
                            HttpServletResponse response) {
        disableCache(response);
        clearAllLoginSessions(request);

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
            return redirectAfterAdminLogin();
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/login";
        }
    }

    /*
     * Trang đăng nhập khách hàng.
     * Mỗi lần mở /customer/login cũng xóa session cũ.
     */
    @GetMapping("/customer/login")
    public String customerLoginForm(HttpServletRequest request,
                                    HttpServletResponse response) {
        disableCache(response);
        clearAllLoginSessions(request);

        return "auth/customer-login";
    }

    @PostMapping("/customer/login")
    public String customerLogin(@RequestParam(required = false) String username,
                                @RequestParam(required = false) String password,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                Model model) {
        disableCache(response);

        try {
            AppUser customer = customerAccountService.login(username, password);

            HttpSession session = request.getSession(true);
            session.removeAttribute(AuthService.SESSION_USER_ID);

            customerAccountService.saveCustomerToSession(request, customer);

            return "redirect:/customer/account";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/customer-login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Đăng nhập thất bại: " + e.getMessage());
            return "auth/customer-login";
        }
    }

    @GetMapping("/customer/register")
    public String customerRegisterForm(HttpServletRequest request,
                                       Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new AppUser());
        }

        return "auth/customer-register";
    }

    @PostMapping("/customer/register")
    public String customerRegister(@ModelAttribute("user") AppUser user,
                                   @RequestParam String confirmPassword,
                                   HttpServletRequest request,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            String rawPassword = user.getPassword();

            if (rawPassword == null || rawPassword.isBlank()) {
                throw new IllegalArgumentException("Mật khẩu không được để trống.");
            }

            if (confirmPassword == null || !confirmPassword.equals(rawPassword)) {
                throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
            }

            AppUser savedCustomer = customerAccountService.register(
                    user.getFullName(),
                    user.getUsername(),
                    rawPassword,
                    user.getEmail(),
                    user.getPhone(),
                    user.getAddress()
            );

            HttpSession session = request.getSession(true);
            session.removeAttribute(AuthService.SESSION_USER_ID);

            customerAccountService.saveCustomerToSession(request, savedCustomer);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đăng ký tài khoản mua hàng thành công."
            );

            return "redirect:/customer/account";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("user", user);
            return "auth/customer-register";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Đăng ký thất bại: " + e.getMessage());
            model.addAttribute("user", user);
            return "auth/customer-register";
        }
    }

    @GetMapping("/customer/logout")
    public String customerLogout(HttpServletRequest request,
                                 HttpServletResponse response,
                                 RedirectAttributes redirectAttributes) {
        customerAccountService.logout(request);
        disableCache(response);

        redirectAttributes.addFlashAttribute("successMessage", "Đã đăng xuất tài khoản khách hàng.");

        return "redirect:/shop";
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
        customerAccountService.logout(request);
        clearAllLoginSessions(request);
        disableCache(response);

        return "redirect:/shop";
    }

    private String redirectAfterAdminLogin() {
        try {
            AppUser user = authService.getCurrentUser();

            if (user != null && AppUser.ROLE_CUSTOMER.equalsIgnoreCase(normalizeRole(user.getRole()))) {
                return "redirect:/customer/account";
            }
        } catch (Exception ignored) {
        }

        return "redirect:/dashboard";
    }

    private void clearAllLoginSessions(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.removeAttribute(AuthService.SESSION_USER_ID);
            session.removeAttribute(CustomerAccountService.SESSION_CUSTOMER_ID);
            session.invalidate();
        }
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