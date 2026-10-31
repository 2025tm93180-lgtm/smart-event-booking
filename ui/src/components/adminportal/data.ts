import { Venue, EventItem } from './types';

export const initialVenues: Venue[] = [
    {
        id: "VEN-001",
        name: "Skyline Arena",
        city: "Hyderabad",
        address: "HITEC City, Hyderabad",
        capacity: 12000,
        facilities: "Parking, Food court, Accessibility",
        active: true,
    },
    {
        id: "VEN-002",
        name: "The Glasshouse",
        city: "Bengaluru",
        address: "Indiranagar, Bengaluru",
        capacity: 1800,
        facilities: "Parking, Cafe, Accessibility",
        active: true,
    },
    {
        id: "VEN-003",
        name: "Blue Note Room",
        city: "Mumbai",
        address: "Bandra West, Mumbai",
        capacity: 350,
        facilities: "Food, Bar, Accessibility",
        active: true,
    },
];

export const initialEvents: EventItem[] = [
    {
        id: "EVT-101",
        title: "Midnight Frequencies",
        venueId: "VEN-001",
        date: "2026-11-14",
        time: "19:30",
        category: "Music",
        price: 1499,
        capacity: 12000,
        description: "An immersive night of live electronic music.",
        status: "PUBLISHED",
    },
    {
        id: "EVT-102",
        title: "The Art of Motion",
        venueId: "VEN-002",
        date: "2026-11-15",
        time: "17:00",
        category: "Arts",
        price: 799,
        capacity: 1800,
        description: "Contemporary dance and visual storytelling.",
        status: "PUBLISHED",
    },
    {
        id: "EVT-103",
        title: "After Hours: Jazz Club",
        venueId: "VEN-003",
        date: "2026-11-27",
        time: "20:00",
        category: "Music",
        price: 999,
        capacity: 350,
        description: "An intimate late-night jazz performance.",
        status: "DRAFT",
    },
];

export const emptyVenue: Venue = {
    id: "",
    name: "",
    city: "",
    address: "",
    capacity: 100,
    facilities: "",
    active: true,
};

export const emptyEvent: EventItem = {
    id: "",
    title: "",
    venueId: "",
    date: "",
    time: "19:00",
    category: "Music",
    price: 499,
    capacity: 100,
    description: "",
    status: "DRAFT",
};
