import {Venue} from "../types.ts";
import {Check} from "lucide-react";

interface VenueFormProps {
    venueForm: Venue;
    setVenueForm: (venue: Venue) => void;
    editingVenueId: string | null;
    onSave: () => void;
    onCancel: () => void;
}

export function VenueForm({
                              venueForm,
                              setVenueForm,
                              editingVenueId,
                              onSave,
                              onCancel,
                          }: VenueFormProps) {
    return (
        <div className="admin-form">
            <label>
                Venue name *
                <input
                    value={venueForm.name}
                    onChange={(e) =>
                        setVenueForm({ ...venueForm, name: e.target.value })
                    }
                    placeholder="e.g. Skyline Arena"
                />
            </label>
            <div className="admin-form-row">
                <label>
                    City *
                    <input
                        value={venueForm.city}
                        onChange={(e) =>
                            setVenueForm({ ...venueForm, city: e.target.value })
                        }
                        placeholder="Hyderabad"
                    />
                </label>
                <label>
                    Maximum capacity *
                    <input
                        type="number"
                        min="1"
                        value={venueForm.capacity}
                        onChange={(e) =>
                            setVenueForm({
                                ...venueForm,
                                capacity: Number(e.target.value),
                            })
                        }
                    />
                </label>
            </div>
            <label>
                Address *
                <input
                    value={venueForm.address}
                    onChange={(e) =>
                        setVenueForm({
                            ...venueForm,
                            address: e.target.value,
                        })
                    }
                    placeholder="Street, area, city"
                />
            </label>
            <label>
                Facilities
                <input
                    value={venueForm.facilities}
                    onChange={(e) =>
                        setVenueForm({
                            ...venueForm,
                            facilities: e.target.value,
                        })
                    }
                    placeholder="Parking, Food court, Accessibility"
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
                    <Check size={15} /> Save venue
                </button>
            </div>
        </div>
    );
}

