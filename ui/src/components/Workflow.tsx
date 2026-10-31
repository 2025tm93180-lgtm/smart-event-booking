import { Check, Zap } from 'lucide-react'
import { Booking } from '../types'

export function Workflow({ booking }: {booking: Booking}) {
  const steps = [
    { title: 'Booking accepted', detail: 'Booking Service · outbox saved' },
    { title: 'Seats reserved', detail: 'Event & Inventory · inventory event' },
    { title: 'Payment processed', detail: 'Payment Service · result event' },
    { title: 'Seats confirmed', detail: 'Inventory confirmation acknowledged' },
    { title: 'Ticket issued', detail: 'Ticket Service · booking confirmed' },
  ]
  return (
    <div className="workflow">
      <div className="workflow-heading"><span><Zap size={15}/> ASYNC BOOKING WORKFLOW</span><span className={booking.status === 'CONFIRMED' ? 'status status-confirmed' : 'status status-pending'}>{booking.status}</span></div>
      <div className="workflow-steps">
        {steps.map((s, i) => <div key={s.title} className={`workflow-step ${booking.step > i ? 'done' : ''} ${booking.step === i + 1 && booking.status !== 'CONFIRMED' ? 'current' : ''}`}>
          <span className="workflow-node">{booking.step > i ? <Check size={12}/> : i + 1}</span><div><strong>{s.title}</strong><small>{s.detail}</small></div>
        </div>)}
      </div>
      <p className="workflow-caption"><span className="live-dot"/>{booking.status === 'CONFIRMED' ? 'All mock workflow steps completed.' : 'Demo: advance one step at a time to simulate asynchronous service events.'}</p>
    </div>
  )
}
