import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import logo from '../assets/images/logo.png';
import './AppLayout.css';

interface AppLayoutProps {
  children: React.ReactNode;
}

const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);

  const handleLogout = () => {
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <div className="app-layout">
      <aside className={`side-menu${collapsed ? ' collapsed' : ''}`}>
        <button
          type="button"
          className="side-menu-tab"
          onClick={() => setCollapsed((open) => !open)}
          aria-label={collapsed ? 'Abrir menu' : 'Recolher menu'}
          title={collapsed ? 'Abrir menu' : 'Recolher menu'}
        >
          {collapsed ? '›' : '‹'}
        </button>
        <div className="side-menu-header">
          <img src={logo} alt="InvestTrack" className="side-menu-logo" />
        </div>
        <nav className="side-menu-nav">
          <Link
            to="/dashboard"
            className={`side-menu-item ${location.pathname === '/dashboard' ? 'active' : ''}`}
          >
            Dashboard
          </Link>
          <Link
            to="/panel"
            className={`side-menu-item ${location.pathname.startsWith('/panel') ? 'active' : ''}`}
          >
            Panel
          </Link>
          <Link
            to="/transactions"
            className={`side-menu-item ${
              location.pathname === '/transactions' || location.pathname.startsWith('/transacao')
                ? 'active'
                : ''
            }`}
          >
            Transações
          </Link>
          <Link
            to="/upload"
            className={`side-menu-item ${location.pathname === '/upload' ? 'active' : ''}`}
          >
            Upload
          </Link>
        </nav>
        <div className="side-menu-footer">
          <button onClick={handleLogout} className="btn btn-secondary side-menu-logout">
            Logout
          </button>
        </div>
      </aside>
      <main className="app-layout-main">
        {children}
      </main>
    </div>
  );
};

export default AppLayout;
