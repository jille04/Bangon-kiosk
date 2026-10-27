package com.bangon.kiosk.controller;

import com.bangon.kiosk.dto.ApiDtos.Checkout;
import com.bangon.kiosk.dto.ApiDtos.Line;
import com.bangon.kiosk.dto.ApiDtos.ProfileUpdate;
import com.bangon.kiosk.entity.Customer;
import com.bangon.kiosk.service.CustomerService;
import com.bangon.kiosk.service.MenuService;
import com.bangon.kiosk.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** REST endpoints for the menu, quotes, customer profile and orders. */
@RestController
@RequestMapping("/api")
public class KioskController {

    private final MenuService menu;
    private final CustomerService customers;
    private final OrderService orders;

    public KioskController(MenuService menu, CustomerService customers, OrderService orders) {
        this.menu = menu;
        this.customers = customers;
        this.orders = orders;
    }

    // ---------- Menu ----------

    @GetMapping("/categories")
    public List<String> categories() {
        return menu.categoryNames();
    }

    @GetMapping("/products")
    public List<Map<String, Object>> products(@RequestParam(required = false) String category,
                                              @RequestParam(required = false) String search) {
        return menu.search(category, search);
    }

    @GetMapping("/products/{id}")
    public Map<String, Object> product(@PathVariable Long id) {
        return menu.details(id);
    }

    @GetMapping("/products/{id}/options")
    public Map<String, Object> options(@PathVariable Long id) {
        return menu.options(id);
    }

    /** Server-side price and nutrition estimate for one cart line. */
    @PostMapping("/quote")
    public Object quote(@RequestBody Line line) {
        return menu.quote(line.productId(), line.choices());
    }

    // ---------- Customer profile ----------

    @GetMapping("/customers/me")
    public Object me(Authentication auth) {
        return customers.profile(customers.require(auth));
    }

    @PutMapping("/customers/me")
    public Object update(@Valid @RequestBody ProfileUpdate req, Authentication auth) {
        Customer updated = customers.updateProfile(customers.require(auth), req);
        return customers.profile(updated);
    }

    // ---------- Orders ----------

    @PostMapping("/orders")
    public Object order(@Valid @RequestBody Checkout req, Authentication auth) {
        return orders.view(orders.place(req, customers.current(auth)));
    }

    @GetMapping("/orders/{id}")
    public Object order(@PathVariable Long id, Authentication auth) {
        return orders.view(orders.findOwned(id, customers.current(auth)));
    }

    @GetMapping("/customers/me/orders")
    public Object history(Authentication auth) {
        return orders.history(customers.require(auth));
    }

    /** Admin only: confirms that staff verified the payment. */
    @PostMapping("/admin/orders/{id}/confirm-paid")
    public Object markPaid(@PathVariable Long id) {
        return orders.view(orders.confirmPaid(id));
    }
}
