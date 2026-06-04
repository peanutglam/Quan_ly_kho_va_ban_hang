package dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CreateOrderApiRequest {

    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private BigDecimal shippingFee = BigDecimal.ZERO;
    private BigDecimal customerDeposit = BigDecimal.ZERO;
    private String note;
    private List<CreateOrderApiItemRequest> items = new ArrayList<>();

    public CreateOrderApiRequest() {
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee;
    }

    public BigDecimal getCustomerDeposit() {
        return customerDeposit;
    }

    public void setCustomerDeposit(BigDecimal customerDeposit) {
        this.customerDeposit = customerDeposit;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<CreateOrderApiItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CreateOrderApiItemRequest> items) {
        this.items = items;
    }
}