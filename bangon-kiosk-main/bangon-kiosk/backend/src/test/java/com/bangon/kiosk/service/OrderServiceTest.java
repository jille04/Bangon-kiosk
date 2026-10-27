package com.bangon.kiosk.service;

import com.bangon.kiosk.dto.ApiDtos.Checkout;
import com.bangon.kiosk.dto.ApiDtos.Line;
import com.bangon.kiosk.entity.Category;
import com.bangon.kiosk.entity.Customer;
import com.bangon.kiosk.entity.Order;
import com.bangon.kiosk.entity.Payment;
import com.bangon.kiosk.entity.Product;
import com.bangon.kiosk.repository.CustomerRepository;
import com.bangon.kiosk.repository.LoyaltyRepository;
import com.bangon.kiosk.repository.OrderRepository;
import com.bangon.kiosk.repository.PaymentRepository;
import com.bangon.kiosk.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private ProductRepository products;
    private CustomerRepository customers;
    private OrderRepository orders;
    private PaymentRepository payments;
    private OrderService service;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        customers = mock(CustomerRepository.class);
        orders = mock(OrderRepository.class);
        payments = mock(PaymentRepository.class);
        LoyaltyRepository loyalty = mock(LoyaltyRepository.class);
        service = new OrderService(products, customers, orders, payments, loyalty, new PricingService());

        Product latte = new Product(new Category("Coffee"), "Latte", 150, 120, 220, "", false);
        when(products.findById(1L)).thenReturn(Optional.of(latte));
        when(orders.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
    }

    private Checkout checkout(String method, boolean redeem, String key) {
        return new Checkout(List.of(new Line(1L, 2, null)), method, redeem, key);
    }

    @Test
    void guestOrderTotalsAreCalculatedOnServer() {
        Order order = service.place(checkout("Cash", false, "key-1"), null);

        assertEquals(new BigDecimal("300.00"), order.getTotal());
        assertEquals("PENDING", order.getPaymentStatus());
        assertEquals(0, order.getPointsEarned());
    }

    @Test
    void repeatedIdempotencyKeyReturnsSameOrder() {
        Order existing = new Order();
        when(orders.findByIdempotencyKey("key-2")).thenReturn(Optional.of(existing));

        Order result = service.place(checkout("Cash", false, "key-2"), null);

        assertSame(existing, result);
        verify(orders, never()).saveAndFlush(any());
    }

    @Test
    void rejectsUnsupportedPaymentMethod() {
        assertThrows(IllegalArgumentException.class,
                () -> service.place(checkout("Bitcoin", false, "key-3"), null));
    }

    @Test
    void guestCannotRedeemPoints() {
        assertThrows(IllegalArgumentException.class,
                () -> service.place(checkout("Cash", true, "key-4"), null));
    }

    @Test
    void confirmPaidAwardsPointsOnlyOnce() {
        Customer customer = new Customer();
        customer.setId(5L);
        Order order = new Order();
        order.setId(9L);
        order.setCustomer(customer);
        order.setPointsEarned(6);

        when(orders.lockById(9L)).thenReturn(Optional.of(order));
        when(customers.lockById(5L)).thenReturn(Optional.of(customer));
        when(payments.findByOrderId(anyLong())).thenReturn(new Payment());

        service.confirmPaid(9L);
        service.confirmPaid(9L);

        assertEquals("PAID", order.getPaymentStatus());
        assertEquals(6, customer.getLoyaltyPoints());
    }
}
