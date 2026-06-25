package entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "position_x", nullable = false)
    private Integer positionX = 50;

    @Column(name = "position_y", nullable = false)
    private Integer positionY = 50;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "main_image", nullable = false)
    private Boolean mainImage = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    public ProductImage() {
    }

    public Long getId() {
        return id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Integer getPositionX() {
        return positionX == null ? 50 : positionX;
    }

    public Integer getPositionY() {
        return positionY == null ? 50 : positionY;
    }

    public Integer getSortOrder() {
        return sortOrder == null ? 0 : sortOrder;
    }

    public Boolean getMainImage() {
        return mainImage != null && mainImage;
    }

    public Product getProduct() {
        return product;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl == null ? null : imageUrl.trim();
    }

    public void setPositionX(Integer positionX) {
        this.positionX = clamp(positionX);
    }

    public void setPositionY(Integer positionY) {
        this.positionY = clamp(positionY);
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
    }

    public void setMainImage(Boolean mainImage) {
        this.mainImage = mainImage != null && mainImage;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    private Integer clamp(Integer value) {
        if (value == null) {
            return 50;
        }

        if (value < 0) {
            return 0;
        }

        if (value > 100) {
            return 100;
        }

        return value;
    }
}