package com.bangon.kiosk.repository;

import com.bangon.kiosk.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for menu categories. */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);

    Category findByName(String name);
}
