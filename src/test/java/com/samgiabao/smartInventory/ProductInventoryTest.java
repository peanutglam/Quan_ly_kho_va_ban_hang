package com.samgiabao.smartInventory;

import entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductInventoryTest {

    @Test
    void registerSaleUpdatesAvailableQuantityImmediately() {
        Product product = new Product();
        product.setImportPrice(new BigDecimal("10.00"));
        product.setSalePrice(new BigDecimal("15.00"));
        product.setTotalQuantity(5);
        product.recalculateInventoryFields();

        product.registerSale(3);

        assertEquals(3, product.getSoldQuantity());
        assertEquals(2, product.getQuantity());
        assertEquals(2, product.getInventoryQuantity());
        assertEquals(new BigDecimal("15.00"), product.getProfit());
    }

    @Test
    void stockOperationsStayConsistentAcrossMultipleChangesBeforePersist() {
        Product product = new Product();
        product.setTotalQuantity(5);
        product.recalculateInventoryFields();

        product.registerSale(3);
        product.registerSale(2);
        product.restoreSale(1);
        product.increaseStock(4);

        assertEquals(9, product.getTotalQuantity());
        assertEquals(4, product.getSoldQuantity());
        assertEquals(5, product.getQuantity());
        assertEquals(5, product.getInventoryQuantity());
    }
}
