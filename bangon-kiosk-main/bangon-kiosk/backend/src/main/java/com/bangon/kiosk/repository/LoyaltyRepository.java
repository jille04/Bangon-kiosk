package com.bangon.kiosk.repository;

import com.bangon.kiosk.entity.LoyaltyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for loyalty point transactions. */
public interface LoyaltyRepository extends JpaRepository<LoyaltyTransaction, Long> {
}
