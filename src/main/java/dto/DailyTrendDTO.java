package dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyTrendDTO {

    private LocalDate date;
    private long orderCount;
    private BigDecimal revenue;

    public DailyTrendDTO() {
    }

    public DailyTrendDTO(LocalDate date, long orderCount, BigDecimal revenue) {
        this.date = date;
        this.orderCount = orderCount;
        this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }

    public LocalDate getDate() {
        return date;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }
}