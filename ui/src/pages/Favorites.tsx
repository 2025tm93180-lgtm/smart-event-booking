import React from 'react';
import { Heart, Search, ArrowRight } from 'lucide-react';
import { events } from '../data';
import { EventCard } from '../components/EventCard';
import { useNavigate } from 'react-router-dom';
import { EventItem } from '../types';

interface FavoritesProps {
  favorites: number[];
  onToggleFavorite: (id: number) => void;
}

export const Favorites: React.FC<FavoritesProps> = ({ favorites, onToggleFavorite }) => {
  const navigate = useNavigate();
  const favoriteEvents = events.filter(e => favorites.includes(e.id));

  const openEvent = (event: EventItem) => {
    navigate(`/event/${event.id}`);
  };

  return (
    <div className="favorites-page container">
      <div className="page-header">
        <div className="eyebrow muted">YOUR COLLECTION</div>
        <h1>Saved <em>Experiences</em></h1>
        <p>Events you're interested in attending.</p>
      </div>

      {favoriteEvents.length > 0 ? (
        <div className="event-grid">
          {favoriteEvents.map((event, i) => (
            <EventCard 
              key={event.id} 
              event={event} 
              index={i} 
              onSelect={openEvent}
              isFavorite={true}
              onToggleFavorite={() => onToggleFavorite(event.id)}
            />
          ))}
        </div>
      ) : (
        <div className="empty-state">
          <Heart size={48} className="muted" />
          <h3>Your list is empty</h3>
          <p>Tap the heart icon on any event to save it here for later.</p>
          <button className="button" onClick={() => navigate('/')}>
            Explore Events <ArrowRight size={18} />
          </button>
        </div>
      )}
    </div>
  );
};
