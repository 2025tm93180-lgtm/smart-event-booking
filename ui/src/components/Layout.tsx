import React, { useState } from 'react';
import { NavLink, Link, useNavigate } from 'react-router-dom';
import { Zap, Heart, ChevronDown, Menu, User, LogOut } from 'lucide-react';
import { User as UserType } from '../types';

interface LayoutProps {
  children: React.ReactNode;
  bookingsCount: number;
  favoritesCount: number;
  notify: (message: string) => void;
  user: UserType | null;
  isAuthenticated: boolean;
  onLogout: () => void;
}

export const Layout: React.FC<LayoutProps> = ({ children, bookingsCount, favoritesCount, notify, user, isAuthenticated, onLogout }) => {
  const [mobileMenu, setMobileMenu] = useState(false);
  const [showProfileDropdown, setShowProfileDropdown] = useState(false);
  const navigate = useNavigate();

  const handleLogout = () => {
    onLogout();
    setShowProfileDropdown(false);
    notify('Logged out successfully');
    navigate('/');
  };

  return (
    <div className="app-shell">
      <header className="topbar">
        <Link to="/" className="brand" aria-label="Afterglow home">
          <span className="brand-mark"><Zap size={19} fill="currentColor" /></span>
          <span>afterglow<span className="brand-dot">.</span></span>
        </Link>
        <nav className={`main-nav ${mobileMenu ? 'nav-open' : ''}`}>
          <NavLink 
            to="/" 
            className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}
            onClick={() => setMobileMenu(false)}
            end
          >
            Discover
          </NavLink>
          <NavLink 
            to="/bookings" 
            className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}
            onClick={() => setMobileMenu(false)}
          >
            My bookings <span className="nav-count">{bookingsCount}</span>
          </NavLink>
          <NavLink
              to="/admin-portal"
              className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}
              onClick={() => setMobileMenu(false)}
          >
            Admin Portal
          </NavLink>
          <NavLink 
            to="/tickets" 
            className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}
            onClick={() => setMobileMenu(false)}
          >
            My tickets
          </NavLink>
        </nav>
        <div className="top-actions">
          <Link to="/favorites" className="icon-button saved-button" title="Saved events">
            <Heart size={18} />
            {favoritesCount > 0 && <span className="nav-count">{favoritesCount}</span>}
          </Link>
          
          <div className="profile-wrapper">
            {isAuthenticated && user ? (
              <>
                <button className="profile-button" onClick={() => setShowProfileDropdown(!showProfileDropdown)}>
                  <span className="avatar">{user.name.charAt(0)}</span>
                  <span className="profile-name">{user.name.split(' ')[0]}</span>
                  <ChevronDown size={14} />
                </button>
                {showProfileDropdown && (
                  <div className="profile-dropdown">
                    <Link to="/profile" className="dropdown-item" onClick={() => setShowProfileDropdown(false)}>
                      <User size={16} /> Profile
                    </Link>
                    <button className="dropdown-item logout-item" onClick={handleLogout}>
                      <LogOut size={16} /> Log Out
                    </button>
                  </div>
                )}
              </>
            ) : (
              <div className="nav-auth-actions">
                <Link to="/signup" className="signup-nav-link">Sign Up</Link>
              </div>
            )}
          </div>
          
          <button className="icon-button mobile-menu" onClick={() => setMobileMenu(!mobileMenu)}><Menu size={20}/></button>
        </div>
      </header>

      <main>
        {children}
      </main>

      <footer className="footer">
        <Link to="/" className="brand footer-brand">
          <span className="brand-mark"><Zap size={17} fill="currentColor"/></span>
          <span>afterglow<span className="brand-dot">.</span></span>
        </Link>
        <span>Made for the moments that move you.</span>
        <span className="footer-status"><span className="live-dot"/> PROTOTYPE MODE</span>
      </footer>
    </div>
  );
};
