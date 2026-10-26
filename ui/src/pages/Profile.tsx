import React from 'react';
import { Navigate } from 'react-router-dom';
import { User as UserIcon, Mail, Calendar, MapPin, CreditCard, Shield } from 'lucide-react';
import { User } from '../types';

interface ProfileProps {
  user: User | null;
  isAuthenticated: boolean;
  onLogout: () => void;
}

export const Profile: React.FC<ProfileProps> = ({ user, isAuthenticated, onLogout }) => {
  if (!isAuthenticated || !user) {
    return <Navigate to="/signup" replace />;
  }

  return (
    <div className="profile-container">
      <div className="profile-header">
        <div className="profile-avatar-large">
          {user.avatar ? (
            <img src={user.avatar} alt={user.name} />
          ) : (
            <div className="avatar-placeholder">{user.name.charAt(0)}</div>
          )}
        </div>
        <div className="profile-info">
          <h1>{user.name}</h1>
          <p className="profile-email">{user.email}</p>
          <span className="member-badge">Member since {user.memberSince}</span>
        </div>
        <button onClick={onLogout} className="logout-button-alt">Log Out</button>
      </div>

      <div className="profile-grid">
        <div className="profile-card">
          <h3><UserIcon size={20} /> Personal Information</h3>
          <div className="profile-details">
            <div className="detail-item">
              <span className="detail-label">Full Name</span>
              <span className="detail-value">{user.name}</span>
            </div>
            <div className="detail-item">
              <span className="detail-label">Email Address</span>
              <span className="detail-value">{user.email}</span>
            </div>
            {user.dateOfBirth && (
              <div className="detail-item">
                <span className="detail-label">Date of Birth</span>
                <span className="detail-value">{user.dateOfBirth}</span>
              </div>
            )}
            {user.phone && (
              <div className="detail-item">
                <span className="detail-label">Phone Number</span>
                <span className="detail-value">{user.phone}</span>
              </div>
            )}
          </div>
        </div>

        <div className="profile-card">
          <h3><Shield size={20} /> Account Security</h3>
          <div className="profile-details">
            <div className="detail-item">
              <span className="detail-label">Two-Factor Auth</span>
              <span className="detail-value">Disabled</span>
              <button className="text-button">Enable</button>
            </div>
          </div>
        </div>

        <div className="profile-card">
          <h3><CreditCard size={20} /> Payment Methods</h3>
          <div className="empty-state">
            <p>No payment methods saved</p>
            <button className="outline-button">Add Card</button>
          </div>
        </div>

        <div className="profile-card">
          <h3><MapPin size={20} /> Saved Addresses</h3>
          <div className="empty-state">
            <p>No addresses saved</p>
            <button className="outline-button">Add Address</button>
          </div>
        </div>
      </div>
    </div>
  );
};
