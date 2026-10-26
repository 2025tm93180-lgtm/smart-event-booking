import { useCallback } from 'react';

const BOOKING_SERVICE_BASE_URL = 'http://localhost:9091';

export type CreateBookingPayload = {
  bookingId?: string;
  seatNumbers?: string[];
  venue?: string;
  username?: string;
  mail?: string;
  phoneNumber?: string;
  eventId?: number;
  eventTitle?: string;
  eventDate?: string;
  eventStartTime?: string;
  eventDuration?: string;
  city?: string;
  amount: number;
  currency: string;
  status: string;
  userId?: string | null;
};

export type CreateBookingResult = {
  ok: boolean;
  bookingId?: string;
  status?: string;
  amount?: number;
  currency?: string;
};

export type BookedSeatsResponse = {
  eventId: number;
  venue: string;
  seatsOccupied: string[];
};

export const useBookingApi = () => {
  const createBooking = useCallback(async (
    payload: CreateBookingPayload
  ): Promise<CreateBookingResult> => {
    try {
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      };

      const response = await fetch(`${BOOKING_SERVICE_BASE_URL}/api/v1/bookings`, {
        method: 'POST',
        headers,
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        console.warn('[useBookingApi] Booking service rejected request:', response.status);
        return { ok: false };
      }

      const data = await response.json();
      const bookingId = data.bookingId || data.receiptId || payload.bookingId;

      if (bookingId) {
        sessionStorage.setItem('bookingId', bookingId);
      }

      return {
        ok: true,
        bookingId,
        status: data.status,
        amount: data.amount,
        currency: data.currency
      };
    } catch (error) {
      console.error('[useBookingApi] Error in createBooking:', error);
      return { ok: false };
    }
  }, []);

  const fetchBookedSeats = useCallback(async (
    eventId: number,
    venue: string,
  ): Promise<BookedSeatsResponse | null> => {
    const query = new URLSearchParams({ venue });

    try {
      const response = await fetch(
        `${BOOKING_SERVICE_BASE_URL}/api/v1/bookings/events/${eventId}/booked-seats?${query.toString()}`,
      );

      if (!response.ok) {
        console.warn('[useBookingApi] Booking service rejected booked seats request:', response.status);
        return null;
      }

      const data = await response.json();
      const seatsOccupied = Array.isArray(data.seatsOccupied)
        ? data.seatsOccupied
        : Array.isArray(data.seatsOccuiped)
          ? data.seatsOccuiped
          : Array.isArray(data.seatNumbers)
            ? data.seatNumbers
            : [];

      return {
        eventId: Number(data.eventId ?? eventId),
        venue: String(data.venue ?? venue),
        seatsOccupied,
      };
    } catch (error) {
      console.warn('[useBookingApi] Booking service unavailable. Using local mock occupied seats.', error);
      return null;
    }
  }, []);

  const fetchBookingsByUsername = useCallback(async (username: string): Promise<any[]> => {
    try {
      const response = await fetch(`${BOOKING_SERVICE_BASE_URL}/api/v1/bookings/user/${username}`);
      if (!response.ok) {
        throw new Error(`Failed to fetch bookings for user ${username}: ${response.status}`);
      }
      return await response.json();
    } catch (error) {
      console.error('[useBookingApi] Error fetching bookings:', error);
      return [];
    }
  }, []);

  const updateBookingStatus = useCallback(async (
    bookingId: string,
    status: string
  ): Promise<boolean> => {
    try {
      const response = await fetch(`${BOOKING_SERVICE_BASE_URL}/api/v1/bookings/${bookingId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ status }),
      });

      return response.ok;
    } catch (error) {
      console.error('[useBookingApi] Error in updateBookingStatus:', error);
      return false;
    }
  }, []);

  return { fetchBookedSeats, fetchBookingsByUsername, createBooking, updateBookingStatus };
};
