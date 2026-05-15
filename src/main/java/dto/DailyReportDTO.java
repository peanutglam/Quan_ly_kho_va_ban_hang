package dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DailyReportDTO {

    private LocalDate date;

    // Đơn hàng
    private long totalOrders;
    private long pendingOrders;
    private long shippingOrders;
    private long completedOrders;
    private long cancelledOrders;

    // Tài chính bán hàng
    private BigDecimal revenue       = BigDecimal.ZERO;  // tổng tiền đơn đã giao/hoàn thành
    private BigDecimal costOfGoods   = BigDecimal.ZERO;  // tổng vốn hàng bán (costPrice * qty)
    private BigDecimal grossProfit   = BigDecimal.ZERO;  // revenue - costOfGoods
    private long totalItemsSold;

    // Nhập hàng
    private BigDecimal importTotal   = BigDecimal.ZERO;  // tổng tiền nhập hàng
    private long totalItemsImported;

    // Chi tiết đơn hàng trong ngày
    private List<OrderSummary> orderSummaries = new ArrayList<>();

    // Chi tiết sản phẩm bán trong ngày
    private List<ProductSummary> productSummaries = new ArrayList<>();

    // ---- nested ----
    public record OrderSummary(
            String orderCode, String customerName, String status,
            BigDecimal totalBill, BigDecimal shipping
    ) {}

    public record ProductSummary(
            String productName, long qtySold, BigDecimal revenue, BigDecimal cost, BigDecimal profit
    ) {}

    // ---- getters / setters ----
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate d) { this.date = d; }
    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long v) { this.totalOrders = v; }
    public long getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(long v) { this.pendingOrders = v; }
    public long getShippingOrders() { return shippingOrders; }
    public void setShippingOrders(long v) { this.shippingOrders = v; }
    public long getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(long v) { this.completedOrders = v; }
    public long getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(long v) { this.cancelledOrders = v; }
    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal v) { this.revenue = v == null ? BigDecimal.ZERO : v; }
    public BigDecimal getCostOfGoods() { return costOfGoods; }
    public void setCostOfGoods(BigDecimal v) { this.costOfGoods = v == null ? BigDecimal.ZERO : v; }
    public BigDecimal getGrossProfit() { return grossProfit; }
    public void setGrossProfit(BigDecimal v) { this.grossProfit = v == null ? BigDecimal.ZERO : v; }
    public long getTotalItemsSold() { return totalItemsSold; }
    public void setTotalItemsSold(long v) { this.totalItemsSold = v; }
    public BigDecimal getImportTotal() { return importTotal; }
    public void setImportTotal(BigDecimal v) { this.importTotal = v == null ? BigDecimal.ZERO : v; }
    public long getTotalItemsImported() { return totalItemsImported; }
    public void setTotalItemsImported(long v) { this.totalItemsImported = v; }
    public List<OrderSummary> getOrderSummaries() { return orderSummaries; }
    public void setOrderSummaries(List<OrderSummary> v) { this.orderSummaries = v; }
    public List<ProductSummary> getProductSummaries() { return productSummaries; }
    public void setProductSummaries(List<ProductSummary> v) { this.productSummaries = v; }
}
