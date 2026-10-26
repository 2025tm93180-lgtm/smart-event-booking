import { Pencil, ArrowDownRight, ArrowUpRight, Trash2 } from 'lucide-react';
import { EventItem } from './types';
import { Status } from './AdminPortalComponents';

interface EventTableProps {
    events: EventItem[];
    getVenueName: (id: string) => string;
    formatCurrency: (amount: number) => string;
    onEdit: (event: EventItem) => void;
    onToggleStatus: (event: EventItem) => void;
    onDelete: (event: EventItem) => void;
}

export function EventTable({
    events,
    getVenueName,
    formatCurrency,
    onEdit,
    onToggleStatus,
    onDelete,
}: EventTableProps) {
    return (
        <div className="admin-panel">
            <div className="admin-table-wrap">
                <table className="admin-table events-table">
                    <thead>
                        <tr>
                            <th>EVENT</th>
                            <th>VENUE</th>
                            <th>DATE & TIME</th>
                            <th>PRICE</th>
                            <th>STATUS</th>
                            <th>ACTIONS</th>
                        </tr>
                    </thead>
                    <tbody>
                        {events.map((event) => (
                            <tr key={event.id}>
                                <td>
                                    <strong>{event.title}</strong>
                                    <small>{event.id} · {event.category}</small>
                                </td>
                                <td>{getVenueName(event.venueId)}</td>
                                <td>
                                    {event.date}
                                    <small>{event.time}</small>
                                </td>
                                <td>{formatCurrency(event.price)}</td>
                                <td><Status status={event.status} /></td>
                                <td>
                                    <div className="admin-row-actions">
                                        <button
                                            className="admin-icon-button"
                                            title="Edit event"
                                            onClick={() => onEdit(event)}
                                        >
                                            <Pencil size={14} />
                                        </button>
                                        {event.status !== "Cancelled" && (
                                            <button
                                                className="admin-icon-button"
                                                title={
                                                    event.status === "Published"
                                                        ? "Move to draft"
                                                        : "Publish event"
                                                }
                                                onClick={() => onToggleStatus(event)}
                                            >
                                                {event.status === "Published" ? (
                                                    <ArrowDownRight size={15} />
                                                ) : (
                                                    <ArrowUpRight size={15} />
                                                )}
                                            </button>
                                        )}
                                        <button
                                            className="admin-icon-button danger"
                                            title="Delete event"
                                            onClick={() => onDelete(event)}
                                        >
                                            <Trash2 size={14} />
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                {events.length === 0 && (
                    <p className="admin-no-results">No matching events.</p>
                )}
            </div>
        </div>
    );
}
