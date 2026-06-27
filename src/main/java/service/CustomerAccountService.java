package service;

import entity.AppUser;
import entity.Order;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.AppUserRepository;
import repository.OrderRepository;

@Service
public class CustomerAccountService {

    public static final String SESSION_CUSTOMER_ID = "CUSTOMER_USER_ID";

    private final AppUserRepository appUserRepository;
    private final OrderRepository orderRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CustomerAccountService(AppUserRepository appUserRepository,
                                  OrderRepository orderRepository) {
        this.appUserRepository = appUserRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public AppUser register(String fullName,
                            String username,
                            String password,
                            String email,
                            String phone,
                            String address) {
        String normalizedUsername = normalizeUsername(username);

        if (normalizedUsername == null || normalizedUsername.isBlank()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống");
        }

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        }

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Họ tên không được để trống");
        }

        if (appUserRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }

        AppUser customer = new AppUser();
        customer.setFullName(fullName);
        customer.setUsername(normalizedUsername);
        customer.setPassword(passwordEncoder.encode(password));
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setAddress(address);
        customer.setRole(AppUser.ROLE_CUSTOMER);
        customer.setOwner(getPublicOwner());
        customer.setActive(true);

        return appUserRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public AppUser login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);

        AppUser customer = appUserRepository.findByUsernameAndActiveTrue(normalizedUsername)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản hoặc mật khẩu không đúng"));

        if (!AppUser.ROLE_CUSTOMER.equals(customer.getRole())) {
            throw new IllegalArgumentException("Tài khoản này không phải tài khoản khách hàng");
        }

        if (!passwordEncoder.matches(password == null ? "" : password, customer.getPassword())) {
            throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không đúng");
        }

        return customer;
    }

    public void saveCustomerToSession(HttpServletRequest request, AppUser customer) {
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_CUSTOMER_ID, customer.getId());
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.removeAttribute(SESSION_CUSTOMER_ID);
        }
    }

    @Transactional(readOnly = true)
    public AppUser getCurrentCustomer(HttpServletRequest request) {
        Long customerId = getCurrentCustomerId(request);

        if (customerId == null) {
            throw new IllegalStateException("Bạn cần đăng nhập tài khoản khách hàng");
        }

        AppUser customer = appUserRepository.findById(customerId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản khách hàng"));

        if (!AppUser.ROLE_CUSTOMER.equals(customer.getRole()) || !customer.getActive()) {
            throw new IllegalStateException("Tài khoản khách hàng không hợp lệ");
        }

        return customer;
    }

    @Transactional(readOnly = true)
    public AppUser getCurrentCustomerOrNull(HttpServletRequest request) {
        try {
            return getCurrentCustomer(request);
        } catch (Exception e) {
            return null;
        }
    }

    public Long getCurrentCustomerId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            return null;
        }

        Object value = session.getAttribute(SESSION_CUSTOMER_ID);

        if (value instanceof Long id) {
            return id;
        }

        if (value instanceof Integer id) {
            return id.longValue();
        }

        return null;
    }

    @Transactional
    public AppUser updateProfile(HttpServletRequest request,
                                 String fullName,
                                 String email,
                                 String phone,
                                 String address,
                                 String currentPassword,
                                 String newPassword) {
        AppUser customer = getCurrentCustomer(request);

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Họ tên không được để trống");
        }

        customer.setFullName(fullName);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setAddress(address);

        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
            }

            if (currentPassword == null || !passwordEncoder.matches(currentPassword, customer.getPassword())) {
                throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
            }

            customer.setPassword(passwordEncoder.encode(newPassword));
        }

        return appUserRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public Page<Order> getMyOrders(HttpServletRequest request, int page, int size) {
        AppUser customer = getCurrentCustomer(request);
        AppUser owner = customer.getOwner() != null ? customer.getOwner() : getPublicOwner();

        String phone = customer.getPhone() == null ? "" : customer.getPhone().trim();

        return orderRepository.findCustomerOrders(
                owner,
                customer,
                phone,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50))
        );
    }

    @Transactional(readOnly = true)
    public Order getMyOrderDetail(HttpServletRequest request, Long orderId) {
        AppUser customer = getCurrentCustomer(request);
        AppUser owner = customer.getOwner() != null ? customer.getOwner() : getPublicOwner();

        String phone = customer.getPhone() == null ? "" : customer.getPhone().trim();

        return orderRepository.findCustomerOrderDetail(orderId, owner, customer, phone)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng của bạn"));
    }

    @Transactional(readOnly = true)
    public AppUser getPublicOwner() {
        return appUserRepository.findFirstByRoleAndActiveTrueOrderByIdAsc(AppUser.ROLE_OWNER)
                .orElseThrow(() -> new IllegalStateException("Chưa có tài khoản Owner trong hệ thống"));
    }

    private String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase();
    }
}