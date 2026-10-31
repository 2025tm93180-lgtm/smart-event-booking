import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { UserPlus, Mail, User as UserIcon, Calendar } from 'lucide-react';
import { User } from '../types';
import { setUser } from '../store/userSlice';

interface SignupProps {
  onSignup: (user: User) => void;
}

export const Signup: React.FC<SignupProps> = ({ onSignup }) => {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    dateOfBirth: ''
  });
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    // Simulate signup
    const newUser: User = {
      id: Math.random().toString(36).substring(2, 9),
      name: formData.name,
      email: formData.email,
      dateOfBirth: formData.dateOfBirth,
      memberSince: new Date().toLocaleDateString('en-US', { month: 'short', year: 'numeric' })
    };

    dispatch(setUser(newUser));
    onSignup(newUser);
    navigate('/profile');
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <div className="auth-icon-circle">
            <UserPlus size={24} color="var(--primary)" />
          </div>
          <h2>Create Account</h2>
          <p>Join Afterglow and start booking events</p>
        </div>

        <form onSubmit={handleSubmit} className="auth-form">
          {error && <div className="auth-error">{error}</div>}
          
          <div className="form-group">
            <label htmlFor="name"><UserIcon size={16} /> Name</label>
            <input
              type="text"
              id="name"
              name="name"
              placeholder="Enter your name"
              value={formData.name}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="email"><Mail size={16} /> Email Address</label>
            <input
              type="email"
              id="email"
              name="email"
              placeholder="your@email.com"
              value={formData.email}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="dateOfBirth"><Calendar size={16} /> Date of Birth</label>
            <input
              type="date"
              id="dateOfBirth"
              name="dateOfBirth"
              value={formData.dateOfBirth}
              onChange={handleChange}
              required
            />
          </div>

          <button type="submit" className="full-button">Sign Up</button>
        </form>

        <div className="auth-footer">
          <p>Already have an account? <Link to="/login">Log In</Link></p>
        </div>
      </div>
    </div>
  );
};
