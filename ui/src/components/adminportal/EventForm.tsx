import React from "react";
import {EventItem, Venue} from "./types.ts";
import {Check} from "lucide-react";

interface EventFormProps {
    eventForm: EventItem;
    setEventForm: (event: EventItem) => void;
    editingEventId: string | null;
    venues: Venue[];
    onSave: () => void;
    onCancel: () => void;
}

export function EventForm({
                              eventForm,
                              setEventForm,
                              editingEventId,
                              venues,
                              onSave,
                              onCancel,
                          }: EventFormProps) {
    return (
        <div className="admin-form">
            <label>
                Event title *
    <input
        value={eventForm.title}
    onChange={(e) =>
    setEventForm({ ...eventForm, title: e.target.value })
}
    placeholder="e.g. Midnight Frequencies"
    />
    </label>
    <label>
    Venue *
    <select
        value={eventForm.venueId}
    onChange={(e) => {
        const venue = venues.find(
            (v) => v.id === e.target.value,
        );

        setEventForm({
            ...eventForm,
            venueId: e.target.value,
            capacity: venue
                ? Math.min(eventForm.capacity, venue.capacity)
                : eventForm.capacity,
        });
    }}
>
    <option value="">Select an active venue</option>
    {venues
        .filter((venue) => venue.active)
        .map((venue) => (
            <option key={venue.id} value={venue.id}>
        {venue.name} — {venue.city}
        </option>
    ))}
    </select>
    </label>
    <div className="admin-form-row">
    <label>
        Date *
    <input
        type="date"
    value={eventForm.date}
    onChange={(e) =>
    setEventForm({ ...eventForm, date: e.target.value })
}
    />
    </label>
    <label>
    Start time *
    <input
        type="time"
    value={eventForm.time}
    onChange={(e) =>
    setEventForm({ ...eventForm, time: e.target.value })
}
    />
    </label>
    </div>
    <div className="admin-form-row">
    <label>
        Category *
    <select
        value={eventForm.category}
    onChange={(e) =>
    setEventForm({
        ...eventForm,
        category: e.target.value,
    })
}
>
    <option>Music</option>
    <option>Sports</option>
    <option>Arts</option>
    <option>Outdoors</option>
    <option>Talks</option>
    <option>Comedy</option>
    </select>
    </label>
    <label>
    Starting price (INR) *
    <input
        type="number"
    min="0"
    value={eventForm.price}
    onChange={(e) =>
    setEventForm({
        ...eventForm,
        price: Number(e.target.value),
    })
}
    />
    </label>
    </div>
    <label>
    Ticket allocation *
    <input
        type="number"
    min="1"
    value={eventForm.capacity}
    onChange={(e) =>
    setEventForm({
        ...eventForm,
        capacity: Number(e.target.value),
    })
}
    />
    <small className="admin-field-help">
        Cannot exceed the selected venue's capacity.
    </small>
    </label>
    <label>
    Description *
    <textarea
        rows={3}
    value={eventForm.description}
    onChange={(e) =>
    setEventForm({
        ...eventForm,
        description: e.target.value,
    })
}
    placeholder="Describe the experience for customers..."
        />
        </label>
        <div className="admin-form-actions">
    <button
        className="admin-secondary-button"
    onClick={onCancel}
        >
        Cancel
        </button>
        <button
    className="admin-primary-button"
    onClick={onSave}
    >
    <Check size={15} /> Save event
    </button>
    </div>
    <p className="admin-form-footnote">
        New events are saved as drafts. Publish them from the Events
    page when they're ready.
    </p>
    </div>
);
}
