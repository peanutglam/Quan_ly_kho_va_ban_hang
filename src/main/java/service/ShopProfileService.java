package service;

import entity.AppUser;
import entity.ShopProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.ShopProfileRepository;
import repository.UserRepository;

@Service
public class ShopProfileService {

    private final ShopProfileRepository shopProfileRepository;
    private final AuthService authService;
    private final UserRepository userRepository;

    public ShopProfileService(ShopProfileRepository shopProfileRepository,
                              AuthService authService,
                              UserRepository userRepository) {
        this.shopProfileRepository = shopProfileRepository;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @Transactional
    public ShopProfile getCurrentProfile() {
        AppUser owner = authService.getWorkspaceOwner();

        return shopProfileRepository.findByUser(owner)
                .orElseGet(() -> shopProfileRepository.save(createDefaultProfile(owner)));
    }

    @Transactional
    public ShopProfile getPublicProfile() {
        AppUser owner = userRepository
                .findFirstByRoleAndActiveTrueOrderByIdAsc(AppUser.ROLE_OWNER)
                .orElse(null);

        if (owner == null) {
            return createDefaultPublicProfile();
        }

        return shopProfileRepository.findByUser(owner)
                .orElseGet(() -> shopProfileRepository.save(createDefaultProfile(owner)));
    }

    @Transactional
    public void updateInvoiceConfig(ShopProfile form) {
        ShopProfile profile = getCurrentProfile();

        profile.setShopName(form.getShopName());
        profile.setSlogan(form.getSlogan());
        profile.setPhone(form.getPhone());
        profile.setAddress(form.getAddress());
        profile.setLogoUrl(form.getLogoUrl());
        profile.setThankYouMessage(form.getThankYouMessage());
        profile.setInvoiceFooter(form.getInvoiceFooter());

        shopProfileRepository.save(profile);
    }

    @Transactional
    public void updateShopConfig(ShopProfile form) {
        ShopProfile profile = getCurrentProfile();

        profile.setShopName(form.getShopName());
        profile.setSlogan(form.getSlogan());
        profile.setPhone(form.getPhone());
        profile.setAddress(form.getAddress());
        profile.setLogoUrl(form.getLogoUrl());

        profile.setHeroTitle(form.getHeroTitle());
        profile.setHeroSubtitle(form.getHeroSubtitle());
        profile.setBannerImageUrl(form.getBannerImageUrl());
        profile.setThemeColor(normalizeColor(form.getThemeColor()));
        profile.setFacebookUrl(form.getFacebookUrl());
        profile.setZaloPhone(form.getZaloPhone());
        profile.setShopNotice(form.getShopNotice());

        shopProfileRepository.save(profile);
    }

    @Transactional
    public void update(ShopProfile form) {
        ShopProfile profile = getCurrentProfile();

        profile.setShopName(form.getShopName());
        profile.setSlogan(form.getSlogan());
        profile.setPhone(form.getPhone());
        profile.setAddress(form.getAddress());
        profile.setLogoUrl(form.getLogoUrl());

        profile.setHeroTitle(form.getHeroTitle());
        profile.setHeroSubtitle(form.getHeroSubtitle());
        profile.setBannerImageUrl(form.getBannerImageUrl());
        profile.setThemeColor(normalizeColor(form.getThemeColor()));
        profile.setFacebookUrl(form.getFacebookUrl());
        profile.setZaloPhone(form.getZaloPhone());
        profile.setShopNotice(form.getShopNotice());

        profile.setThankYouMessage(form.getThankYouMessage());
        profile.setInvoiceFooter(form.getInvoiceFooter());

        shopProfileRepository.save(profile);
    }

    private ShopProfile createDefaultProfile(AppUser owner) {
        ShopProfile profile = new ShopProfile();

        profile.setShopName("Tên cửa hàng");
        profile.setSlogan("Mua sắm cùng SmartInventory");
        profile.setPhone("");
        profile.setAddress("");
        profile.setLogoUrl("");

        profile.setHeroTitle("Mỹ phẩm chính hãng cho cửa hàng của bạn");
        profile.setHeroSubtitle("Khám phá các sản phẩm đang có sẵn, đặt hàng nhanh và dễ dàng.");
        profile.setBannerImageUrl("");
        profile.setThemeColor("#dc6b98");
        profile.setFacebookUrl("");
        profile.setZaloPhone("");
        profile.setShopNotice("");

        profile.setThankYouMessage("Cảm ơn quý khách đã mua hàng!");
        profile.setInvoiceFooter("Hóa đơn được tạo tự động từ hệ thống SmartInventory.");
        profile.setUser(owner);

        return profile;
    }

    private ShopProfile createDefaultPublicProfile() {
        ShopProfile profile = new ShopProfile();

        profile.setShopName("SmartInventory Store");
        profile.setSlogan("Mua sắm mỹ phẩm dễ dàng và nhanh chóng");
        profile.setPhone("");
        profile.setAddress("");
        profile.setLogoUrl("");

        profile.setHeroTitle("Cửa hàng mỹ phẩm");
        profile.setHeroSubtitle("Khám phá các sản phẩm đang có sẵn tại cửa hàng.");
        profile.setBannerImageUrl("");
        profile.setThemeColor("#dc6b98");
        profile.setFacebookUrl("");
        profile.setZaloPhone("");
        profile.setShopNotice("");

        profile.setThankYouMessage("Cảm ơn quý khách đã mua hàng!");
        profile.setInvoiceFooter("Hóa đơn được tạo tự động từ hệ thống SmartInventory.");

        return profile;
    }

    private String normalizeColor(String color) {
        if (color == null || color.trim().isEmpty()) {
            return "#dc6b98";
        }

        String value = color.trim();

        if (!value.startsWith("#")) {
            value = "#" + value;
        }

        if (!value.matches("^#[0-9a-fA-F]{6}$")) {
            return "#dc6b98";
        }

        return value;
    }
}