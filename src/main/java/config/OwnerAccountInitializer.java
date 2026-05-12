package config;

import entity.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import repository.UserRepository;

import java.util.Locale;

@Component
@org.springframework.core.annotation.Order(1)
public class OwnerAccountInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.owner.username:owner}")
    private String ownerUsername;

    @Value("${app.owner.password:123456}")
    private String ownerPassword;

    @Value("${app.owner.full-name:Chủ cửa hàng}")
    private String ownerFullName;

    @Value("${app.owner.email:}")
    private String ownerEmail;

    @Value("${app.owner.phone:}")
    private String ownerPhone;

    @Value("${app.owner.address:}")
    private String ownerAddress;

    public OwnerAccountInitializer(UserRepository userRepository,
                                   BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        boolean hasOwner = userRepository.findAll()
                .stream()
                .anyMatch(user ->
                        Boolean.TRUE.equals(user.getActive())
                                && AppUser.ROLE_OWNER.equals(normalizeRole(user.getRole()))
                );

        if (hasOwner) {
            return;
        }

        AppUser owner = new AppUser();
        owner.setFullName(ownerFullName);
        owner.setUsername(ownerUsername);
        owner.setPassword(passwordEncoder.encode(ownerPassword));
        owner.setEmail(ownerEmail);
        owner.setPhone(ownerPhone);
        owner.setAddress(ownerAddress);
        owner.setRole(AppUser.ROLE_OWNER);
        owner.setOwner(null);
        owner.setActive(true);

        userRepository.save(owner);

        System.out.println("===============================================");
        System.out.println("Đã tạo tài khoản Owner mặc định:");
        System.out.println("Username: " + ownerUsername);
        System.out.println("Password: " + ownerPassword);
        System.out.println("Vui lòng đăng nhập và đổi mật khẩu sau khi triển khai.");
        System.out.println("===============================================");
    }

    private String normalizeRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
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