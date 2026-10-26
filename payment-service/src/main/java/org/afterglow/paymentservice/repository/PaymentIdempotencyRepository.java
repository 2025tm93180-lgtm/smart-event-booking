package org.afterglow.paymentservice.repository;

import org.afterglow.paymentservice.model.PaymentIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentIdempotencyRepository extends JpaRepository<PaymentIdempotency, Long> {
    Optional<PaymentIdempotency> findByIdempotencyKey(String idempotencyKey);
}
