import { CalendarDays, Zap } from 'lucide-react'
import { Booking } from '../types'

export function DigitalTicket({ booking }: {booking: Booking}) {
  return (
    <article className="digital-ticket">
      <div className="ticket-visual" style={{backgroundImage:`linear-gradient(0deg,rgba(5,10,20,.88),rgba(5,10,20,.08)),url("${booking.event.image}")`}}>
        <span className="ticket-brand"><Zap size={14} fill="currentColor"/> AFTERGLOW</span><div className="ticket-event-name">{booking.event.title}</div><div className="ticket-date"><CalendarDays size={14}/>{booking.event.date}</div>
      </div>
      <div className="ticket-info">
        <div className="ticket-meta"><span>BOOKING REF</span><strong>{booking.id}</strong></div>
        <div className="ticket-meta"><span>SEAT{booking.seats.length > 1 ? 'S' : ''}</span><strong>{booking.seats.join(', ')}</strong></div>
        <div className="ticket-meta"><span>VENUE</span><strong>{booking.event.venue}</strong></div>
        <div className="fake-qr" aria-label="Decorative demo QR code">
          <div className="qr-pattern">{Array.from({length:81},(_,i)=><i key={i} style={{opacity: ((i*7+i%3)%5) > 1 ? 1 : .18}}/>)}</div>
          <div><strong>READY TO GO</strong><small>Demo ticket · not valid for entry</small></div>
        </div>
      </div>
    </article>
  )
}
