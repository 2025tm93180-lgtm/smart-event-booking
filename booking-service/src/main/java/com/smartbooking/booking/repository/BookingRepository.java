
package com.smartbooking.booking.repository;

import com.smartbooking.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository
        extends JpaRepository<Booking, String> {

    List<Booking> findByCustomerId(String customerId);
}
