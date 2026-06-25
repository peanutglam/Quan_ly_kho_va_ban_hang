package dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RangeReportDTO {

    private LocalDate startDate;
    private LocalDate endDate;

    private long totalOrders;
    private long pendingOrders;
    private long shippingOrders;
    private long completedOrders;
    private long cancelledOrders;

    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal costOfGoods = BigDecimal.ZERO;
    private BigDecimal grossProfit = BigDecimal.ZERO;

    private long totalItemsSold;

    private BigDecimal importTotal = BigDecimal.ZERO;
    private long totalItemsImported;

    private List<DailyReportDTO.OrderSummary> orderSummaries = new ArrayList<>();
    private List<DailyReportDTO.ProductSummary> productSummaries = new ArrayList<>();

    public RangeReportDTO() {
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getShippingOrders() {
        return shippingOrders;
    }

    public void setShippingOrders(long shippingOrders) {
        this.shippingOrders = shippingOrders;
    }

    public long getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(long completedOrders) {
        this.completedOrders = completedOrders;
    }

    public long getCancelledOrders() {
        return cancelledOrders;
    }

    public void setCancelledOrders(long cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }

    public BigDecimal getCostOfGoods() {
        return costOfGoods;
    }

    public void setCostOfGoods(BigDecimal costOfGoods) {
        this.costOfGoods = costOfGoods == null ? BigDecimal.ZERO : costOfGoods;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit;
    }

    public void setGrossProfit(BigDecimal grossProfit) {
        this.grossProfit = grossProfit == null ? BigDecimal.ZERO : grossProfit;
    }

    public long getTotalItemsSold() {
        return totalItemsSold;
    }

    public void setTotalItemsSold(long totalItemsSold) {
        this.totalItemsSold = totalItemsSold;
    }

    public BigDecimal getImportTotal() {
        return importTotal;
    }

    public void setImportTotal(BigDecimal importTotal) {
        this.importTotal = importTotal == null ? BigDecimal.ZERO : importTotal;
    }

    public long getTotalItemsImported() {
        return totalItemsImported;
    }

    public void setTotalItemsImported(long totalItemsImported) {
        this.totalItemsImported = totalItemsImported;
    }

    public List<DailyReportDTO.OrderSummary> getOrderSummaries() {
        return orderSummaries;
    }

    public void setOrderSummaries(List<DailyReportDTO.OrderSummary> orderSummaries) {
        this.orderSummaries = orderSummaries == null ? new ArrayList<>() : orderSummaries;
    }

    public List<DailyReportDTO.ProductSummary> getProductSummaries() {
        return productSummaries;
    }

    public void setProductSummaries(List<DailyReportDTO.ProductSummary> productSummaries) {
        this.productSummaries = productSummaries == null ? new ArrayList<>() : productSummaries;
    }
}