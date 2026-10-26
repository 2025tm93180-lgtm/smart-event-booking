import { Building2, MapPin, Users, Pencil, Trash2 } from 'lucide-react';
import { Venue } from '../types.ts';

interface VenueCardProps {
    venue: Venue;
    index: number;
    onEdit: (venue: Venue) => void;
    onToggle: (venue: Venue) => void;
    onDelete: (venue: Venue) => void;
}

export function VenueCard({ venue, index, onEdit, onToggle, onDelete }: VenueCardProps) {
    return (
        <article className="admin-venue-card">
            <div className={`venue-art venue-art-${index % 3}`}>
                <Building2 size={31} />
                <span>{venue.id}</span>
                <span
                    className={
                        venue.active
                            ? "admin-status status-active"
                            : "admin-status status-inactive"
                    }
                >
                    {venue.active ? "ACTIVE" : "INACTIVE"}
                </span>
            </div>

            <div className="admin-venue-body">
                <h3>{venue.name}</h3>
                <p className="venue-location">
                    <MapPin size={14} /> {venue.address}
                </p>
                <div className="venue-capacity">
                    <Users size={15} />
                    <strong>
                        {venue.capacity.toLocaleString("en-IN")}
                    </strong>
                    <span>maximum capacity</span>
                </div>
                <div className="venue-facilities">
                    {venue.facilities
                        .split(",")
                        .filter(Boolean)
                        .slice(0, 3)
                        .map((facility) => (
                            <span key={facility}>{facility.trim()}</span>
                        ))}
                </div>
                <div className="admin-card-actions">
                    <button
                        className="admin-secondary-button"
                        onClick={() => onEdit(venue)}
                    >
                        <Pencil size={14} /> Edit
                    </button>
                    <button
                        className="admin-secondary-button"
                        onClick={() => onToggle(venue)}
                    >
                        {venue.active ? "Deactivate" : "Activate"}
                    </button>
                    <button
                        className="admin-icon-danger"
                        title="Delete venue"
                        onClick={() => onDelete(venue)}
                    >
                        <Trash2 size={15} />
                    </button>
                </div>
            </div>
        </article>
    );
}
