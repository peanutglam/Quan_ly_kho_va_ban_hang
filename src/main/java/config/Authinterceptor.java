package config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import service.AuthService;

@Component
public class Authinterceptor implements HandlerInterceptor {

    /*
     * Các path công khai - không cần đăng nhập.
     * Trang "/" là trang đầu tiên của web và sẽ redirect sang /shop.
     */
    private static final String[] PUBLIC_PREFIXES = {
            "/",
            "/shop",
            "/cart",
            "/checkout",
            "/order-success",
            "/login",
            "/register",
            "/css",
            "/js",
            "/images",
            "/img",
            "/webjars",
            "/favicon",
            "/error"
    };

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String uri = request.getRequestURI();

        if (isPublicPath(uri)) {
            return true;
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

    private boolean isPublicPath(String uri) {
        if (uri == null) {
            return false;
        }

        for (String prefix : PUBLIC_PREFIXES) {
            if ("/".equals(prefix)) {
                if ("/".equals(uri)) {
                    return true;
                }
            } else {
                if (uri.equals(prefix) || uri.startsWith(prefix + "/")) {
                    return true;
                }
            }
        }

        return false;
    }
}