package config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import service.AuthService;

@Component
public class Authinterceptor implements HandlerInterceptor {

    private static final String[] PUBLIC_PATHS = {
            "/login", "/register", "/css/", "/js/", "/images/", "/webjars/", "/favicon", "/error"
    };

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String uri = request.getRequestURI();

        for (String pub : PUBLIC_PATHS) {
            if (uri.startsWith(pub) || uri.equals(pub)) {
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
        response.setHeader("Expires", "0");
        return true;
    }
}
