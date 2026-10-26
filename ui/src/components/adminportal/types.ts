export type Venue = {
    id: string;
    name: string;
    city: string;
    address: string;
    capacity: number;
    facilities: string;
    active: boolean;
};

export type EventStatus = "Draft" | "Published" | "Cancelled";

export type EventItem = {
    id: string;
    title: string;
    venueId: string;
    date: string;
    time: string;
    category: string;
    price: number;
    capacity: number;
    description: string;
    status: EventStatus;
};

export type View = "Dashboard" | "Venues" | "Events" | "Inventory";
