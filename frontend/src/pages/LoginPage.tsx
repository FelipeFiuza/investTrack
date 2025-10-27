import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { usuarioApi } from '../services/api';
import './LoginPage.css';

const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      // Skip validation - just create a mock user and navigate to dashboard
      const mockUser = {
        idUsuario: 1,
        nome: email || 'Demo User',
        token: password || 'demo-token'
      };
      
      localStorage.setItem('user', JSON.stringify(mockUser));
      navigate('/dashboard');
    } catch (error) {
      setError('Login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <h1>InvestTrack</h1>
          <p>Track your investments with ease</p>
        </div>
        
        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label htmlFor="email" className="form-label">
              Email
            </label>
            <input
              type="email"
              id="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="form-input"
              placeholder="Enter your email"
              required
            />
          </div>
          
          <div className="form-group">
            <label htmlFor="password" className="form-label">
              Password
            </label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="form-input"
              placeholder="Enter your password"
              required
            />
          </div>
          
          {error && (
            <div className="error-message">
              {error}
            </div>
          )}
          
          <button
            type="submit"
            className="btn btn-primary login-button"
            disabled={loading}
          >
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>
        
        <div className="quick-login">
          <button
            onClick={() => {
              const mockUser = { idUsuario: 1, nome: 'Demo User', token: 'demo-token' };
              localStorage.setItem('user', JSON.stringify(mockUser));
              navigate('/dashboard');
            }}
            className="btn btn-secondary"
            style={{ width: '100%', marginTop: '16px' }}
          >
            Quick Demo Login (Skip Validation)
          </button>
        </div>
        
        <div className="login-footer">
          <p>New to InvestTrack? Just sign in to create your account!</p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
