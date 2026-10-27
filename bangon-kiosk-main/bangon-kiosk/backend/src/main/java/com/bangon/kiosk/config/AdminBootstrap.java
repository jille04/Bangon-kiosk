package com.bangon.kiosk.config;

import com.bangon.kiosk.entity.Customer;
import com.bangon.kiosk.repository.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;

/** Creates the administrator account from environment variables on first startup. */
@Configuration
public class AdminBootstrap {

    private static final int MIN_PASSWORD_LENGTH = 12;

    @Bean
    CommandLineRunner admin(CustomerRepository repo, PasswordEncoder encoder) {
        return args -> {
            String email = System.getenv("ADMIN_EMAIL");
            String password = System.getenv("ADMIN_PASSWORD");

            // Skip when not configured or the password is too short.
            if (email == null || password == null || password.length() < MIN_PASSWORD_LENGTH) {
                return;
            }

            // Never elevate or overwrite an existing user.
            if (repo.findByEmailIgnoreCase(email).isEmpty()) {
                Customer admin = new Customer();
                admin.setEmail(email.trim().toLowerCase(Locale.ROOT));
                admin.setName("Kiosk Administrator");
                admin.setRole("ADMIN");
                admin.setPasswordHash(encoder.encode(password));
                repo.save(admin);
            }
        };
    }
}
