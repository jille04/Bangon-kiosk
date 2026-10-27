package com.bangon.kiosk.service;

import com.bangon.kiosk.dto.ApiDtos.Checkout;
import com.bangon.kiosk.dto.ApiDtos.Line;
import com.bangon.kiosk.entity.*;
import com.bangon.kiosk.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Handles placing orders, confirming payments and loyalty points. */
@Service
public class OrderService {

    private static final Set<String> PAYMENT_METHODS = Set.of("Cash", "Card", "E-Wallet");
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;
    private static final int MAX_QUANTITY = 30;
    private static final int MAX_LINE_ITEMS = 50;
    private static final int POINTS_TO_REDEEM = 100;

    private final ProductRepository products;
    private final CustomerRepository customers;
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final LoyaltyRepository loyalty;
    private final PricingService pricing;

    public OrderService(ProductRepository products, CustomerRepository customers,
                        OrderRepository orders, PaymentRepository payments,
                        LoyaltyRepository loyalty, PricingService pricing) {
        this.products = products;
        this.customers = customers;
        this.orders = orders;
        this.payments = payments;
        this.loyalty = loyalty;
        this.pricing = pricing;
    }

    /**
     * Places a pending order. Prices are always recalculated on the server.
     * A repeated idempotency key returns the original order instead of a duplicate.
     */
    @Transactional
    public Order place(Checkout req, Customer logged) {
        if (req.idempotencyKey().length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new IllegalArgumentException("Idempotency key too long");
        }

        // Return the existing order if this request was already processed.
        Optional<Order> previous = orders.findByIdempotencyKey(req.idempotencyKey());
        if (previous.isPresent()) {
            Order existing = previous.get();
            Long existingOwner = existing.getCustomer() == null ? null : existing.getCustomer().getId();
            Long currentUser = logged == null ? null : logged.getId();
            if (!Objects.equals(existingOwner, currentUser)) {
                throw new IllegalArgumentException("Idempotency key already used");
            }
            return existing;
        }

        if (!PAYMENT_METHODS.contains(req.paymentMethod())) {
            throw new IllegalArgumentException("Unsupported payment method");
        }

        Customer user = logged == null ? null : customers.lockById(logged.getId()).orElseThrow();
        if (req.redeemPoints() && user == null) {
            throw new IllegalArgumentException("Sign in to redeem points");
        }

        Order order = new Order();
        order.setOrderNumber("B" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        order.setIdempotencyKey(req.idempotencyKey());
        order.setCustomer(user);
        order.setPaymentMethod(req.paymentMethod());

        for (Line line : req.items()) {
            addItem(order, line);
        }
        if (order.getItems().size() > MAX_LINE_ITEMS) {
            throw new IllegalArgumentException("Too many line items");
        }

        // The discount can never exceed the subtotal.
        order.setDiscount(pricing.discount(req.redeemPoints(), user == null ? 0 : user.getLoyaltyPoints()));
        if (order.getDiscount().compareTo(order.getSubtotal()) > 0) {
            order.setDiscount(order.getSubtotal());
        }
        order.setTotal(order.getSubtotal().subtract(order.getDiscount()));

        // Points are reserved/deducted only after a staff-verified payment; no fake card/e-wallet success.
        order.setPointsRedeemed(req.redeemPoints() ? POINTS_TO_REDEEM : 0);
        order.setPointsEarned(user == null ? 0 : pricing.earnedPoints(order.getTotal()));

        orders.saveAndFlush(order);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(req.paymentMethod());
        payments.save(payment);

        return order;
    }

    /** Prices one cart line and adds it to the order, updating the running totals. */
    private void addItem(Order order, Line line) {
        if (line.quantity() < 1 || line.quantity() > MAX_QUANTITY) {
            throw new IllegalArgumentException("Invalid quantity");
        }
        Product product = products.findById(line.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        PricingService.Quote quote = pricing.quote(product, line.choices());

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setName(product.getName());
        item.setQuantity(line.quantity());
        item.setUnitPrice(quote.price());
        item.setCaffeine(quote.caffeine());
        item.setCalories(quote.calories());
        item.setSize(quote.size());
        item.setMilk(quote.milk());
        item.setSweetness(quote.sweetness());
        item.setExtraShot(quote.extraShot());
        item.setCaramel(quote.caramel());
        item.setCream(quote.cream());
        order.getItems().add(item);

        order.setSubtotal(order.getSubtotal().add(quote.price().multiply(BigDecimal.valueOf(line.quantity()))));
        order.setCaffeine(order.getCaffeine() + quote.caffeine() * line.quantity());
        order.setCalories(order.getCalories() + quote.calories() * line.quantity());
    }

    /**
     * Marks an order as paid (staff action) and applies loyalty points.
     * Confirming an already-paid order does nothing, so points are never awarded twice.
     */
    @Transactional
    public Order confirmPaid(Long orderId) {
        Order order = orders.lockById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getPaymentStatus().equals("PAID")) {
            return order;
        }

        if (order.getCustomer() != null) {
            Customer user = customers.lockById(order.getCustomer().getId()).orElseThrow();
            if (order.getPointsRedeemed() > 0) {
                if (user.getLoyaltyPoints() < order.getPointsRedeemed()) {
                    throw new IllegalArgumentException("Loyalty balance changed; payment cannot be confirmed");
                }
                user.setLoyaltyPoints(user.getLoyaltyPoints() - order.getPointsRedeemed());
                log(user, order, -order.getPointsRedeemed(), "REDEEM");
            }
            user.setLoyaltyPoints(user.getLoyaltyPoints() + order.getPointsEarned());
            log(user, order, order.getPointsEarned(), "EARN");
        }

        order.setPaymentStatus("PAID");
        order.setStatus("CONFIRMED");
        Payment payment = payments.findByOrderId(order.getId());
        payment.setStatus("PAID");
        payment.setConfirmedAt(Instant.now());
        return order;
    }

    /** Saves a loyalty point transaction. */
    private void log(Customer customer, Order order, int delta, String reason) {
        LoyaltyTransaction t = new LoyaltyTransaction();
        t.setCustomer(customer);
        t.setOrder(order);
        t.setDelta(delta);
        t.setReason(reason);
        loyalty.save(t);
    }

    /** Returns an order only to its owner; guest orders cannot be looked up. */
    public Order findOwned(Long id, Customer user) {
        Order o = orders.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (o.getCustomer() == null || user == null || !o.getCustomer().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not your order");
        }
        return o;
    }

    /** Order history of a customer, newest first. */
    public List<Map<String, Object>> history(Customer user) {
        return orders.findByCustomerIdOrderByCreatedAtDesc(user.getId()).stream().map(this::view).toList();
    }

    /** Converts an order into the JSON structure returned by the API. */
    public Map<String, Object> view(Order o) {
        List<Map<String, Object>> items = o.getItems().stream().map(this::itemView).toList();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("orderNumber", o.getOrderNumber());
        m.put("status", o.getStatus());
        m.put("paymentStatus", o.getPaymentStatus());
        m.put("paymentMethod", o.getPaymentMethod());
        m.put("subtotal", o.getSubtotal());
        m.put("discount", o.getDiscount());
        m.put("total", o.getTotal());
        m.put("pointsEarned", o.getPointsEarned());
        m.put("pointsRedeemed", o.getPointsRedeemed());
        m.put("caffeine", o.getCaffeine());
        m.put("calories", o.getCalories());
        m.put("createdAt", o.getCreatedAt());
        m.put("items", items);
        return m;
    }

    private Map<String, Object> itemView(OrderItem i) {
        String details = i.getSize() == null
                ? "1 item"
                : i.getSize() + " · " + i.getMilk() + " milk · " + i.getSweetness() + "% sweet"
                + (i.isExtraShot() ? " · extra shot" : "")
                + (i.isCaramel() ? " · caramel syrup" : "")
                + (i.isCream() ? " · whipped cream" : "");

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", i.getName());
        m.put("quantity", i.getQuantity());
        m.put("unitPrice", i.getUnitPrice());
        m.put("caffeine", i.getCaffeine());
        m.put("calories", i.getCalories());
        m.put("details", details);
        return m;
    }
}
