export type EventItem = {
  id: number;
  title: string;
  category: string;
  date: string;
  day: string;
  startTime: string;
  duration: string;
  venue: string;
  city: string;
  price: number;
  image: string;
  tag: string;
  description: string;
  gradient: string;
  seats: number,
  capacity: number;
  status: 'DRAFT' | 'PUBLISHED' | 'CANCELLED';
}

export type PaymentStatusEnum = 'PENDING' | 'CONFIRMED' | 'FAILED' | 'CANCELLED';

export type Booking = {
  id: string;
  event: EventItem;
  seats: string[];
  amount: number;
  status: 'PENDING' | 'CONFIRMED' | 'FAILED';
  payment: 'PENDING' | 'CONFIRMED' | 'DECLINED';
  createdAt: string;
  step: number
}

export type User = {
  id: string;
  name: string;
  email: string;
  dateOfBirth?: string;
  avatar?: string;
  phone?: string;
  memberSince: string;
}

export type AuthState = {
  user: User | null;
  isAuthenticated: boolean;
  favorites: number[];
}
