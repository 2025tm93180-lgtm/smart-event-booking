import React, { useMemo, useState, useEffect } from 'react';
import { Search, ArrowRight } from 'lucide-react';
import { EventItem } from '../types';
import { events as dummyEvents } from '../data';
import { EventCard } from '../components/EventCard';
import { Hero } from '../components/Hero';
import { useNavigate } from 'react-router-dom';
import { useEventApi } from '../hooks/useEventApi';
import { EventItem as AdminEventItem } from '../components/adminportal/types';

interface DiscoverProps {
  favorites: number[];
  onToggleFavorite: (id: number) => void;
}

export const Discover: React.FC<DiscoverProps> = ({ favorites, onToggleFavorite }) => {
  const [category, setCategory] = useState('All events');
  const [events, setEvents] = useState<EventItem[]>(dummyEvents);
  const { fetchEvents } = useEventApi();

  useEffect(() => {
    const loadEvents = async () => {
      const fetched = await fetchEvents();
      if (fetched.length > 0) {
        const mapped: EventItem[] = fetched.map((e: AdminEventItem) => ({
          id: Number(e.id.replace(/\D/g, '')) || Math.floor(Math.random() * 1000),
          title: e.title,
          category: e.category,
          date: e.date + ' · ' + e.time,
          day: e.date,
          venue: e.venueId,
          city: 'Unknown',
          price: e.price,
          image: 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?auto=format&fit=crop&w=1400&q=85',
          tag: e.status === 'PUBLISHED' ? 'LIVE' : 'DRAFT',
          description: e.description,
          gradient: 'linear-gradient(135deg,#41216c,#e05b89)',
          seats: e.capacity,
          capacity: e.capacity,
          status: e.status,
          startTime: e.time,
          duration: '2h'
        }));
        setEvents(mapped);
      }
    };
    loadEvents();
  }, [fetchEvents]);
  const [search, setSearch] = useState('');
  const navigate = useNavigate();

  const filteredEvents = useMemo(() => events.filter(e =>
    (category === 'All events' || e.category === category) &&
    `${e.title} ${e.venue} ${e.city} ${e.category}`.toLowerCase().includes(search.toLowerCase())
  ), [category, search]);

  const openEvent = (event: EventItem) => {
    navigate(`/event/${event.id}`);
  };

  return (
    <>
      <Hero />

      <section className="stats-strip">
        <div><span className="stat-icon"><Search size={18}/></span><span><strong>250+</strong><small>Curated experiences</small></span></div>
        <div><span><strong>Secure by design</strong><small>Protected booking flow</small></span></div>
        <div><span><strong>Made for moments</strong><small>Discover something new</small></span></div>
        <div className="stats-note">GOOD THINGS HAPPEN LIVE <ArrowRight size={15}/></div>
      </section>

      <section className="events-section" id="events">
        <div className="section-heading">
          <div><div className="eyebrow muted">THE LINEUP</div><h2>Find your <em>next thing.</em></h2><p>Handpicked experiences worth stepping out for.</p></div>
          <button className="text-button" onClick={() => { setCategory('All events'); setSearch(''); }}>View all events <ArrowRight size={16}/></button>
        </div>
        <div className="discovery-tools">
          <div className="search-box"><Search size={18}/><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search events, artists, venues..." aria-label="Search events"/></div>
          <div className="category-list">
            {['All events', 'Music', 'Sports', 'Arts', 'Outdoors', 'Talks'].map(c =>
              <button key={c} className={category === c ? 'category-pill selected' : 'category-pill'} onClick={() => setCategory(c)}>{c}</button>
            )}
          </div>
        </div>
        <div className="event-grid">
          {filteredEvents.map((event, i) => (
            <EventCard 
              key={event.id} 
              event={event} 
              index={i} 
              onSelect={openEvent}
              isFavorite={favorites.includes(event.id)}
              onToggleFavorite={() => onToggleFavorite(event.id)}
            />
          ))}
        </div>
        {filteredEvents.length === 0 && <div className="empty-state"><Search size={28}/><h3>No experiences found</h3><p>Try another search or category.</p><button className="button button-secondary" onClick={() => {setSearch(''); setCategory('All events')}}>Clear filters</button></div>}
      </section>
    </>
  );
};
