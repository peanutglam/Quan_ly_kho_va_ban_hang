package service;

import entity.AppUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    public static final String SESSION_USER_ID = "currentUserId";
    public static final String SESSION_CURRENT_USER = "currentUser";

    public static final String ACCOUNT_TYPE_OWNER = "OWNER";
    public static final String ACCOUNT_TYPE_EMPLOYEE = "EMPLOYEE";

    private static final int SESSION_TIMEOUT_SECONDS = 30 * 60;

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser registerOwner(AppUser user, String confirmPassword) {
        validateCommonUserInfo(user);
        validatePassword(user.getPassword(), confirmPassword);
        validateUsernameUnique(user.getUsername());

        user.setRole(AppUser.ROLE_OWNER);
        user.setOwner(null);
        user.setActive(true);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    @Transactional
    public AppUser register(AppUser user) {
        return registerOwner(user, user.getPassword());
    }

    @Transactional
    public AppUser login(String username, String password, String accountType) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ tài khoản và mật khẩu");
        }

        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);

        AppUser user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new IllegalArgumentException("Sai tài khoản hoặc mật khẩu"));

        if (Boolean.FALSE.equals(user.getActive())) {
            throw new IllegalArgumentException("Tài khoản này đã bị khóa hoặc đã bị xóa");
        }

        if (!isPasswordCorrect(user, password)) {
            throw new IllegalArgumentException("Sai tài khoản hoặc mật khẩu");
        }

        String type = normalizeAccountType(accountType);
        String role = normalizeRole(user.getRole());

        if (ACCOUNT_TYPE_OWNER.equals(type) && !AppUser.ROLE_OWNER.equals(role)) {
            throw new IllegalArgumentException("Đây không phải tài khoản Owner. Vui lòng đăng nhập ở mục Employee.");
        }

        if (ACCOUNT_TYPE_EMPLOYEE.equals(type)) {
            if (AppUser.ROLE_OWNER.equals(role)) {
                throw new IllegalArgumentException("Đây là tài khoản Owner. Vui lòng đăng nhập ở mục Owner.");
            }

            if (user.getOwner() == null || Boolean.FALSE.equals(user.getOwner().getActive())) {
                throw new IllegalArgumentException("Tài khoản Employee chưa được gắn với Owner hợp lệ");
            }
        }

        if (user.getPassword() != null && !user.getPassword().startsWith("$2")) {
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
        }

        startAuthenticatedSession(user);
        return user;
    }

    @Transactional
    public AppUser login(String username, String password) {
        return login(username, password, ACCOUNT_TYPE_OWNER);
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

        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Phiên đăng nhập không còn hợp lệ"));

        if (Boolean.FALSE.equals(user.getActive())) {
            throw new IllegalArgumentException("Tài khoản này đã bị khóa hoặc đã bị xóa");
        }

        return user;
    }

    public AppUser getWorkspaceOwner() {
        AppUser currentUser = getCurrentUser();
        return getWorkspaceOwner(currentUser);
    }

    public AppUser getWorkspaceOwner(AppUser user) {
        if (user == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập");
        }

        String role = normalizeRole(user.getRole());

        if (AppUser.ROLE_OWNER.equals(role)) {
            return user;
        }

        if (user.getOwner() == null) {
            throw new IllegalArgumentException("Tài khoản Employee chưa được gắn với Owner");
        }

        return user.getOwner();
    }

    public void logout(HttpServletRequest request,
                       HttpServletResponse response) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.removeAttribute(SESSION_USER_ID);
            session.removeAttribute(SESSION_CURRENT_USER);
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
        String currentRole = normalizeRole(user.getRole());

        for (String role : roles) {
            if (currentRole.equals(normalizeRole(role))) {
                return true;
            }
        }

        return false;
    }

    public void requireRole(String... roles) {
        if (!hasRole(roles)) {
            throw new SecurityException("Bạn không có quyền truy cập chức năng này");
        }
    }

    @Transactional
    public void changePassword(String oldPassword,
                               String newPassword,
                               String confirmPassword) {
        AppUser user = getCurrentUser();

        validatePassword(newPassword, confirmPassword);

        if (!isPasswordCorrect(user, oldPassword)) {
            throw new IllegalArgumentException("Mật khẩu cũ không đúng");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshSessionUser(user);
    }

    public List<AppUser> getUsersInCurrentWorkspace() {
        requireRole(AppUser.ROLE_OWNER);

        AppUser owner = getWorkspaceOwner();

        List<AppUser> users = new ArrayList<>();
        users.add(owner);
        users.addAll(userRepository.findByOwnerOrderByIdDesc(owner));

        return users;
    }

    @Transactional
    public AppUser createEmployeeAccount(AppUser user,
                                         String confirmPassword) {
        requireRole(AppUser.ROLE_OWNER);

        AppUser owner = getWorkspaceOwner();

        validateCommonUserInfo(user);
        validatePassword(user.getPassword(), confirmPassword);
        validateUsernameUnique(user.getUsername());

        String role = normalizeRole(user.getRole());

        if (!AppUser.ROLE_STAFF.equals(role) && !AppUser.ROLE_SALE.equals(role)) {
            throw new IllegalArgumentException("Owner chỉ được tạo tài khoản Employee với vai trò STAFF hoặc SALE");
        }

        user.setRole(role);
        user.setOwner(owner);
        user.setActive(true);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    @Transactional
    public void createStaffAccount(AppUser user) {
        createEmployeeAccount(user, user.getPassword());
    }

    @Transactional
    public void deleteEmployee(Long employeeId) {
        requireRole(AppUser.ROLE_OWNER);

        AppUser owner = getWorkspaceOwner();

        AppUser employee = userRepository.findByIdAndOwner(employeeId, owner)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Employee thuộc web của bạn"));

        if (AppUser.ROLE_OWNER.equals(normalizeRole(employee.getRole()))) {
            throw new IllegalArgumentException("Không thể xóa Owner ở chức năng xóa Employee");
        }

        employee.setActive(false);
        userRepository.save(employee);
    }

    @Transactional
    public void deleteUser(Long id) {
        deleteEmployee(id);
    }

    @Transactional
    public void deleteCurrentAccount(String password,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        AppUser currentUser = getCurrentUser();

        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu để xác nhận xóa tài khoản");
        }

        if (!isPasswordCorrect(currentUser, password)) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không đúng");
        }

        if (AppUser.ROLE_OWNER.equals(normalizeRole(currentUser.getRole()))) {
            List<AppUser> employees = userRepository.findByOwnerOrderByIdDesc(currentUser);

            for (AppUser employee : employees) {
                employee.setActive(false);
            }

            userRepository.saveAll(employees);
        }

        currentUser.setActive(false);
        userRepository.save(currentUser);

        logout(request, response);
    }

    private void startAuthenticatedSession(AppUser user) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();

        HttpServletRequest request = attributes.getRequest();

        HttpSession oldSession = request.getSession(false);

        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession newSession = request.getSession(true);
        newSession.setMaxInactiveInterval(30 * 60);
        newSession.setAttribute(SESSION_USER_ID, user.getId());
        newSession.setAttribute(SESSION_CURRENT_USER, user);
    }

    private void refreshSessionUser(AppUser user) {
        HttpSession session = currentSession(false);

        if (session != null) {
            session.setAttribute(SESSION_CURRENT_USER, user);
        }
    }

    private HttpSession currentSession(boolean create) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập");
        }

        return attributes.getRequest().getSession(create);
    }

    private void validateCommonUserInfo(AppUser user) {
        if (user == null) {
            throw new IllegalArgumentException("Thông tin tài khoản không hợp lệ");
        }

        if (!StringUtils.hasText(user.getFullName())) {
            throw new IllegalArgumentException("Vui lòng nhập họ tên");
        }

        if (!StringUtils.hasText(user.getUsername())) {
            throw new IllegalArgumentException("Vui lòng nhập tên đăng nhập");
        }

        String username = user.getUsername().trim().toLowerCase(Locale.ROOT);
        user.setUsername(username);

        if (!username.matches("^[a-zA-Z0-9_.-]{4,60}$")) {
            throw new IllegalArgumentException(
                    "Tên đăng nhập phải từ 4 ký tự, chỉ gồm chữ, số, dấu gạch dưới, dấu chấm hoặc gạch ngang"
            );
        }

        if (StringUtils.hasText(user.getEmail())
                && !user.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Email không đúng định dạng");
        }

        if (StringUtils.hasText(user.getPhone())
                && !user.getPhone().matches("^[0-9+() .-]{8,20}$")) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng");
        }
    }

    private void validatePassword(String password,
                                  String confirmPassword) {
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu");
        }

        if (password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        }

        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Xác nhận mật khẩu không khớp");
        }
    }

    private void validateUsernameUnique(String username) {
        String normalizedUsername = username == null ? null : username.trim().toLowerCase(Locale.ROOT);

        if (normalizedUsername != null && userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại trong hệ thống");
        }
    }

    private boolean isPasswordCorrect(AppUser user, String rawPassword) {
        if (user == null || rawPassword == null || user.getPassword() == null) {
            return false;
        }

        if (user.getPassword().startsWith("$2")) {
            return passwordEncoder.matches(rawPassword, user.getPassword());
        }

        return user.getPassword().equals(rawPassword);
    }

    private String normalizeAccountType(String rawType) {
        if (!StringUtils.hasText(rawType)) {
            return ACCOUNT_TYPE_OWNER;
        }

        String value = rawType.trim().toUpperCase(Locale.ROOT);

        if (ACCOUNT_TYPE_EMPLOYEE.equals(value)) {
            return ACCOUNT_TYPE_EMPLOYEE;
        }

        return ACCOUNT_TYPE_OWNER;
    }

    private String normalizeRole(String rawRole) {
        if (!StringUtils.hasText(rawRole)) {
            return AppUser.ROLE_OWNER;
        }

        String value = rawRole.trim().toUpperCase(Locale.ROOT);

        if (value.startsWith("ROLE_")) {
            value = value.substring(5);
        }

        if ("ADMIN".equals(value)) {
            return AppUser.ROLE_OWNER;
        }

        return value;
    }
}