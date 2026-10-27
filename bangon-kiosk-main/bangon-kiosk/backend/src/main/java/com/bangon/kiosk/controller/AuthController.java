
package com.bangon.kiosk.controller;

import com.bangon.kiosk.dto.ApiDtos.*;
import com.bangon.kiosk.service.CustomerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Endpoints for CSRF tokens, registration, login and logout. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CustomerService customers;
    private final AuthenticationManager auth;

    public AuthController(
            CustomerService customers,
            PasswordEncoder encoder,
            UserDetailsService users) {

        this.customers = customers;

        // Configure password authentication
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(encoder);
        provider.setUserDetailsService(users);
        this.auth = new ProviderManager(provider);
    }

    // CSRF token
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    // Customer registration
    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody Register r) {
        customers.register(r);
        return Map.of("message", "Registered. Please sign in.");
    }

    // Customer login
    @PostMapping("/login")
    public Map<String, Object> login(
            @Valid @RequestBody Login r,
            HttpServletRequest request,
            HttpServletResponse response) {

        var authentication = auth.authenticate(
                new UsernamePasswordAuthenticationToken(
                        r.email().trim(), r.password())
        );

        // Rotate session ID after authentication
        HttpServletRequest authenticatedRequest = request;

        if (request.getSession(false) == null) {
            request.getSession(true);
        } else {
            request.changeSessionId();
        }

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        HttpSessionSecurityContextRepository repository =
                new HttpSessionSecurityContextRepository();

        repository.saveContext(
                context,
                authenticatedRequest,
                response
        );

        return Map.of("message", "Signed in");
    }

    // Customer logout
    @PostMapping("/logout")
    public Map<String, String> logout(
            HttpServletRequest request) {

        var session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        return Map.of("message", "Signed out");
    }
}