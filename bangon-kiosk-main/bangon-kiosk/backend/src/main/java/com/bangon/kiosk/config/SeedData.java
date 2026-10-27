package com.bangon.kiosk.config;

import com.bangon.kiosk.entity.Category;
import com.bangon.kiosk.entity.Product;
import com.bangon.kiosk.repository.CategoryRepository;
import com.bangon.kiosk.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Inserts the default categories and menu items, and refreshes their image links. */
@Configuration
public class SeedData {

    private static final String[] CATEGORIES = {"Coffee", "Frappes", "Tea", "Pastries", "Desserts"};

    /** Each row: category, name, price, caffeine (mg), calories, image URL, is pastry. */
    private static final Object[][] MENU = {
            {"Coffee", "Caramel Latte", 150, 120, 220,
                    "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=900&q=82", false},
            {"Coffee", "Spanish Latte", 145, 115, 210,
                    "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?auto=format&fit=crop&w=900&q=82", false},
            {"Coffee", "Americano", 95, 150, 15,
                    "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=900&q=82", false},
            {"Coffee", "Cappuccino", 125, 120, 120,
                    "https://images.unsplash.com/photo-1572442388796-11668a67e53d?auto=format&fit=crop&w=900&q=82", false},
            {"Frappes", "Mocha Frappe", 175, 95, 390,
                    "https://images.unsplash.com/photo-1511920170033-f8396924c348?auto=format&fit=crop&w=900&q=82", false},
            {"Frappes", "Caramel Frappe", 180, 70, 430,
                    "https://budgetdelicious.com/wp-content/uploads/2021/02/caramel-frappe-17-of-21.jpg", false},
            {"Tea", "Matcha Latte", 145, 65, 200,
                    "https://images.unsplash.com/photo-1742790104414-c3a42088942d?auto=format&fit=crop&w=900&q=82", false},
            {"Tea", "Strawberry Tea Latte", 135, 35, 190,
                    "https://images.unsplash.com/photo-1641919089328-5d5063828c4f?auto=format&fit=crop&w=900&q=82", false},
            {"Pastries", "Butter Croissant", 85, 0, 280,
                    "https://images.unsplash.com/photo-1555507036-ab1f4038808a?auto=format&fit=crop&w=900&q=82", true},
            {"Pastries", "Cinnamon Roll", 105, 0, 410,
                    "https://images.unsplash.com/photo-1730235341656-b07152f891a9?auto=format&fit=crop&w=900&q=82", true},
            {"Desserts", "Chocolate Cake", 125, 12, 390,
                    "https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=900&q=82", true},
            {"Desserts", "Cheesecake", 135, 0, 350,
                    "https://images.unsplash.com/photo-1754711596655-04ad921bd050?auto=format&fit=crop&w=900&q=82", true}
    };

    @Bean
    CommandLineRunner seed(CategoryRepository categories, ProductRepository products) {
        return args -> {
            for (String name : CATEGORIES) {
                if (!categories.existsByName(name)) {
                    categories.save(new Category(name));
                }
            }

            for (Object[] row : MENU) {
                String name = (String) row[1];
                String image = (String) row[5];
                Product existing = products.findByName(name);
                if (existing == null) {
                    Category category = categories.findByName((String) row[0]);
                    products.save(new Product(category, name, (int) row[2], (int) row[3],
                            (int) row[4], image, (boolean) row[6]));
                } else if (!image.equals(existing.getImage())) {
                    // Keep image links up to date for databases seeded earlier.
                    existing.setImage(image);
                    products.save(existing);
                }
            }
        };
    }
}
