import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { User, AuthState } from '../types';

const initialState: AuthState = {
  user: null,
  isAuthenticated: false,
  favorites: []
};

const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    setUser: (state, action: PayloadAction<User>) => {
      state.user = action.payload;
      state.isAuthenticated = true;
    },
    logout: (state) => {
      state.user = null;
      state.isAuthenticated = false;
    },
    toggleFavorite: (state, action: PayloadAction<number>) => {
      const eventId = action.payload;
      if (state.favorites.includes(eventId)) {
        state.favorites = state.favorites.filter(id => id !== eventId);
      } else {
        state.favorites.push(eventId);
      }
    }
  }
});

export const { setUser, logout, toggleFavorite } = userSlice.actions;
export default userSlice.reducer;
