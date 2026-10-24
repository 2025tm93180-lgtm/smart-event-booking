
package com.smartbooking.booking.repository;

import com.smartbooking.booking.entity.BookingSaga;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSagaRepository
        extends JpaRepository<BookingSaga, String> {
}
