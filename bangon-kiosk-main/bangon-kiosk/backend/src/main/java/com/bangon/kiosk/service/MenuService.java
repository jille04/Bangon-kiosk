package com.bangon.kiosk.service;

import com.bangon.kiosk.dto.ApiDtos.Choices;
import com.bangon.kiosk.entity.Category;
import com.bangon.kiosk.entity.Product;
import com.bangon.kiosk.repository.CategoryRepository;
import com.bangon.kiosk.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Provides the menu: categories, products, customization options and quotes. */
@Service
public class MenuService {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final PricingService pricing;

    public MenuService(ProductRepository products, CategoryRepository categories, PricingService pricing) {
        this.products = products;
        this.categories = categories;
        this.pricing = pricing;
    }

    public List<String> categoryNames() {
        return categories.findAll().stream().map(Category::getName).toList();
    }

    /** Lists active products, optionally filtered by category and search text. */
    public List<Map<String, Object>> search(String category, String search) {
        return products.findByActiveTrueOrderByIdAsc().stream()
                .filter(p -> category == null || p.getCategory().getName().equalsIgnoreCase(category))
                .filter(p -> search == null || p.getName().toLowerCase().contains(search.toLowerCase()))
                .map(this::view)
                .toList();
    }

    public Map<String, Object> details(Long id) {
        return view(find(id));
    }

    /** Customization options; pastries and desserts cannot be customized. */
    public Map<String, Object> options(Long id) {
        if (find(id).isPastry()) {
            return Map.of("customizable", false);
        }
        return Map.of(
                "customizable", true,
                "sizes", List.of("small", "medium", "large"),
                "milk", List.of("whole", "oat", "almond"),
                "sweetness", List.of(25, 50, 100),
                "addons", List.of("extraShot", "caramel", "cream"));
    }

    /** Price and nutrition estimate for one cart line. */
    public PricingService.Quote quote(Long productId, Choices choices) {
        return pricing.quote(find(productId), choices);
    }

    private Product find(Long id) {
        return products.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    private Map<String, Object> view(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("category", p.getCategory().getName());
        m.put("name", p.getName());
        m.put("price", p.getPrice());
        m.put("caffeine", p.getCaffeine());
        m.put("calories", p.getCalories());
        m.put("image", p.getImage());
        m.put("pastry", p.isPastry());
        return m;
    }
}
