import { useCallback } from 'react';
import { EventItem } from '../components/adminportal/types';

const EVENT_SERVICE_BASE_URL = 'http://localhost:9091';

export const useEventApi = () => {
  const fetchEvents = useCallback(async (): Promise<EventItem[]> => {
    try {
      const response = await fetch(`${EVENT_SERVICE_BASE_URL}/api/events`);
      if (!response.ok) {
        throw new Error(`Failed to fetch events: ${response.status}`);
      }
      return await response.json();
    } catch (error) {
      console.error('[useEventApi] Error fetching events:', error);
      return [];
    }
  }, []);

  const saveEvent = useCallback(async (event: Omit<EventItem, 'id'>): Promise<EventItem | null> => {
    try {
      const response = await fetch(`${EVENT_SERVICE_BASE_URL}/api/events`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(event),
      });
      if (!response.ok) {
        throw new Error(`Failed to save event: ${response.status}`);
      }
      return await response.json();
    } catch (error) {
      console.error('[useEventApi] Error saving event:', error);
      return null;
    }
  }, []);

  const updateEvent = useCallback(async (id: string, event: Partial<EventItem>): Promise<EventItem | null> => {
    try {
      const response = await fetch(`${EVENT_SERVICE_BASE_URL}/api/events/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(event),
      });
      if (!response.ok) {
        throw new Error(`Failed to update event: ${response.status}`);
      }
      return await response.json();
    } catch (error) {
      console.error('[useEventApi] Error updating event:', error);
      return null;
    }
  }, []);

  const deleteEvent = useCallback(async (id: string): Promise<boolean> => {
    try {
      const response = await fetch(`${EVENT_SERVICE_BASE_URL}/api/events/${id}`, {
        method: 'DELETE',
      });
      return response.ok;
    } catch (error) {
      console.error('[useEventApi] Error deleting event:', error);
      return false;
    }
  }, []);

  return { fetchEvents, saveEvent, updateEvent, deleteEvent };
};
