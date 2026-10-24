
package com.smartbooking.booking.repository;

import com.smartbooking.booking.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingSeatRepository
        extends JpaRepository<BookingSeat, Long> {

    List<BookingSeat> findByBookingId(String bookingId);
}
