import React, {useEffect, useState} from 'react';
import {useParams, useNavigate, useSearchParams} from 'react-router-dom';
import {CalendarDays, MapPin, ShieldCheck, ArrowRight, X, Check, Clock3, Ticket, Zap} from 'lucide-react';
import {EventItem, Booking, User} from '../types';
import {events, seatRows, seatNumbers, occupiedSeats} from '../data';
import {money} from '../utils';
import {Seat} from '../components/Seat';
import {Workflow} from '../components/Workflow';
import {Payment} from '../components/Payment';
import {useBookingApi} from '../hooks/useBookingApi';

import {useEventApi} from '../hooks/useEventApi';

interface EventDetailsProps {
    bookings: Booking[];
    startBooking: (event: EventItem, seats: string[]) => void;
    advanceWorkflow: (id: string, targetStep?: number) => void;
    updateBookingId: (oldId: string, newId: string) => void;
    activeBookingId: string | null;
    user: User | null;
}

export const EventDetails: React.FC<EventDetailsProps> = ({
                                                              bookings,
                                                              startBooking,
                                                              advanceWorkflow,
                                                              updateBookingId,
                                                              activeBookingId,
                                                              user
                                                          }) => {
    const {id} = useParams<{ id: string }>();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const showCheckout = searchParams.get('checkout') === 'true';
    const {fetchBookedSeats} = useBookingApi();
    const {fetchEvents} = useEventApi();

    const [event, setEvent] = useState<EventItem | undefined>(events.find(e => e.id === Number(id)));

    useEffect(() => {
        const loadEvent = async () => {
            const fetched = await fetchEvents();
            const found = fetched.find(e => e.id === id);
            if (found) {
                // Map to EventItem
                const mapped: EventItem = {
                    id: Number(found.id.replace(/\D/g, '')) || Math.floor(Math.random() * 1000),
                    title: found.title,
                    category: found.category,
                    date: found.date + ' · ' + found.time,
                    day: found.date,
                    venue: found.venueId,
                    city: 'Unknown',
                    price: found.price,
                    image: 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?auto=format&fit=crop&w=1400&q=85',
                    tag: found.status === 'Published' ? 'LIVE' : 'DRAFT',
                    description: found.description,
                    gradient: 'linear-gradient(135deg,#41216c,#e05b89)',
                    seats: found.capacity,
                    capacity: found.capacity,
                    status: found.status,
                    startTime: found.time,
                    duration: '2h'
                };
                setEvent(mapped);
            }
        };
        loadEvent();
    }, [id, fetchEvents]);

    const [selectedSeats, setSelectedSeats] = useState<string[]>([]);
    const [bookedSeats, setBookedSeats] = useState<Set<string>>(() => new Set(occupiedSeats));

    useEffect(() => {
        // Scroll to top on mount
        window.scrollTo(0, 0);
    }, []);

    useEffect(() => {
        if (!event) return;

        let ignore = false;
        setBookedSeats(new Set(occupiedSeats));

        fetchBookedSeats(event.id, event.venue).then((result) => {
            if (ignore || !result) return;

            const nextBookedSeats = new Set(result.seatsOccupied);
            setBookedSeats(nextBookedSeats);
            setSelectedSeats((current) => current.filter((seat) => !nextBookedSeats.has(seat)));
        });

        return () => {
            ignore = true;
        };
    }, [event, fetchBookedSeats]);

    if (!event) {
        return (
            <div className="empty-state">
                <h3>Event not found</h3>
                <button className="button button-primary" onClick={() => navigate('/')}>Back to Discover</button>
            </div>
        );
    }

    const toggleSeat = (seat: string) => {
        if (bookedSeats.has(seat)) return;
        setSelectedSeats(prev => prev.includes(seat) ? prev.filter(s => s !== seat) : [...prev, seat]);
    };

    const handleStartBooking = () => {
        startBooking(event, selectedSeats);
        navigate(`/event/${event.id}?checkout=true`);
    };

    const currentBooking = bookings.find(b => b.id === activeBookingId) ?? null;

    return (
        <div className="event-details-container">
            <div className="event-details-card">
                {!showCheckout ? (
                    <>
                        <div className="event-cover"
                             style={{backgroundImage: `linear-gradient(0deg,rgba(7,12,22,.95),rgba(7,12,22,.05)),url("${event.image}")`}}>
                            <span className="event-tag">{event.tag}</span>
                            <div>
                                <div className="eyebrow">{event.category.toUpperCase()} · {event.day}</div>
                                <h2>{event.title}</h2></div>
                        </div>
                        <div className="event-details-body">
                            <div className="event-meta"><span><CalendarDays size={16}/>{event.date}</span><span><MapPin
                                size={16}/>{event.venue}, {event.city}</span></div>
                            <p className="event-description">{event.description}</p>
                            <div className="seat-header">
                                <div><h3>Choose your seats</h3><p>Select available seats for your crew.</p></div>
                                <span className="seat-count">{event.seats} seats left</span></div>
                            <div className="seat-map">
                                <div className="stage"><span>STAGE</span></div>
                                <div className="seat-grid"
                                     style={{'--seat-count': seatNumbers.length} as React.CSSProperties}>
                                    {seatRows.map(row => (
                                        <div className="seat-row" key={row}>
                                            <span className="row-label">{row}</span>
                                            {seatNumbers.map(n => (
                                                <Seat
                                                    key={`${row}${n}`}
                                                    row={row}
                                                    number={n}
                                                    isOccupied={bookedSeats.has(`${row}${n}`)}
                                                    isSelected={selectedSeats.includes(`${row}${n}`)}
                                                    onToggle={toggleSeat}
                                                />
                                            ))}
                                        </div>
                                    ))}
                                </div>
                                <div className="seat-legend"><span><i
                                    className="legend-seat available"/>Available</span><span><i
                                    className="legend-seat chosen-legend"/>Selected</span><span><i
                                    className="legend-seat unavailable"/>Unavailable</span></div>
                            </div>
                            <div className="checkout-bar">
                                <div>
                                    <small>{selectedSeats.length} seat{selectedSeats.length === 1 ? '' : 's'} selected</small><strong>{money(selectedSeats.length * event.price)}</strong>
                                </div>
                                <button disabled={!selectedSeats.length} className="button button-primary"
                                        onClick={handleStartBooking}>Continue to checkout <ArrowRight size={16}/>
                                </button>
                            </div>
                            <p className="fine-print"><ShieldCheck size={13}/> Demo checkout · no real payment will be
                                collected</p>
                        </div>
                    </>
                ) : (
                    <div className="checkout-view">
                        <div className="checkout-top"><span className="brand-mark"><Zap size={19} fill="currentColor"/></span><span
                            className="eyebrow">SECURE CHECKOUT · DEMO</span></div>
                        {currentBooking ? (
                            <>
                                {currentBooking.payment === 'PENDING' && currentBooking.status !== 'CONFIRMED' ? (
                                        <Payment
                                                booking={currentBooking}
                                                user={user}
                                                onSuccess={(paymentId, newBookingId) => {
                                                    console.log('Payment Success:', paymentId);
                                                    const oldId = currentBooking.id;
                                                    const finalBookingId = newBookingId || oldId;

                                                    if (newBookingId && newBookingId !== oldId) {
                                                        updateBookingId(oldId, newBookingId);
                                                    }
                                                    
                                                    // Immediately fetch authoritative status after success if possible
                                                    // or just advance workflow steps as a reaction to UI success.
                                                    // In production, we'd wait for a socket or poll.
                                                    advanceWorkflow(finalBookingId, 3);
                                                }}
                                                onCancel={() => {
                                                    console.log('Payment Cancelled');
                                                    // Optionally reset or show message
                                                }}
                                                onError={(msg) => {
                                                    console.error('Payment Error:', msg);
                                                }}
                                            />
                                ) : (
                                    <>
                                        <div className="checkout-success-icon">{currentBooking.status === 'CONFIRMED' ?
                                            <Check size={28}/> : <Clock3 size={28}/>}</div>
                                        <h2>{currentBooking.status === 'CONFIRMED' ? 'You’re on the list.' : 'Your booking is in motion.'}</h2>
                                        <p className="checkout-intro">{currentBooking.status === 'CONFIRMED' ? 'Your experience is confirmed. Your digital ticket is ready.' : 'Your request is accepted. Watch the services coordinate your booking.'}</p>
                                        <div className="order-summary">
                                            <div><span>Booking reference</span><strong>{currentBooking.id}</strong>
                                            </div>
                                            <div><span>Experience</span><strong>{currentBooking.event.title}</strong>
                                            </div>
                                            <div><span>Seats</span><strong>{currentBooking.seats.join(', ')}</strong>
                                            </div>
                                            <div><span>Total</span><strong>{money(currentBooking.amount)}</strong></div>
                                        </div>
                                        <Workflow booking={currentBooking}/>
                                        <div className="checkout-actions">
                                            {currentBooking.status !== 'CONFIRMED' ? (
                                                <button className="button button-primary full-button"
                                                        onClick={() => advanceWorkflow(currentBooking.id)}>Simulate next
                                                    Kafka step <ArrowRight size={16}/></button>
                                            ) : (
                                                <button className="button button-primary full-button" onClick={() => {
                                                    navigate('/tickets')
                                                }}>View my ticket <Ticket size={16}/></button>
                                            )}
                                            <button className="text-button centered" onClick={() => {
                                                navigate('/bookings')
                                            }}>Go to my bookings <ArrowRight size={15}/></button>
                                        </div>
                                    </>
                                )}
                                <p className="fine-print centered"><Zap size={13}/> Workflow is simulated locally; Kafka
                                    is not connected.</p>
                            </>
                        ) : <div className="empty-state"><p>Booking not found.</p></div>}
                    </div>
                )}
            </div>
        </div>
    );
};
