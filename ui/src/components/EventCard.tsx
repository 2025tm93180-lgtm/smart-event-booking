import {ArrowRight, ArrowUpRight, MapPin, Heart} from 'lucide-react'
import {EventItem} from '../types'
import {money} from '../utils'

type EventCardProps = {
    event: EventItem;
    index: number;
    onSelect: (e: EventItem) => void;
    isFavorite?: boolean;
    onToggleFavorite?: () => void;
}

export function EventCard({event, index, onSelect, isFavorite, onToggleFavorite}: EventCardProps) {
    return (
        <article className={`event-card card-${index % 3}`}>
            <div className="event-image-container">
                <button className="event-image-button" onClick={() => onSelect(event)} aria-label={`View ${event.title}`}>
                    <div className="event-image"
                         style={{backgroundImage: `linear-gradient(0deg,rgba(5,10,20,.86),rgba(5,10,20,.04) 68%),url("${event.image}")`}}>
                        <span className="event-tag">{event.tag}</span><span className="event-date-badge">{event.day}</span>
                        <span className="image-arrow"><ArrowUpRight size={18}/></span>
                        {onToggleFavorite && (
                            <button
                                className={`favorite-button ${isFavorite ? 'active' : ''}`}
                                onClick={(e) => {
                                    e.stopPropagation();
                                    onToggleFavorite();
                                }}
                                aria-label={isFavorite ? "Remove from favorites" : "Add to favorites"}
                            >
                                <Heart size={18} fill={isFavorite ? "currentColor" : "none"} />
                            </button>
                        )}
                        <div className="event-image-copy"><span className="event-category">{event.category}</span><h3
                            style={{color: "white"}}>{event.title}</h3><span className="event-location">
                            <MapPin size={13}/>{event.venue} · {event.city}</span></div>
                    </div>
                </button>
            </div>
            <div className="event-card-bottom">
                <div><span className="price-label">TICKETS FROM</span><strong>{money(event.price)}<small> /
                    person</small></strong></div>
                <button className="button button-small" onClick={() => onSelect(event)}>Book now <ArrowRight size={14}/>
                </button>
            </div>
        </article>
    )
}
