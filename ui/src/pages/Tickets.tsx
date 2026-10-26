import React from 'react';
import { Ticket } from 'lucide-react';
import { Booking } from '../types';
import { DigitalTicket } from '../components/DigitalTicket';
import { useNavigate } from 'react-router-dom';

interface TicketsProps {
  bookings: Booking[];
}

export const Tickets: React.FC<TicketsProps> = ({ bookings }) => {
  const navigate = useNavigate();
  const confirmedBookings = bookings.filter(b => b.status === 'CONFIRMED');

  return (
    <section className="page-section">
      <div className="eyebrow muted">THE MOMENT IS YOURS</div>
      <h1 className="page-title">Your <em>tickets.</em></h1>
      <p className="page-subtitle">Everything you need for getting through the door.</p>
      <div className="ticket-grid">
        {confirmedBookings.map(b => <DigitalTicket key={b.id} booking={b}/>)}
        {confirmedBookings.length === 0 && (
          <div className="empty-state">
            <Ticket size={30}/>
            <h3>No tickets just yet</h3>
            <p>Once a booking is confirmed, your tickets will appear here.</p>
            <button className="button button-primary" onClick={() => navigate('/')}>Discover events</button>
          </div>
        )}
      </div>
    </section>
  );
};
