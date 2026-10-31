
package com.smartbooking.booking.service;

import com.smartbooking.booking.dto.CreateReservationRequest;
import com.smartbooking.booking.dto.InventoryReservationResponse;
import com.smartbooking.booking.dto.InventorySeatResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@Service
public class InventoryClient {

    private final RestClient inventoryRestClient;

    public InventoryClient(RestClient inventoryRestClient) {
        this.inventoryRestClient = inventoryRestClient;
    }

    public List<InventorySeatResponse> getEventSeats(String eventId) {
        try {
            InventorySeatResponse[] seats = inventoryRestClient.get()
                    .uri("/api/events/{eventId}/seats", eventId)
                    .retrieve()
                    .body(InventorySeatResponse[].class);

            if (seats == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Inventory Service returned an empty response"
                );
            }

            return Arrays.asList(seats);

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to retrieve seats from Inventory Service",
                    ex
            );
        }
    }

    public InventoryReservationResponse reserveSeats(
            String bookingId,
            String eventId,
            List<String> seatIds) {

        try {
            InventoryReservationResponse reservation =
                    inventoryRestClient.post()
                            .uri("/api/reservations")
                            .body(new CreateReservationRequest(
                                    bookingId,
                                    eventId,
                                    seatIds
                            ))
                            .retrieve()
                            .body(InventoryReservationResponse.class);

            if (reservation == null || reservation.id() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Inventory Service returned an invalid reservation"
                );
            }

            return reservation;

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to reserve seats with Inventory Service",
                    ex
            );
        }
    }

    public void releaseReservation(String reservationId) {
        try {
            inventoryRestClient.post()
                    .uri("/api/reservations/{reservationId}/release",
                            reservationId)
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to release inventory reservation",
                    ex
            );
        }
    }

    public InventoryReservationResponse confirmReservation(String reservationId) {
        try {
            InventoryReservationResponse reservation =
                    inventoryRestClient.post()
                            .uri("/api/reservations/{reservationId}/confirm",
                                    reservationId)
                            .retrieve()
                            .body(InventoryReservationResponse.class);

            if (reservation == null || reservation.status() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Inventory Service returned an invalid confirmation response"
                );
            }

            if (!"CONFIRMED".equalsIgnoreCase(reservation.status())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Inventory reservation was not confirmed. Status: "
                                + reservation.status()
                );
            }

            return reservation;

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to confirm inventory reservation",
                    ex
            );
        }
    }
}
