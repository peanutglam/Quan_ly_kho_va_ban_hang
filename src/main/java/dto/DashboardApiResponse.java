package dto;

import java.math.BigDecimal;

public class DashboardApiResponse {

    private long totalProducts;
    private long totalSuppliers;
    private long totalOrders;
    private long totalImports;
    private BigDecimal totalRevenue;
    private long pendingOrders;
    private long shippingOrders;
    private long completedOrders;
    private long cancelledOrders;
    private long lowStockCount;
    private long expiringCount;

    public DashboardApiResponse() {
    }

    public DashboardApiResponse(long totalProducts,
                                long totalSuppliers,
                                long totalOrders,
                                long totalImports,
                                BigDecimal totalRevenue,
                                long pendingOrders,
                                long shippingOrders,
                                long completedOrders,
                                long cancelledOrders,
                                long lowStockCount,
                                long expiringCount) {
        this.totalProducts = totalProducts;
        this.totalSuppliers = totalSuppliers;
        this.totalOrders = totalOrders;
        this.totalImports = totalImports;
        this.totalRevenue = totalRevenue == null ? BigDecimal.ZERO : totalRevenue;
        this.pendingOrders = pendingOrders;
        this.shippingOrders = shippingOrders;
        this.completedOrders = completedOrders;
        this.cancelledOrders = cancelledOrders;
        this.lowStockCount = lowStockCount;
        this.expiringCount = expiringCount;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public long getTotalSuppliers() {
        return totalSuppliers;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public long getTotalImports() {
        return totalImports;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public long getShippingOrders() {
        return shippingOrders;
    }

    public long getCompletedOrders() {
        return completedOrders;
    }

    public long getCancelledOrders() {
        return cancelledOrders;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public long getExpiringCount() {
        return expiringCount;
    }
}