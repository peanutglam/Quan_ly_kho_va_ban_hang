package dto;

public class UpdateOrderStatusApiRequest {

    private String status;
    private String newStatus;
    private String orderStatus;

    public UpdateOrderStatusApiRequest() {
    }

    public String getStatus() {
        if (status != null && !status.trim().isEmpty()) {
            return status.trim();
        }

        if (newStatus != null && !newStatus.trim().isEmpty()) {
            return newStatus.trim();
        }

        if (orderStatus != null && !orderStatus.trim().isEmpty()) {
            return orderStatus.trim();
        }

        return "";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
}