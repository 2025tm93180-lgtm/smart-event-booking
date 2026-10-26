import { useCallback } from 'react';
import { Venue } from '../components/adminportal/types';

const VENUE_SERVICE_BASE_URL = 'http://localhost:9091';

export const useVenueApi = () => {
  const fetchVenues = useCallback(async (): Promise<Venue[]> => {
    try {
      const response = await fetch(`${VENUE_SERVICE_BASE_URL}/api/venues`);
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
      const response = await fetch(`${VENUE_SERVICE_BASE_URL}/api/venues`, {
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
      const response = await fetch(`${VENUE_SERVICE_BASE_URL}/api/venues/${id}`, {
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

  const deleteVenue = useCallback(async (id: string): Promise<boolean> => {
    try {
      const response = await fetch(`${VENUE_SERVICE_BASE_URL}/api/venues/${id}`, {
        method: 'DELETE',
      });
      return response.ok;
    } catch (error) {
      console.error('[useVenueApi] Error deleting venue:', error);
      return false;
    }
  }, []);

  return { fetchVenues, saveVenue, updateVenue, deleteVenue };
};
