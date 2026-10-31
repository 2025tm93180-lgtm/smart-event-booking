package org.afterglow.eventinventoryservice.service;

import org.afterglow.eventinventoryservice.dto.CreateReservationRequest;
import org.afterglow.eventinventoryservice.dto.CreateSeatsRequest;
import org.afterglow.eventinventoryservice.model.*;
import org.afterglow.eventinventoryservice.repo.EventRepository;
import org.afterglow.eventinventoryservice.repo.SeatRepository;
import org.afterglow.eventinventoryservice.repo.SeatReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryService {
    private static final int HOLD_MINUTES = 10;
    private final SeatRepository seatRepository;
    private final SeatReservationRepository reservationRepository;
    private final EventRepository eventRepository;

    public InventoryService(SeatRepository seatRepository, SeatReservationRepository reservationRepository, EventRepository eventRepository) {
        this.seatRepository = seatRepository; this.reservationRepository = reservationRepository; this.eventRepository = eventRepository;
    }


    @Transactional
    public List<Seat> createSeats(String eventId, CreateSeatsRequest request) {
        System.out.println("Seat creation eventId = [" + eventId + "]");
        System.out.println("Event exists = " + eventRepository.existsById(eventId));
        System.out.println("Event count = " + eventRepository.count());

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event not found"
                ));
        List<String> ids = request.seatIds().stream().map(String::trim).toList();
        if (ids.stream().anyMatch(String::isBlank) || new HashSet<>(ids).size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seat IDs must be nonblank and unique");
        }
        if (seatRepository.findByEventIdOrderBySeatId(eventId).size() + ids.size() > event.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seat count cannot exceed event capacity");
        }
        for (String seatId : ids) {
            if (seatRepository.existsByEventIdAndSeatId(eventId, seatId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat already exists: " + seatId);
        }
        BigDecimal price = request.price();
        List<Seat> seats = ids.stream().map(id -> new Seat(UUID.randomUUID().toString(), eventId, id, price)).toList();
        return seatRepository.saveAll(seats);
    }

    @Transactional(readOnly = true)
    public List<Seat> listSeats(String eventId) {
        if (!eventRepository.existsById(eventId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        return seatRepository.findByEventIdOrderBySeatId(eventId);
    }

    @Transactional
    public SeatReservation reserve(CreateReservationRequest request) {
        List<String> ids = request.seatIds().stream().map(String::trim).distinct().sorted().toList();
        if (ids.isEmpty() || ids.size() != request.seatIds().size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seat IDs must be unique and nonempty");
        if (!eventRepository.existsById(request.eventId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        Optional<SeatReservation> prior = reservationRepository.findByBookingId(request.bookingId());
        if (prior.isPresent()) {
            SeatReservation existing = prior.get();
            if (!existing.getEventId().equals(request.eventId()) || !new HashSet<>(Arrays.asList(existing.getSeatIds().split(","))).equals(new HashSet<>(ids)))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking ID was already used for a different reservation request");
            if (existing.getStatus() == ReservationStatus.HELD && existing.getExpiresAt().isBefore(Instant.now())) expire(existing);
            if (existing.getStatus() == ReservationStatus.EXPIRED || existing.getStatus() == ReservationStatus.RELEASED)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This reservation is no longer active");
            return existing;
        }
        List<Seat> seats = seatRepository.findForUpdate(request.eventId(), ids);
        if (seats.size() != ids.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more seat IDs do not belong to this event");
        Instant now = Instant.now();
        for (Seat seat : seats) {
            if (seat.getStatus() == SeatStatus.HELD && seat.getReservationId() != null) {
                SeatReservation old = reservationRepository.findById(seat.getReservationId()).orElse(null);
                if (old != null && old.getStatus() == ReservationStatus.HELD && !old.getExpiresAt().isAfter(now)) expire(old);
            }
        }
        for (Seat seat : seats) if (seat.getStatus() != SeatStatus.AVAILABLE) throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat unavailable: " + seat.getSeatId());

        BigDecimal total = seats.stream().map(Seat::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        String reservationId = UUID.randomUUID().toString();
        Instant expiry = now.plus(HOLD_MINUTES, ChronoUnit.MINUTES);
        SeatReservation reservation = new SeatReservation(reservationId, request.bookingId(), request.eventId(), String.join(",", ids), total, now, expiry);
        reservationRepository.save(reservation);
        seats.forEach(seat -> { seat.setStatus(SeatStatus.HELD); seat.setReservationId(reservationId); });
        seatRepository.saveAll(seats);
        return reservation;
    }

    @Transactional
    public SeatReservation confirm(String reservationId) {
        SeatReservation reservation = getReservationForUpdate(reservationId);
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) return reservation;
        if (reservation.getStatus() != ReservationStatus.HELD) throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation is not active");
        if (!reservation.getExpiresAt().isAfter(Instant.now())) {
            expire(reservation);
            return reservation;
        }
        List<Seat> seats = seatRepository.findForUpdate(reservation.getEventId(), Arrays.asList(reservation.getSeatIds().split(",")));
        if (seats.stream().anyMatch(s -> !reservationId.equals(s.getReservationId()) || s.getStatus() != SeatStatus.HELD)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat hold is no longer valid");
        seats.forEach(s -> s.setStatus(SeatStatus.RESERVED));
        seatRepository.saveAll(seats);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public SeatReservation release(String reservationId) {
        SeatReservation reservation = getReservationForUpdate(reservationId);
        if (reservation.getStatus() == ReservationStatus.RELEASED || reservation.getStatus() == ReservationStatus.EXPIRED) return reservation;
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) throw new ResponseStatusException(HttpStatus.CONFLICT, "Confirmed reservation cannot be released by this endpoint");
        List<Seat> seats = seatRepository.findForUpdate(reservation.getEventId(), Arrays.asList(reservation.getSeatIds().split(",")));
        seats.stream().filter(s -> reservationId.equals(s.getReservationId()) && s.getStatus() == SeatStatus.HELD).forEach(s -> { s.setStatus(SeatStatus.AVAILABLE); s.setReservationId(null); });
        seatRepository.saveAll(seats);
        reservation.setStatus(ReservationStatus.RELEASED);
        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public SeatReservation getReservation(String id) {
        return reservationRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
    }

    private SeatReservation getReservationForUpdate(String id) {
        SeatReservation r = reservationRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
        return r;
    }

    private void expire(SeatReservation reservation) {
        List<Seat> seats = seatRepository.findForUpdate(reservation.getEventId(), Arrays.asList(reservation.getSeatIds().split(",")));
        seats.stream().filter(s -> reservation.getId().equals(s.getReservationId()) && s.getStatus() == SeatStatus.HELD).forEach(s -> { s.setStatus(SeatStatus.AVAILABLE); s.setReservationId(null); });
        seatRepository.saveAll(seats);
        reservation.setStatus(ReservationStatus.EXPIRED);
        reservationRepository.save(reservation);
    }

    @Scheduled(fixedDelayString = "${inventory.expiry-scan-ms:30000}")
    @Transactional
    public void expireHolds() {
        for (SeatReservation reservation : reservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.HELD, Instant.now())) expire(reservation);
    }
}
