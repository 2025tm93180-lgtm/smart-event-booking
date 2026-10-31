
package com.smartbooking.booking.repository;

import com.smartbooking.booking.entity.BookingIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingIdempotencyRepository
        extends JpaRepository<BookingIdempotency, Long> {

    Optional<BookingIdempotency> findByCustomerIdAndIdempotencyKey(
            String customerId, String idempotencyKey);
}
