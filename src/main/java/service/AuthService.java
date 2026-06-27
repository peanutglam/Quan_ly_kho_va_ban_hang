package service;

import entity.AppUser;
import entity.Order;
import entity.Product;
import entity.StockImport;
import entity.Supplier;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import repository.OrderRepository;
import repository.ProductRepository;
import repository.StockImportRepository;
import repository.SupplierRepository;
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

    @Value("${app.owner.username:owner}")
    private String configuredOwnerUsername;

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final StockImportRepository stockImportRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       ProductRepository productRepository,
                       OrderRepository orderRepository,
                       SupplierRepository supplierRepository,
                       StockImportRepository stockImportRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.supplierRepository = supplierRepository;
        this.stockImportRepository = stockImportRepository;
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
    public AppUser registerCustomer(AppUser user, String confirmPassword) {
        validateCommonUserInfo(user);
        validatePassword(user.getPassword(), confirmPassword);
        validateUsernameUnique(user.getUsername());

        AppUser owner = getCanonicalOwner();
        owner = normalizeCanonicalOwner(owner);

        user.setRole(AppUser.ROLE_CUSTOMER);
        user.setOwner(owner);
        user.setActive(true);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    @Transactional
    public AppUser login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ tài khoản và mật khẩu");
        }

        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);

        AppUser user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new IllegalArgumentException("Sai tài khoản hoặc mật khẩu"));

        if (Boolean.FALSE.equals(user.getActive())) {
            throw new IllegalArgumentException("Tài khoản này đã bị khóa hoặc không còn hoạt động");
        }

        if (!isPasswordCorrect(user, password)) {
            throw new IllegalArgumentException("Sai tài khoản hoặc mật khẩu");
        }

        AppUser canonicalOwner = getCanonicalOwner();
        canonicalOwner = normalizeCanonicalOwner(canonicalOwner);

        String role = normalizeRole(user.getRole());

        if (AppUser.ROLE_OWNER.equals(role)) {
            if (!sameUser(user, canonicalOwner)) {
                throw new IllegalArgumentException(
                        "Chỉ tài khoản Owner chính được phép đăng nhập. Vui lòng dùng tài khoản: "
                                + canonicalOwner.getUsername()
                );
            }

            user = canonicalOwner;
        } else {
            if (user.getOwner() == null
                    || user.getOwner().getId() == null
                    || !user.getOwner().getId().equals(canonicalOwner.getId())
                    || Boolean.FALSE.equals(user.getOwner().getActive())) {

                user.setOwner(canonicalOwner);
                user = userRepository.save(user);
            }
        }

        if (user.getPassword() != null && !user.getPassword().startsWith("$2")) {
            user.setPassword(passwordEncoder.encode(password));
            user = userRepository.save(user);
        }

        if (!AppUser.ROLE_CUSTOMER.equals(role)) {
            repairDataOwnership(canonicalOwner);
        }

        startAuthenticatedSession(user);
        return user;
    }

    @Transactional
    public AppUser login(String username, String password, String accountType) {
        return login(username, password);
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
            throw new IllegalArgumentException("Tài khoản này đã bị khóa hoặc không còn hoạt động");
        }

        return user;
    }

    public AppUser getWorkspaceOwner() {
        getCurrentUser();
        return getCanonicalOwner();
    }
    public AppUser getSystemOwner() {
        return getCanonicalOwner();
    }
    public AppUser getWorkspaceOwner(AppUser user) {
        if (user == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập");
        }

        return getCanonicalOwner();
    }

    private AppUser getCanonicalOwner() {
        String ownerUsername = configuredOwnerUsername == null
                ? "owner"
                : configuredOwnerUsername.trim().toLowerCase(Locale.ROOT);

        return userRepository.findFirstByUsernameOrderByIdAsc(ownerUsername)
                .or(() -> userRepository.findFirstByUsernameAndActiveTrueOrderByIdAsc(ownerUsername))
                .or(() -> userRepository.findFirstByRoleAndActiveTrueOrderByIdAsc(AppUser.ROLE_OWNER))
                .or(() -> userRepository.findFirstByRoleOrderByIdAsc(AppUser.ROLE_OWNER))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Chưa có tài khoản Owner chính. Vui lòng kiểm tra OwnerAccountInitializer hoặc database."
                ));
    }

    @Transactional
    protected AppUser normalizeCanonicalOwner(AppUser owner) {
        if (owner == null) {
            throw new IllegalArgumentException("Chưa có tài khoản Owner chính");
        }

        boolean changed = false;

        String normalizedUsername = owner.getUsername() == null
                ? configuredOwnerUsername
                : owner.getUsername().trim().toLowerCase(Locale.ROOT);

        if (!normalizedUsername.equals(owner.getUsername())) {
            owner.setUsername(normalizedUsername);
            changed = true;
        }

        if (!AppUser.ROLE_OWNER.equals(normalizeRole(owner.getRole()))) {
            owner.setRole(AppUser.ROLE_OWNER);
            changed = true;
        }

        if (owner.getOwner() != null) {
            owner.setOwner(null);
            changed = true;
        }

        if (Boolean.FALSE.equals(owner.getActive())) {
            owner.setActive(true);
            changed = true;
        }

        if (changed) {
            owner = userRepository.save(owner);
        }

        return owner;
    }

    @Transactional
    protected void repairDataOwnership(AppUser owner) {
        if (owner == null || owner.getId() == null) {
            return;
        }

        repairProducts(owner);
        repairOrders(owner);
        repairSuppliers(owner);
        repairStockImports(owner);
        repairEmployees(owner);
        disableExtraOwners(owner);
    }

    private void repairProducts(AppUser owner) {
        List<Product> changedProducts = new ArrayList<>();

        for (Product product : productRepository.findAll()) {
            if (!sameUser(product.getUser(), owner)) {
                product.setUser(owner);
                changedProducts.add(product);
            }
        }

        if (!changedProducts.isEmpty()) {
            productRepository.saveAll(changedProducts);
        }
    }

    private void repairOrders(AppUser owner) {
        List<Order> changedOrders = new ArrayList<>();

        for (Order order : orderRepository.findAll()) {
            if (!sameUser(order.getUser(), owner)) {
                order.setUser(owner);
                changedOrders.add(order);
            }
        }

        if (!changedOrders.isEmpty()) {
            orderRepository.saveAll(changedOrders);
        }
    }

    private void repairSuppliers(AppUser owner) {
        List<Supplier> changedSuppliers = new ArrayList<>();

        for (Supplier supplier : supplierRepository.findAll()) {
            if (!sameUser(supplier.getUser(), owner)) {
                supplier.setUser(owner);
                changedSuppliers.add(supplier);
            }
        }

        if (!changedSuppliers.isEmpty()) {
            supplierRepository.saveAll(changedSuppliers);
        }
    }

    private void repairStockImports(AppUser owner) {
        List<StockImport> changedImports = new ArrayList<>();

        for (StockImport stockImport : stockImportRepository.findAll()) {
            if (!sameUser(stockImport.getUser(), owner)) {
                stockImport.setUser(owner);
                changedImports.add(stockImport);
            }
        }

        if (!changedImports.isEmpty()) {
            stockImportRepository.saveAll(changedImports);
        }
    }

    private void repairEmployees(AppUser owner) {
        List<AppUser> changedUsers = new ArrayList<>();

        for (AppUser user : userRepository.findAll()) {
            if (user.getId() == null || user.getId().equals(owner.getId())) {
                continue;
            }

            String role = normalizeRole(user.getRole());

            if (!AppUser.ROLE_OWNER.equals(role) && !sameUser(user.getOwner(), owner)) {
                user.setOwner(owner);
                changedUsers.add(user);
            }
        }

        if (!changedUsers.isEmpty()) {
            userRepository.saveAll(changedUsers);
        }
    }

    private void disableExtraOwners(AppUser owner) {
        List<AppUser> changedUsers = new ArrayList<>();

        for (AppUser otherOwner : userRepository.findByRoleAndActiveTrueOrderByIdAsc(AppUser.ROLE_OWNER)) {
            if (otherOwner.getId() != null
                    && !otherOwner.getId().equals(owner.getId())
                    && Boolean.TRUE.equals(otherOwner.getActive())) {
                otherOwner.setActive(false);
                changedUsers.add(otherOwner);
            }
        }

        if (!changedUsers.isEmpty()) {
            userRepository.saveAll(changedUsers);
        }
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

    public boolean isOwner() {
        return hasRole(AppUser.ROLE_OWNER);
    }

    public boolean isStockStaff() {
        return hasRole(AppUser.ROLE_STAFF);
    }

    public boolean isSaleStaff() {
        return hasRole(AppUser.ROLE_SALE);
    }

    public boolean canViewProducts() {
        return hasRole(AppUser.ROLE_OWNER, AppUser.ROLE_STAFF, AppUser.ROLE_SALE);
    }

    public boolean canManageProducts() {
        return hasRole(AppUser.ROLE_OWNER, AppUser.ROLE_STAFF);
    }

    public boolean canManageSuppliers() {
        return hasRole(AppUser.ROLE_OWNER, AppUser.ROLE_STAFF);
    }

    public boolean canManageImports() {
        return hasRole(AppUser.ROLE_OWNER, AppUser.ROLE_STAFF);
    }

    public boolean canManageOrders() {
        return hasRole(AppUser.ROLE_OWNER, AppUser.ROLE_SALE);
    }

    public boolean canViewReports() {
        return hasRole(AppUser.ROLE_OWNER);
    }

    public boolean canManageShopConfig() {
        return hasRole(AppUser.ROLE_OWNER);
    }

    public boolean canManageAccounts() {
        return hasRole(AppUser.ROLE_OWNER);
    }

    public boolean canInventoryCheck() {
        return hasRole(AppUser.ROLE_OWNER, AppUser.ROLE_STAFF);
    }

    public boolean canViewInventoryLogs() {
        return hasRole(AppUser.ROLE_OWNER, AppUser.ROLE_STAFF, AppUser.ROLE_SALE);
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

        for (AppUser user : userRepository.findByOwnerOrderByIdDesc(owner)) {
            String role = normalizeRole(user.getRole());
            if (AppUser.ROLE_STAFF.equals(role) || AppUser.ROLE_SALE.equals(role)) {
                users.add(user);
            }
        }

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
            throw new IllegalArgumentException("Chỉ được tạo tài khoản nhân viên với vai trò STAFF hoặc SALE");
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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản nhân viên thuộc cửa hàng"));

        if (AppUser.ROLE_OWNER.equals(normalizeRole(employee.getRole()))) {
            throw new IllegalArgumentException("Không thể xóa tài khoản Owner chính tại chức năng này");
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
            throw new IllegalArgumentException("Không thể xóa tài khoản Owner chính. Owner là tài khoản được cấp khi triển khai hệ thống.");
        }

        currentUser.setActive(false);
        userRepository.save(currentUser);

        logout(request, response);
    }

    public void verifyOwnerPassword(String ownerPassword) {
        requireRole(AppUser.ROLE_OWNER);

        if (!StringUtils.hasText(ownerPassword)) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu Owner để xác nhận thao tác");
        }

        AppUser owner = getWorkspaceOwner();

        if (!isPasswordCorrect(owner, ownerPassword)) {
            throw new IllegalArgumentException("Mật khẩu Owner không đúng. Không thể thực hiện thao tác xóa.");
        }
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
        newSession.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
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

    private boolean sameUser(AppUser a, AppUser b) {
        return a != null
                && b != null
                && a.getId() != null
                && b.getId() != null
                && a.getId().equals(b.getId());
    }
}