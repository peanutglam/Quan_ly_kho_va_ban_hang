package service;

import entity.AppUser;
import entity.ShopProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.ShopProfileRepository;

@Service
public class ShopProfileService {

    private final ShopProfileRepository shopProfileRepository;
    private final AuthService authService;

    public ShopProfileService(ShopProfileRepository shopProfileRepository,
                              AuthService authService) {
        this.shopProfileRepository = shopProfileRepository;
        this.authService = authService;
    }

    public ShopProfile getCurrentProfile() {
        AppUser owner = authService.getWorkspaceOwner();

        return shopProfileRepository.findByUser(owner).orElseGet(() -> {
            ShopProfile profile = new ShopProfile();

            profile.setShopName("Tên cửa hàng");
            profile.setSlogan("Quản lý kho và bán hàng");
            profile.setPhone("");
            profile.setAddress("");
            profile.setLogoUrl("");
            profile.setThankYouMessage("Cảm ơn quý khách đã mua hàng!");
            profile.setInvoiceFooter("Hóa đơn được tạo tự động từ hệ thống SmartInventory.");
            profile.setUser(owner);

            return shopProfileRepository.save(profile);
        });
    }

    @Transactional
    public void update(ShopProfile form) {
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
}