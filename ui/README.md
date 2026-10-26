# Afterglow — Smart Event Ticket Booking UI Prototype

A working React + TypeScript customer portal prototype for the Smart Event Ticket Booking System assignment.

## Included screens
- Event discovery with search and category filters
- Event details and interactive seat selection
- Demo checkout and booking reference
- Booking status page with a manually advanced Kafka/Saga workflow simulation
- Digital ticket view for confirmed bookings

## Important scope note
This is a **frontend prototype using local mock data**. It is not connected to Spring Boot, MySQL, Kafka, JWT authentication, or a real payment provider. The workflow step button simulates asynchronous service progress in local React state. The displayed ticket QR pattern is decorative and not valid for venue entry.

## Requirements
- Node.js 18+ (Node.js 20+ recommended)
- npm

## Run locally
```bash
npm install
npm run dev
```
Open the local URL printed by Vite.

## Production build check
```bash
npm run build
npm run preview
```

## Suggested backend integration points
| UI action | Future backend integration |
|---|---|
| Load events | `GET /api/v1/events` on Event & Inventory Service |
| Load categories/seats | `GET /api/v1/events/{eventId}/categories`, `GET /api/v1/events/{eventId}/seats` |
| Register / login | `POST /api/v1/auth/register`, `POST /api/v1/auth/login` on Booking Service |
| Submit booking | `POST /api/v1/bookings` with JWT and `Idempotency-Key` |
| Poll booking status | `GET /api/v1/bookings/{bookingId}/status` |
| List bookings | `GET /api/v1/bookings` |
| Load tickets | `GET /api/v1/tickets/by-booking/{bookingId}` (with booking ownership authorization) |

Kafka should remain internal to the backend. The frontend should not connect directly to Kafka. Booking progress can be updated via polling, or later via a backend-provided SSE/WebSocket endpoint if the team implements one.
