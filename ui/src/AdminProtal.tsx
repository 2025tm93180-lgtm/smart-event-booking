import { useMemo, useState, useEffect } from "react";
import {
    Activity,
    Armchair,
    ArrowUpRight,
    Building2,
    CalendarDays,
    ChevronRight,
    Clock3,
    LayoutDashboard,
    Plus,
    Ticket,
    Users,
    CirclePlus,
    Check,
} from "lucide-react";
import "./admin.css";

import { Venue, EventItem, View, EventStatus } from "./components/adminportal/types";
import { initialVenues, initialEvents, emptyVenue, emptyEvent } from "./components/adminportal/data";
import { currency } from "./components/adminportal/utils";
import { Metric, Status, SearchBar } from "./components/adminportal/AdminPortalComponents";
import { VenueCard } from "./components/adminportal/AdminForms/VenueCard.tsx";
import { EventTable } from "./components/adminportal/EventTable";
import { InventoryTable } from "./components/adminportal/InventoryTable";
import { VenueForm } from "./components/adminportal/AdminForms/VenuForm.tsx";
import { useEventApi } from "./hooks/useEventApi";
import { useVenueApi } from "./hooks/useVenueApi";
import {Modal} from "./components/adminportal/AdminForms/Modal.tsx";
import {EventForm} from "./components/adminportal/EventForm.tsx";

export default function AdminPortal() {
    const [view, setView] = useState<View>("Dashboard");
    const [venues, setVenues] = useState<Venue[]>(initialVenues);
    const [events, setEvents] = useState<EventItem[]>(initialEvents);
    const [query, setQuery] = useState("");
    const [venueForm, setVenueForm] = useState<Venue | null>(null);
    const [eventForm, setEventForm] = useState<EventItem | null>(null);
    const [editingVenueId, setEditingVenueId] = useState<string | null>(null);
    const [editingEventId, setEditingEventId] = useState<string | null>(null);
    const [toast, setToast] = useState("");

    const { fetchEvents, saveEvent: apiSaveEvent, updateEvent: apiUpdateEvent, deleteEvent: apiDeleteEvent } = useEventApi();
    const { fetchVenues, saveVenue: apiSaveVenue, updateVenue: apiUpdateVenue, deleteVenue: apiDeleteVenue } = useVenueApi();

    useEffect(() => {
        const loadData = async () => {
            const [fetchedVenues, fetchedEvents] = await Promise.all([
                fetchVenues(),
                fetchEvents(),
            ]);
            if (fetchedVenues.length > 0) setVenues(fetchedVenues);
            if (fetchedEvents.length > 0) setEvents(fetchedEvents);
        };
        loadData();
    }, [fetchVenues, fetchEvents]);

    const publishedEvents = events.filter(
        (event) => event.status === "PUBLISHED",
    ).length;

    const draftEvents = events.filter(
        (event) => event.status === "DRAFT",
    ).length;

    const activeVenues = venues.filter((venue) => venue.active).length;

    const filteredVenues = useMemo(
        () =>
            venues.filter((venue) =>
                `${venue.name} ${venue.city} ${venue.address}`
                    .toLowerCase()
                    .includes(query.toLowerCase()),
            ),
        [venues, query],
    );

    const filteredEvents = useMemo(
        () =>
            events.filter((event) => {
                const venueName =
                    venues.find((venue) => venue.id === event.venueId)?.name ?? "";

                return `${event.title} ${event.category} ${venueName} ${event.status}`
                    .toLowerCase()
                    .includes(query.toLowerCase());
            }),
        [events, venues, query],
    );

    function notify(message: string) {
        setToast(message);
        window.setTimeout(() => setToast(""), 2500);
    }

    function venueName(id: string) {
        return venues.find((venue) => venue.id === id)?.name ?? "Unknown venue";
    }

    async function saveVenue() {
        if (!venueForm) return;

        if (
            !venueForm.name.trim() ||
            !venueForm.city.trim() ||
            !venueForm.address.trim() ||
            venueForm.capacity < 1
        ) {
            notify("Complete the required venue fields.");
            return;
        }

        if (editingVenueId) {
            const updated = await apiUpdateVenue(editingVenueId, venueForm);
            if (updated) {
                setVenues((current) =>
                    current.map((venue) =>
                        venue.id === editingVenueId ? updated : venue,
                    ),
                );
                notify("Venue updated.");
            }
        } else {
            const saved = await apiSaveVenue(venueForm);
            if (saved) {
                setVenues((current) => [...current, saved]);
                notify("Venue created.");
            }
        }

        setVenueForm(null);
        setEditingVenueId(null);
    }

    async function saveEvent() {
        if (!eventForm) return;

        if (
            !eventForm.title.trim() ||
            !eventForm.venueId ||
            !eventForm.date ||
            !eventForm.time ||
            !eventForm.description.trim() ||
            eventForm.price < 0 ||
            eventForm.capacity < 1
        ) {
            notify("Complete the required event fields.");
            return;
        }

        const venue = venues.find((item) => item.id === eventForm.venueId);

        if (!venue || !venue.active) {
            notify("Select an active venue.");
            return;
        }

        if (eventForm.capacity > venue.capacity) {
            notify(
                `Event capacity cannot exceed venue capacity (${venue.capacity}).`,
            );
            return;
        }

        if (editingEventId) {
            const updated = await apiUpdateEvent(editingEventId, eventForm);
            if (updated) {
                setEvents((current) =>
                    current.map((event) =>
                        event.id === editingEventId ? updated : event,
                    ),
                );
                notify("Event updated.");
            }
        } else {
            const saved = await apiSaveEvent(eventForm);
            if (saved) {
                setEvents((current) => [...current, saved]);
                notify("Event created as a draft.");
            }
        }

        setEventForm(null);
        setEditingEventId(null);
    }

    function editVenue(venue: Venue) {
        setEditingVenueId(venue.id);
        setVenueForm({ ...venue });
    }

    function editEvent(event: EventItem) {
        setEditingEventId(event.id);
        setEventForm({ ...event });
    }

    async function deleteVenue(venue: Venue) {
        const hasEvents = events.some(
            (event) => event.venueId === venue.id,
        );

        if (hasEvents) {
            notify("This venue has events. Deactivate it instead.");
            return;
        }

        if (!window.confirm(`Delete ${venue.name}?`)) return;

        const success = await apiDeleteVenue(venue.id);
        if (success) {
            setVenues((current) =>
                current.filter((item) => item.id !== venue.id),
            );
            notify("Venue deleted.");
        } else {
            notify("Failed to delete venue.");
        }
    }

    async function deleteEvent(event: EventItem) {
        if (!window.confirm(`Delete ${event.title}?`)) return;

        const success = await apiDeleteEvent(event.id);
        if (success) {
            setEvents((current) =>
                current.filter((item) => item.id !== event.id),
            );
            notify("Event deleted.");
        } else {
            notify("Failed to delete event.");
        }
    }

    async function toggleVenue(venue: Venue) {
        if (venue.active) {
            const hasUpcomingPublishedEvent = events.some(
                (event) =>
                    event.venueId === venue.id &&
                    event.status === "PUBLISHED" &&
                    event.date >= new Date().toISOString().slice(0, 10),
            );

            if (hasUpcomingPublishedEvent) {
                notify("This venue has upcoming published events.");
                return;
            }
        }

        const nextActive = !venue.active;
        const updated = await apiUpdateVenue(venue.id, { ...venue, active: nextActive });

        if (updated) {
            setVenues((current) =>
                current.map((item) =>
                    item.id === venue.id ? updated : item,
                ),
            );
            notify(nextActive ? "Venue activated." : "Venue deactivated.");
        } else {
            notify("Failed to update venue status.");
        }
    }

    async function toggleEventStatus(event: EventItem) {
        if (event.status === "CANCELLED") {
            notify("Cancelled events cannot be republished in this prototype.");
            return;
        }

        const nextStatus: EventStatus =
            event.status === "PUBLISHED" ? "DRAFT" : "PUBLISHED";

        const updated = await apiUpdateEvent(event.id, { ...event, status: nextStatus });

        if (updated) {
            setEvents((current) =>
                current.map((item) =>
                    item.id === event.id ? updated : item,
                ),
            );

            notify(
                nextStatus === "PUBLISHED"
                    ? "Event published."
                    : "Event moved back to draft.",
            );
        } else {
            notify("Failed to update event status.");
        }
    }

    async function cancelEvent(event: EventItem) {
        if (!window.confirm(`Cancel ${event.title}?`)) return;

        const updated = await apiUpdateEvent(event.id, { ...event, status: "CANCELLED" });

        if (updated) {
            setEvents((current) =>
                current.map((item) =>
                    item.id === event.id ? updated : item,
                ),
            );
            notify("Event marked as cancelled.");
        } else {
            notify("Failed to cancel event.");
        }
    }

    const navigation: {
        label: View;
        icon: typeof LayoutDashboard;
    }[] = [
        { label: "Dashboard", icon: LayoutDashboard },
        { label: "Venues", icon: Building2 },
        { label: "Events", icon: CalendarDays },
        { label: "Inventory", icon: Armchair },
    ];

    return (
        <div className="admin-layout">
            <aside className="admin-sidebar">
                <div className="admin-brand">
                    <span className="admin-brand-mark">A</span>
                    <span>
                        <span className="admin-brand-text">
                            afterglow<span className="admin-brand-dot">.</span>
                        </span>
                        <small>EVENT OPERATIONS</small>
                    </span>
                </div>

                <div className="admin-side-label">WORKSPACE</div>

                <nav className="admin-navigation">
                    {navigation.map(({ label, icon: Icon }) => (
                        <button
                            key={label}
                            className={
                                view === label
                                    ? "admin-nav-item active"
                                    : "admin-nav-item"
                            }
                            onClick={() => {
                                setView(label);
                                setQuery("");
                            }}
                        >
                            <Icon size={17} />
                            <span>{label}</span>
                            {label === "Events" && (
                                <span className="admin-nav-count">{events.length}</span>
                            )}
                        </button>
                    ))}
                </nav>

                <div className="admin-sidebar-bottom">
                    <div className="admin-online-dot" />
                    <div>
                        <strong>Prototype environment</strong>
                        <small>Mock data · No backend</small>
                    </div>
                </div>
            </aside>

            <section className="admin-main">
                <header className="admin-topbar">
                    <div>
                        <span className="admin-breadcrumb">WORKSPACE / ADMIN</span>
                        <h2>{view}</h2>
                    </div>

                    <div className="admin-user">
                        <span className="admin-user-avatar">A</span>
                        <span>
                            <strong>Event Admin</strong>
                            <small>Administrator</small>
                        </span>
                    </div>
                </header>

                <div className="admin-content">
                    {view === "Dashboard" && (
                        <>
                            <div className="admin-welcome">
                                <div>
                                    <span className="admin-kicker">
                                        <Activity size={13} /> OPERATIONS OVERVIEW
                                    </span>
                                    <h1>Make every event <em>count.</em></h1>
                                    <p>
                                        Your events, venues, and inventory — all in one place.
                                    </p>
                                </div>
                                <button
                                    className="admin-primary-button"
                                    onClick={() => {
                                        setEventForm({
                                            ...emptyEvent,
                                            venueId: venues.find((v) => v.active)?.id ?? "",
                                        });
                                        setEditingEventId(null);
                                    }}
                                >
                                    <Plus size={16} /> Create event
                                </button>
                            </div>

                            <div className="admin-metrics">
                                <Metric
                                    label="Active venues"
                                    value={activeVenues}
                                    note={`${venues.length} total venues`}
                                    icon={<Building2 size={18} />}
                                    trend="VENUES"
                                />
                                <Metric
                                    label="Published events"
                                    value={publishedEvents}
                                    note="Visible to customers"
                                    icon={<CalendarDays size={18} />}
                                    trend="LIVE"
                                />
                                <Metric
                                    label="Draft events"
                                    value={draftEvents}
                                    note="Awaiting publication"
                                    icon={<Clock3 size={18} />}
                                    trend="DRAFT"
                                />
                                <Metric
                                    label="Event capacity"
                                    value={events
                                        .filter((e) => e.status === "PUBLISHED")
                                        .reduce((sum, e) => sum + e.capacity, 0)
                                        .toLocaleString("en-IN")}
                                    note="Across published events"
                                    icon={<Users size={18} />}
                                    trend="SEATS"
                                />
                            </div>

                            <div className="admin-dashboard-grid">
                                <section className="admin-panel">
                                    <div className="admin-panel-heading">
                                        <div>
                                            <span className="admin-kicker">YOUR LINEUP</span>
                                            <h3>Recent events</h3>
                                        </div>
                                        <button
                                            className="admin-link-button"
                                            onClick={() => setView("Events")}
                                        >
                                            All events <ChevronRight size={15} />
                                        </button>
                                    </div>

                                    <div className="admin-table-wrap">
                                        <table className="admin-table">
                                            <thead>
                                                <tr>
                                                    <th>EVENT</th>
                                                    <th>VENUE</th>
                                                    <th>STATUS</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {events.slice(0, 5).map((event) => (
                                                    <tr key={event.id}>
                                                        <td>
                                                            <strong>{event.title}</strong>
                                                            <small>{event.date}</small>
                                                        </td>
                                                        <td>{venueName(event.venueId)}</td>
                                                        <td>
                                                            <Status status={event.status} />
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                </section>

                                <section className="admin-panel admin-quick-panel">
                                    <span className="admin-kicker">QUICK ACTIONS</span>
                                    <h3>What are we planning?</h3>
                                    <p>
                                        Set up a venue or get your next event ready to go live.
                                    </p>

                                    <button
                                        className="admin-quick-action"
                                        onClick={() => {
                                            setView("Venues");
                                            setVenueForm({ ...emptyVenue });
                                            setEditingVenueId(null);
                                        }}
                                    >
                                        <span className="admin-action-icon">
                                            <Building2 size={17} />
                                        </span>
                                        <span>
                                            <strong>Add a venue</strong>
                                            <small>Create a new event location</small>
                                        </span>
                                        <ArrowUpRight size={16} />
                                    </button>

                                    <button
                                        className="admin-quick-action"
                                        onClick={() => {
                                            setEventForm({
                                                ...emptyEvent,
                                                venueId: venues.find((v) => v.active)?.id ?? "",
                                            });
                                            setEditingEventId(null);
                                        }}
                                    >
                                        <span className="admin-action-icon lime">
                                            <CalendarDays size={17} />
                                        </span>
                                        <span>
                                            <strong>Create an event</strong>
                                            <small>Choose venue, date, and pricing</small>
                                        </span>
                                        <ArrowUpRight size={16} />
                                    </button>

                                    <button
                                        className="admin-quick-action"
                                        onClick={() => setView("Inventory")}
                                    >
                                        <span className="admin-action-icon purple">
                                            <Ticket size={17} />
                                        </span>
                                        <span>
                                            <strong>Manage inventory</strong>
                                            <small>Review ticket allocations</small>
                                        </span>
                                        <ArrowUpRight size={16} />
                                    </button>
                                </section>
                            </div>

                            <div className="admin-info-banner">
                                <Activity size={19} />
                                <div>
                                    <strong>Event & Inventory Service · Port 8081</strong>
                                    <p>
                                        Venue and event management will be connected to this
                                        service's REST APIs. Publishing can later emit a Kafka
                                        domain event if other services need to react.
                                    </p>
                                </div>
                                <span className="admin-demo-tag">MOCK MODE</span>
                            </div>
                        </>
                    )}

                    {view === "Venues" && (
                        <>
                            <div className="admin-page-heading">
                                <div>
                                    <span className="admin-kicker">LOCATIONS & CAPACITY</span>
                                    <h1>Venue <em>management.</em></h1>
                                    <p>
                                        Manage the places where your experiences come to life.
                                    </p>
                                </div>
                                <button
                                    className="admin-primary-button"
                                    onClick={() => {
                                        setVenueForm({ ...emptyVenue });
                                        setEditingVenueId(null);
                                    }}
                                >
                                    <CirclePlus size={16} /> Add venue
                                </button>
                            </div>

                            <SearchBar query={query} setQuery={setQuery} placeholder="Search venues or cities..." />

                            <div className="admin-venue-grid">
                                {filteredVenues.map((venue, index) => (
                                    <VenueCard
                                        key={venue.id}
                                        venue={venue}
                                        index={index}
                                        onEdit={editVenue}
                                        onToggle={toggleVenue}
                                        onDelete={deleteVenue}
                                    />
                                ))}

                                <button
                                    className="admin-add-card"
                                    onClick={() => {
                                        setVenueForm({ ...emptyVenue });
                                        setEditingVenueId(null);
                                    }}
                                >
                                    <span><Plus size={22} /></span>
                                    <strong>Add another venue</strong>
                                    <small>Expand your event locations</small>
                                </button>
                            </div>
                        </>
                    )}

                    {view === "Events" && (
                        <>
                            <div className="admin-page-heading">
                                <div>
                                    <span className="admin-kicker">CONTENT & PUBLICATION</span>
                                    <h1>Event <em>management.</em></h1>
                                    <p>
                                        Create events, configure ticket pricing, and manage
                                        publication status.
                                    </p>
                                </div>
                                <button
                                    className="admin-primary-button"
                                    onClick={() => {
                                        setEventForm({
                                            ...emptyEvent,
                                            venueId: venues.find((v) => v.active)?.id ?? "",
                                        });
                                        setEditingEventId(null);
                                    }}
                                >
                                    <CirclePlus size={16} /> Create event
                                </button>
                            </div>

                            <SearchBar query={query} setQuery={setQuery} placeholder="Search events, venues, or status..." />

                            <EventTable
                                events={filteredEvents}
                                getVenueName={venueName}
                                formatCurrency={currency}
                                onEdit={editEvent}
                                onToggleStatus={toggleEventStatus}
                                onDelete={deleteEvent}
                            />

                            <div className="admin-info-banner">
                                <CalendarDays size={19} />
                                <div>
                                    <strong>Publication workflow</strong>
                                    <p>
                                        Draft events remain unpublished. Publishing makes the
                                        event eligible for customer discovery once the backend
                                        and inventory rules are connected.
                                    </p>
                                </div>
                            </div>
                        </>
                    )}

                    {view === "Inventory" && (
                        <>
                            <div className="admin-page-heading">
                                <div>
                                    <span className="admin-kicker">TICKET ALLOCATION</span>
                                    <h1>Inventory <em>overview.</em></h1>
                                    <p>
                                        Review event capacities and configure initial ticket
                                        allocations.
                                    </p>
                                </div>
                            </div>

                            <div className="admin-info-banner">
                                <Armchair size={19} />
                                <div>
                                    <strong>Prototype inventory model</strong>
                                    <p>
                                        This screen shows event capacity and base ticket pricing.
                                        Per-category quotas, sold counts, holds, and seat-level
                                        reservations should be persisted by Event & Inventory
                                        Service. The customer seat map is currently a separate
                                        mock.
                                    </p>
                                </div>
                            </div>

                            <InventoryTable
                                events={events}
                                venues={venues}
                                formatCurrency={currency}
                                onEdit={editEvent}
                            />
                        </>
                    )}
                </div>
            </section>

            {venueForm && (
                <Modal
                    title={editingVenueId ? "Edit venue" : "Create venue"}
                    kicker="VENUE MANAGEMENT"
                    onClose={() => {
                        setVenueForm(null);
                        setEditingVenueId(null);
                    }}
                >
                    <VenueForm
                        venueForm={venueForm}
                        setVenueForm={setVenueForm}
                        editingVenueId={editingVenueId}
                        onSave={saveVenue}
                        onCancel={() => {
                            setVenueForm(null);
                            setEditingVenueId(null);
                        }}
                    />
                </Modal>
            )}

            {eventForm && (
                <Modal
                    title={editingEventId ? "Edit event" : "Create event"}
                    kicker="EVENT MANAGEMENT"
                    onClose={() => {
                        setEventForm(null);
                        setEditingEventId(null);
                    }}
                >
                    <EventForm
                        eventForm={eventForm}
                        setEventForm={setEventForm}
                        editingEventId={editingEventId}
                        venues={venues}
                        onSave={saveEvent}
                        onCancel={() => {
                            setEventForm(null);
                            setEditingEventId(null);
                        }}
                    />
                </Modal>
            )}

            {toast && (
                <div className="admin-toast">
                    <Check size={16} /> {toast}
                </div>
            )}
        </div>
    );
}