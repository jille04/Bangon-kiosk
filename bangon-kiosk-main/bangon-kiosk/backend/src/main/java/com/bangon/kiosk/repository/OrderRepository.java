package com.bangon.kiosk.repository;

import com.bangon.kiosk.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Database access for orders. */
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdempotencyKey(String key);

    /** Loads an order with a write lock so it cannot be confirmed twice. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> lockById(Long id);

    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
