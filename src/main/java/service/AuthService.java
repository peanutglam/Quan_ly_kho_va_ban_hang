package service;

import entity.AppUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import repository.UserRepository;

import java.util.List;

@Service
public class AuthService {

    public static final String SESSION_USER_ID = "currentUserId";
    private static final int SESSION_TIMEOUT_SECONDS = 30 * 60;

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser register(AppUser user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("OWNER");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public AppUser login(String username, String password) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Sai tài khoản hoặc mật khẩu"));

        boolean valid;
        if (user.getPassword() != null && user.getPassword().startsWith("$2")) {
            valid = passwordEncoder.matches(password, user.getPassword());
        } else {
            valid = user.getPassword() != null && user.getPassword().equals(password);
            if (valid) {
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
            }
        }

        if (!valid) throw new IllegalArgumentException("Sai tài khoản hoặc mật khẩu");

        startAuthenticatedSession(user);
        return user;
    }

    public AppUser getCurrentUser() {
        HttpSession session = currentSession(false);
        if (session == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập");
        }

        Object userId = session.getAttribute(SESSION_USER_ID);
        if (!(userId instanceof Long id)) {
            throw new IllegalArgumentException("Bạn cần đăng nhập");
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiên đăng nhập không còn hợp lệ"));
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_USER_ID);
            session.invalidate();
        }

        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setPath(request.getContextPath().isBlank() ? "/" : request.getContextPath());
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public boolean hasRole(String... roles) {
        AppUser user = getCurrentUser();
        for (String role : roles) {
            if (role.equals(user.getRole())) return true;
        }
        return false;
    }

    public void requireRole(String... roles) {
        if (!hasRole(roles)) throw new SecurityException("Bạn không có quyền truy cập chức năng này");
    }

    public void changePassword(String oldPassword, String newPassword, String confirmPassword) {
        AppUser user = getCurrentUser();
        if (newPassword == null || newPassword.length() < 6) throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
        if (!newPassword.equals(confirmPassword)) throw new IllegalArgumentException("Xác nhận mật khẩu không khớp");

        boolean oldCorrect = user.getPassword() != null && user.getPassword().startsWith("$2")
                ? passwordEncoder.matches(oldPassword, user.getPassword())
                : user.getPassword() != null && user.getPassword().equals(oldPassword);

        if (!oldCorrect) throw new IllegalArgumentException("Mật khẩu cũ không đúng");

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public List<AppUser> getAllUsers() {
        requireRole("OWNER");
        return userRepository.findAllByOrderByIdDesc();
    }

    public void createStaffAccount(AppUser user) {
        requireRole("OWNER");
        if (userRepository.existsByUsername(user.getUsername())) throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        if (!"STAFF".equals(user.getRole()) && !"SALE".equals(user.getRole())) throw new IllegalArgumentException("Role không hợp lệ");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        requireRole("OWNER");
        AppUser currentUser = getCurrentUser();
        AppUser user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
        if (user.getId().equals(currentUser.getId())) throw new IllegalArgumentException("Không thể xóa chính mình");
        userRepository.delete(user);
    }

    private void startAuthenticatedSession(AppUser user) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession newSession = request.getSession(true);
        newSession.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
        newSession.setAttribute(SESSION_USER_ID, user.getId());
    }

    private HttpSession currentSession(boolean create) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập");
        }
        return attributes.getRequest().getSession(create);
    }
}
