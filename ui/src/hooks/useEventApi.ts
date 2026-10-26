import { useCallback } from 'react';
import { EventItem } from '../components/adminportal/types';

const EVENT_SERVICE_BASE_URL = 'http://localhost:9092'; // Assuming a port for event service

export const useEventApi = () => {
  const fetchEvents = useCallback(async (): Promise<EventItem[]> => {
    try {
      const response = await fetch(`${EVENT_SERVICE_BASE_URL}/api/v1/events`);
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
      const response = await fetch(`${EVENT_SERVICE_BASE_URL}/api/v1/events`, {
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
      const response = await fetch(`${EVENT_SERVICE_BASE_URL}/api/v1/events/${id}`, {
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

  return { fetchEvents, saveEvent, updateEvent };
};
