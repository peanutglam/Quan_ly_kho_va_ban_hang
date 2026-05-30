package config;

import entity.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import service.AuthService;

@Component
public class Authinterceptor implements HandlerInterceptor {

    private static final String[] PUBLIC_PREFIXES = {
            "/",
            "/shop",
            "/cart",
            "/checkout",
            "/order-success",
            "/login",
            "/register",
            "/customer/login",
            "/customer/register",
            "/api",
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

        Object currentUser = session.getAttribute(AuthService.SESSION_CURRENT_USER);
        if (currentUser instanceof AppUser user && AppUser.ROLE_CUSTOMER.equals(normalizeRole(user.getRole()))) {
            response.sendRedirect(request.getContextPath() + "/shop");
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
            } else if (uri.equals(prefix) || uri.startsWith(prefix + "/")) {
                return true;
            }
        }

        return false;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }

        String value = role.trim().toUpperCase();
        return value.startsWith("ROLE_") ? value.substring(5) : value;
    }
}
