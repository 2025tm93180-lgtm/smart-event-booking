import React, { useEffect, useState } from 'react';
import { Ticket, Zap, ArrowRight } from 'lucide-react';
import { Booking, User } from '../types';
import { BookingCard } from '../components/BookingCard';
import { useNavigate } from 'react-router-dom';
import { useBookingApi } from '../hooks/useBookingApi';

interface BookingsProps {
  bookings: Booking[];
  advanceWorkflow: (id: string, targetStep?: number) => void;
  setActiveBooking: (id: string) => void;
  user: User | null;
}

export const Bookings: React.FC<BookingsProps> = ({ bookings: localBookings, advanceWorkflow, setActiveBooking, user }) => {
  const navigate = useNavigate();
  const { fetchBookingsByUsername } = useBookingApi();
  const [bookings, setBookings] = useState<Booking[]>(localBookings);

  useEffect(() => {
    if (user?.name) {
      const loadBookings = async () => {
        const fetched = await fetchBookingsByUsername(user.name);
        if (fetched && fetched.length > 0) {
          // Merge or replace. Given the requirement to fetch, we should probably show fetched data.
          // Map fetched data if necessary.
          setBookings(fetched);
        }
      };
      loadBookings();
    }
  }, [user, fetchBookingsByUsername]);

  return (
    <section className="page-section">
      <div className="eyebrow muted">YOUR PLANS, IN ONE PLACE</div>
      <h1 className="page-title">My <em>bookings.</em></h1>
      <p className="page-subtitle">Follow your booking from request to confirmation.</p>
      <div className="booking-list">
        {bookings.map(b => (
          <BookingCard 
            key={b.id} 
            booking={b} 
            onOpen={() => {
              setActiveBooking(b.id);
              navigate(`/event/${b.event.id}?checkout=true`);
            }} 
            onAdvance={() => advanceWorkflow(b.id)} 
          />
        ))}
        {bookings.length === 0 && (
          <div className="empty-state">
            <Ticket size={30}/>
            <h3>Your next memory is waiting</h3>
            <button className="button button-primary" onClick={() => navigate('/')}>Explore events</button>
          </div>
        )}
      </div>
      <div className="prototype-note">
        <Zap size={17}/>
        <div>
          <strong>Kafka workflow simulation</strong>
          <p>Pending bookings can be advanced through the mock inventory, payment, confirmation and ticket steps. No broker or backend is connected.</p>
        </div>
      </div>
    </section>
  );
};
