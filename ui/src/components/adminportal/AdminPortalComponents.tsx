import React from 'react';
import { Search } from 'lucide-react';
import { EventStatus } from './types';

export function Metric({
    label,
    value,
    note,
    icon,
    trend,
}: {
    label: string;
    value: string | number;
    note: string;
    icon: React.ReactNode;
    trend: string;
}) {
    return (
        <article className="admin-metric-card">
            <div className="admin-metric-top">
                <span className="admin-metric-icon">{icon}</span>
                <span className="admin-metric-tag">{trend}</span>
            </div>
            <span className="admin-metric-label">{label}</span>
            <strong className="admin-metric-value">{value}</strong>
            <span className="admin-metric-note">{note}</span>
        </article>
    );
}

export function Status({ status }: { status: EventStatus }) {
    return (
        <span className={`admin-status status-${status.toLowerCase()}`}>
            <span /> {status}
        </span>
    );
}

export function SearchBar({
    query,
    setQuery,
    placeholder,
}: {
    query: string;
    setQuery: (value: string) => void;
    placeholder: string;
}) {
    return (
        <div className="admin-search">
            <Search size={17} />
            <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder={placeholder}
            />
        </div>
    );
}
