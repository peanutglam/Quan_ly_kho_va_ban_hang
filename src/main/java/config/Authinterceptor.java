package config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import service.AuthService;

@Component
public class Authinterceptor implements HandlerInterceptor {

    /** Các path công khai - không cần đăng nhập */
    private static final String[] PUBLIC_PREFIXES = {
            "/login", "/register",
            "/css/", "/js/", "/images/", "/webjars/", "/favicon", "/error",
            // Trang bán hàng công khai
            "/shop", "/cart", "/checkout", "/order-success"
    };

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String uri = request.getRequestURI();

        for (String prefix : PUBLIC_PREFIXES) {
            if (uri.equals(prefix) || uri.startsWith(prefix + "/") || uri.startsWith(prefix + "?")) {
                return true;
            }
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(AuthService.SESSION_USER_ID) == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        return true;
    }
}