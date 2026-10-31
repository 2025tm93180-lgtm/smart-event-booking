import {ArrowRight, CalendarDays, MapPin} from 'lucide-react'
import {Booking} from '../types'
import {money} from '../utils'

type BookingCardProps = {
    booking: Booking;
    onOpen: () => void;
    onAdvance: () => void;
}

export function BookingCard({booking, onOpen, onAdvance}: BookingCardProps) {
    return (
        <article className="booking-card">
            <div className="booking-thumb"
                 style={{backgroundImage: `linear-gradient(0deg,rgba(5,10,20,.3),rgba(5,10,20,.05)),url("${booking.event.image}")`}}/>
            <div className="booking-main">
                <div className="booking-overline">{booking.event.category} <span>·</span> {booking.createdAt}</div>
                <h3>{booking.event.title}</h3><p><CalendarDays size={14}/>{booking.event.date}</p>
                <p><MapPin size={14}/>{booking.event.venue}, {booking.event.city}</p>
                <div className="booking-seats">
                    {booking.seats.map((seat, i) => <span key={seat} className="booking-seat">{seat}{i < booking.seats.length - 1 && <span className="booking-seat-separator">, </span>}</span>)}
                </div>
                <div className="booking-ref">REF <strong>{booking.id}</strong></div>
            </div>
            <div className="booking-side"><span
                className={`status ${booking.status === 'CONFIRMED' ? 'status-confirmed' : 'status-pending'}`}>{booking.status}</span><strong>{money(booking.amount)}</strong>
                <button className="button button-small" onClick={onOpen}>View progress <ArrowRight size={14}/></button>
                {booking.status === 'PENDING' &&
                    <button className="quiet-button" onClick={onAdvance}>Advance mock step</button>}</div>
        </article>
    )
}
