package com.bangon.kiosk.service;

import com.bangon.kiosk.dto.ApiDtos.ProfileUpdate;
import com.bangon.kiosk.dto.ApiDtos.Register;
import com.bangon.kiosk.entity.Customer;
import com.bangon.kiosk.repository.CustomerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

/** Handles the signed-in customer and their profile settings. */
@Service
public class CustomerService {

    private final CustomerRepository customers;
    private final PasswordEncoder encoder;

    public CustomerService(CustomerRepository customers, PasswordEncoder encoder) {
        this.customers = customers;
        this.encoder = encoder;
    }

    /** Registers a new customer with a hashed password; each email can be used once. */
    public void register(Register request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (customers.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        Customer customer = new Customer();
        customer.setName(request.name().trim());
        customer.setEmail(email);
        customer.setPasswordHash(encoder.encode(request.password()));
        customers.save(customer);
    }

    /** Returns the signed-in customer, or null for guests. */
    public Customer current(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return customers.findByEmailIgnoreCase(auth.getName()).orElse(null);
    }

    /** Returns the signed-in customer or fails if nobody is signed in. */
    public Customer require(Authentication auth) {
        Customer c = current(auth);
        if (c == null) {
            throw new IllegalArgumentException("Not signed in");
        }
        return c;
    }

    public Customer updateProfile(Customer c, ProfileUpdate req) {
        c.setCaffeineLimit(req.caffeineLimit());
        c.setCalorieGoal(req.calorieGoal());
        c.setLactoseIntolerant(req.lactoseIntolerant());
        return customers.save(c);
    }

    public Map<String, Object> profile(Customer c) {
        return Map.of(
                "id", c.getId(),
                "name", c.getName(),
                "email", c.getEmail(),
                "caffeineLimit", c.getCaffeineLimit(),
                "calorieGoal", c.getCalorieGoal(),
                "lactoseIntolerant", c.isLactoseIntolerant(),
                "points", c.getLoyaltyPoints());
    }
}
