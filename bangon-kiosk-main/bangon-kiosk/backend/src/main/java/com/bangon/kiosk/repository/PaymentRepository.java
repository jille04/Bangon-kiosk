package com.bangon.kiosk.repository;

import com.bangon.kiosk.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for order payments. */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Payment findByOrderId(Long id);
}
