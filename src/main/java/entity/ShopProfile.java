package entity;

import jakarta.persistence.*;

@Entity
@Table(name = "shop_profiles")
public class ShopProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String shopName;

    private String slogan;

    private String phone;

    private String address;

    private String logoUrl;

    @Column(name = "hero_title")
    private String heroTitle;

    @Column(name = "hero_subtitle", columnDefinition = "TEXT")
    private String heroSubtitle;

    @Column(name = "banner_image_url", columnDefinition = "TEXT")
    private String bannerImageUrl;

    @Column(name = "theme_color")
    private String themeColor;

    @Column(name = "facebook_url", columnDefinition = "TEXT")
    private String facebookUrl;

    @Column(name = "zalo_phone")
    private String zaloPhone;

    @Column(name = "shop_notice", columnDefinition = "TEXT")
    private String shopNotice;

    @Column(columnDefinition = "TEXT")
    private String thankYouMessage;

    @Column(columnDefinition = "TEXT")
    private String invoiceFooter;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    public Long getId() {
        return id;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = trim(shopName);
    }

    public String getSlogan() {
        return slogan;
    }

    public void setSlogan(String slogan) {
        this.slogan = trim(slogan);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = trim(phone);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = trim(address);
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = trim(logoUrl);
    }

    public String getHeroTitle() {
        return heroTitle;
    }

    public void setHeroTitle(String heroTitle) {
        this.heroTitle = trim(heroTitle);
    }

    public String getHeroSubtitle() {
        return heroSubtitle;
    }

    public void setHeroSubtitle(String heroSubtitle) {
        this.heroSubtitle = trim(heroSubtitle);
    }

    public String getBannerImageUrl() {
        return bannerImageUrl;
    }

    public void setBannerImageUrl(String bannerImageUrl) {
        this.bannerImageUrl = trim(bannerImageUrl);
    }

    public String getThemeColor() {
        return themeColor;
    }

    public void setThemeColor(String themeColor) {
        this.themeColor = trim(themeColor);
    }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public void setFacebookUrl(String facebookUrl) {
        this.facebookUrl = trim(facebookUrl);
    }

    public String getZaloPhone() {
        return zaloPhone;
    }

    public void setZaloPhone(String zaloPhone) {
        this.zaloPhone = trim(zaloPhone);
    }

    public String getShopNotice() {
        return shopNotice;
    }

    public void setShopNotice(String shopNotice) {
        this.shopNotice = trim(shopNotice);
    }

    public String getThankYouMessage() {
        return thankYouMessage;
    }

    public void setThankYouMessage(String thankYouMessage) {
        this.thankYouMessage = trim(thankYouMessage);
    }

    public String getInvoiceFooter() {
        return invoiceFooter;
    }

    public void setInvoiceFooter(String invoiceFooter) {
        this.invoiceFooter = trim(invoiceFooter);
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    @Transient
    public String getDisplayShopName() {
        return isBlank(shopName) ? "SmartInventory Store" : shopName;
    }

    @Transient
    public String getDisplaySlogan() {
        return isBlank(slogan) ? "Mua sắm mỹ phẩm chính hãng, dễ dàng và nhanh chóng" : slogan;
    }

    @Transient
    public String getDisplayHeroTitle() {
        return isBlank(heroTitle) ? getDisplayShopName() : heroTitle;
    }

    @Transient
    public String getDisplayHeroSubtitle() {
        return isBlank(heroSubtitle)
                ? "Khám phá các sản phẩm mỹ phẩm đang có sẵn tại cửa hàng."
                : heroSubtitle;
    }

    @Transient
    public String getDisplayThemeColor() {
        return isBlank(themeColor) ? "#dc6b98" : themeColor;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}