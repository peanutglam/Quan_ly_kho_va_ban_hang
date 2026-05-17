package com.samgiabao.smartInventory;

import entity.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderMoneyFieldsTest {

    @Test
    void totalBillFollowsProductTotalAndShippingWhenBillIsNotExplicit() {
        Order order = new Order();

        order.setTotalAmount(new BigDecimal("100.00"));
        order.setShippingFee(new BigDecimal("10.00"));

        assertEquals(new BigDecimal("110.00"), order.getTotalBill());
        assertEquals(new BigDecimal("110.00"), order.getRemainingAmount());
    }

    @Test
    void explicitImportedTotalBillIsPreservedAndRemainingAmountUsesDeposit() {
        Order order = new Order();

        order.setTotalAmount(new BigDecimal("100.00"));
        order.setShippingFee(new BigDecimal("10.00"));
        order.setTotalBill(new BigDecimal("125.00"));
        order.setCustomerDeposit(new BigDecimal("25.00"));

        assertEquals(new BigDecimal("125.00"), order.getTotalBill());
        assertEquals(new BigDecimal("100.00"), order.getRemainingAmount());
    }
}
