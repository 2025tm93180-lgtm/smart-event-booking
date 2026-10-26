import { useCallback } from 'react';
import { Venue } from '../components/adminportal/types';

const VENUE_SERVICE_BASE_URL = 'http://localhost:9093'; // Assuming a port for venue service

export const useVenueApi = () => {
  const fetchVenues = useCallback(async (): Promise<Venue[]> => {
    try {
      const response = await fetch(`${VENUE_SERVICE_BASE_URL}/api/v1/venues`);
      if (!response.ok) {
        throw new Error(`Failed to fetch venues: ${response.status}`);
      }
      return await response.json();
    } catch (error) {
      console.error('[useVenueApi] Error fetching venues:', error);
      return [];
    }
  }, []);

  const saveVenue = useCallback(async (venue: Omit<Venue, 'id'>): Promise<Venue | null> => {
    try {
      const response = await fetch(`${VENUE_SERVICE_BASE_URL}/api/v1/venues`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(venue),
      });
      if (!response.ok) {
        throw new Error(`Failed to save venue: ${response.status}`);
      }
      return await response.json();
    } catch (error) {
      console.error('[useVenueApi] Error saving venue:', error);
      return null;
    }
  }, []);

  const updateVenue = useCallback(async (id: string, venue: Partial<Venue>): Promise<Venue | null> => {
    try {
      const response = await fetch(`${VENUE_SERVICE_BASE_URL}/api/v1/venues/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(venue),
      });
      if (!response.ok) {
        throw new Error(`Failed to update venue: ${response.status}`);
      }
      return await response.json();
    } catch (error) {
      console.error('[useVenueApi] Error updating venue:', error);
      return null;
    }
  }, []);

  return { fetchVenues, saveVenue, updateVenue };
};
