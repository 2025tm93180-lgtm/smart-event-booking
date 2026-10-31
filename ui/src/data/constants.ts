import { Booking } from '../types';
import { events } from '../data';

export const INITIAL_BOOKINGS: Booking[] = [
  { 
    id: 'AF-2048', 
    event: events[1], 
    seats: ['C3', 'C4'], 
    amount: 1598, 
    status: 'CONFIRMED', 
    payment: 'CONFIRMED', 
    createdAt: 'Oct 02, 2026', 
    step: 5 
  }
];
