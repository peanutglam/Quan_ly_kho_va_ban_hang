package controller;

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
            return "redirect:/dashboard-warmup?ts=" + System.currentTimeMillis();
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

            /*
             * Không redirect thẳng vào Dashboard nữa.
             * Đi qua warmup để tránh lỗi Dashboard request đầu tiên bị 0.
             */
            return "redirect:/dashboard-warmup?login=1&ts=" + System.currentTimeMillis();
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerDisabled(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Hệ thống không hỗ trợ đăng ký công khai. Tài khoản Owner được cấp sẵn khi triển khai ứng dụng."
        );

        return "redirect:/login";
    }

    @PostMapping("/register")
    public String registerPostDisabled(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Hệ thống không hỗ trợ đăng ký công khai. Vui lòng đăng nhập bằng tài khoản được cấp."
        );

        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request,
                         HttpServletResponse response) {
        authService.logout(request, response);
        disableCache(response);

        return "redirect:/login?logout=" + System.currentTimeMillis();
    }

    private void disableCache(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }
}