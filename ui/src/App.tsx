import { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Check } from 'lucide-react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { EventItem, Booking, User } from './types'
import { events } from './data'
import { INITIAL_BOOKINGS } from './data/constants'
import { Layout } from './components/Layout'
import { Discover } from './pages/Discover'
import { Bookings } from './pages/Bookings'
import { Tickets } from './pages/Tickets'
import { EventDetails } from './pages/EventDetails'
import { Signup } from './pages/Signup'
import { Login } from './pages/Login'
import { Profile } from './pages/Profile'
import { Favorites } from './pages/Favorites'
import AdminPortal from "./AdminProtal.tsx";
import { RootState } from './store'
import { setUser, logout, toggleFavorite as toggleFavoriteAction } from './store/userSlice'

export default function App() {
  const dispatch = useDispatch();
  const authState = useSelector((state: RootState) => state.user);
  
  const [bookings, setBookings] = useState<Booking[]>(INITIAL_BOOKINGS)
  const [activeBooking, setActiveBooking] = useState<string | null>(null)
  const [toast, setToast] = useState('')

  const notify = (message: string) => {
    setToast(message)
    window.setTimeout(() => setToast(''), 2800)
  }

  const startBooking = (event: EventItem, selectedSeats: string[]) => {
    const amount = selectedSeats.length * event.price
    const booking: Booking = {
      id: '', event, seats: [...selectedSeats], amount,
      status: 'PENDING', payment: 'PENDING', createdAt: 'Just now', step: 1
    }
    // Check if there is already a booking in progress to prevent duplicates in local state if needed
    // But typically we allow starting a new one. 
    // Idempotency is mainly handled in Payment.tsx via sessionStorage.bookingId
    setBookings(prev => [booking, ...prev])
    setActiveBooking('')
    notify('Booking initiated · Choose payment method')
  }

  const advanceWorkflow = (id: string, targetStep?: number) => {
    setBookings(prev => prev.map(b => {
      if (b.id !== id || b.status !== 'PENDING') return b
      const step = targetStep ?? Math.min(b.step + 1, 5)
      return {
        ...b, step,
        payment: step >= 3 ? 'CONFIRMED' : 'PENDING',
        status: step >= 5 ? 'CONFIRMED' : 'PENDING'
      }
    }))
    const current = bookings.find(b => b.id === id)
    if (current && (targetStep ? targetStep >= 5 : current.step >= 4)) notify('Booking confirmed · tickets are ready')
  }

  const updateBookingId = (oldId: string, newId: string) => {
    setBookings(prev => prev.map(b => b.id === oldId ? { ...b, id: newId } : b));
    if (activeBooking === oldId) setActiveBooking(newId);
  }

  const handleLogin = (user: User) => {
    dispatch(setUser(user));
    notify(`Welcome back, ${user.name.split(' ')[0]}!`);
  };

  const handleSignup = (user: User) => {
    dispatch(setUser(user));
    notify(`Welcome, ${user.name.split(' ')[0]}!`);
  };

  const handleLogout = () => {
    dispatch(logout());
  };

  const toggleFavorite = (eventId: number) => {
    const isAdding = !authState.favorites.includes(eventId);
    dispatch(toggleFavoriteAction(eventId));
    notify(isAdding ? 'Added to favorites' : 'Removed from favorites');
  };

  return (
    <BrowserRouter>
      <Layout 
        bookingsCount={bookings.length} 
        favoritesCount={authState.favorites.length}
        notify={notify}
        user={authState.user}
        isAuthenticated={authState.isAuthenticated}
        onLogout={handleLogout}
      >
        <Routes>
          <Route path="/" element={<Discover favorites={authState.favorites} onToggleFavorite={toggleFavorite} />} />
          <Route path="/favorites" element={<Favorites favorites={authState.favorites} onToggleFavorite={toggleFavorite} />} />
          <Route path="/admin-portal" element={<AdminPortal/>} />
          <Route path="/bookings" element={<Bookings bookings={bookings} advanceWorkflow={advanceWorkflow} setActiveBooking={setActiveBooking} user={authState.user} />} />
          <Route path="/tickets" element={<Tickets bookings={bookings} />} />
          <Route path="/login" element={<Login onLogin={handleLogin} />} />
          <Route path="/signup" element={<Signup onSignup={handleSignup} />} />
          <Route path="/profile" element={<Profile user={authState.user} isAuthenticated={authState.isAuthenticated} onLogout={handleLogout} />} />
          <Route path="/event/:id" element={<EventDetails bookings={bookings} startBooking={startBooking} advanceWorkflow={advanceWorkflow} activeBookingId={activeBooking} user={authState.user} updateBookingId={updateBookingId} />} />
        </Routes>
      </Layout>
      {toast && <div className="toast"><Check size={16}/>{toast}</div>}
    </BrowserRouter>
  )
}
