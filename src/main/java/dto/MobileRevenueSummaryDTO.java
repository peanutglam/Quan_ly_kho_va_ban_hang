package dto;

import java.math.BigDecimal;

public class MobileRevenueSummaryDTO {

    private long totalOrders;
    private long pendingOrders;
    private long deliveredOrders;
    private long completedOrders;
    private BigDecimal totalRevenue;

    public MobileRevenueSummaryDTO() {
    }

    public MobileRevenueSummaryDTO(long totalOrders,
                                   long pendingOrders,
                                   long deliveredOrders,
                                   long completedOrders,
                                   BigDecimal totalRevenue) {
        this.totalOrders = totalOrders;
        this.pendingOrders = pendingOrders;
        this.deliveredOrders = deliveredOrders;
        this.completedOrders = completedOrders;
        this.totalRevenue = totalRevenue == null ? BigDecimal.ZERO : totalRevenue;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public long getDeliveredOrders() {
        return deliveredOrders;
    }

    public long getCompletedOrders() {
        return completedOrders;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue == null ? BigDecimal.ZERO : totalRevenue;
    }
}