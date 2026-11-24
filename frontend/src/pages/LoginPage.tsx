import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { usuarioApi } from '../services/api';
import logo from '../assets/images/logo.png';
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
          <img src={logo} alt="InvestTrack" className="login-logo" />
          <p>Acompanhe seus investimentos com facilidade</p>
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
              placeholder="Insira seu email"
              required
            />
          </div>
          
          <div className="form-group">
            <label htmlFor="password" className="form-label">
              Senha
            </label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="form-input"
              placeholder="Insira sua senha"
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
            {loading ? 'Entrando...' : 'Entrar'}
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
            Login de demonstração (Sem necessidade de registro)
          </button>
        </div>
        
        <div className="login-footer">
          <p>Não tem conta? Crie aqui!</p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
