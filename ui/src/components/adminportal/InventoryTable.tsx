import { ChevronRight } from 'lucide-react';
import { EventItem, Venue } from './types';
import { Status } from './AdminPortalComponents';

interface InventoryTableProps {
    events: EventItem[];
    venues: Venue[];
    formatCurrency: (amount: number) => string;
    onEdit: (event: EventItem) => void;
}

export function InventoryTable({
    events,
    venues,
    formatCurrency,
    onEdit,
}: InventoryTableProps) {
    return (
        <div className="admin-panel inventory-panel">
            <div className="admin-table-wrap">
                <table className="admin-table">
                    <thead>
                        <tr>
                            <th>EVENT</th>
                            <th>VENUE CAPACITY</th>
                            <th>EVENT ALLOCATION</th>
                            <th>BASE PRICE</th>
                            <th>STATUS</th>
                            <th>CONFIGURE</th>
                        </tr>
                    </thead>
                    <tbody>
                        {events.map((event) => {
                            const venue = venues.find(
                                (v) => v.id === event.venueId,
                            );

                            return (
                                <tr key={event.id}>
                                    <td>
                                        <strong>{event.title}</strong>
                                        <small>{event.id}</small>
                                    </td>
                                    <td>
                                        {(venue?.capacity ?? 0).toLocaleString("en-IN")}
                                    </td>
                                    <td>
                                        {event.capacity.toLocaleString("en-IN")}
                                        <div className="admin-capacity-bar">
                                            <span
                                                style={{
                                                    width: `${
                                                        venue
                                                            ? Math.min(
                                                                100,
                                                                (event.capacity /
                                                                    venue.capacity) *
                                                                100,
                                                            )
                                                            : 0
                                                    }%`,
                                                }}
                                            />
                                        </div>
                                    </td>
                                    <td>{formatCurrency(event.price)}</td>
                                    <td><Status status={event.status} /></td>
                                    <td>
                                        <button
                                            className="admin-secondary-button"
                                            onClick={() => onEdit(event)}
                                        >
                                            Configure <ChevronRight size={14} />
                                        </button>
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </div>
    );
}
