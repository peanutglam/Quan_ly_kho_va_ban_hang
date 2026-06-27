package config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import service.AuthService;

@Component
public class Authinterceptor implements HandlerInterceptor {

    private static final String[] PUBLIC_PREFIXES = {
            "/login",
            "/register",

            "/css/",
            "/js/",
            "/images/",
            "/img/",
            "/uploads/",
            "/webjars/",
            "/favicon",
            "/error",

            "/shop",
            "/cart",
            "/checkout",
            "/order-success",

            /*
             * Cho phép toàn bộ khu vực khách hàng đi qua interceptor quản trị.
             * Các trang /customer/account, /customer/orders...
             * sẽ tự kiểm tra session khách hàng trong CustomerAccountController.
             */
            "/customer",

            "/api/auth/login"
    };

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String uri = request.getRequestURI();

        for (String prefix : PUBLIC_PREFIXES) {
            if (uri.equals(prefix)
                    || uri.startsWith(prefix + "/")
                    || uri.startsWith(prefix + "?")) {
                return true;
            }
        }

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute(AuthService.SESSION_USER_ID) == null) {
            if (uri.startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"Bạn cần đăng nhập\"}");
                return false;
            }

            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        response.setHeader("Pragma", "no-cache");

        return true;
    }
}