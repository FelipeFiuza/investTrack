import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import './AppLayout.css';

interface AppLayoutProps {
  children: React.ReactNode;
}

const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <div className="app-layout">
      <aside className="side-menu">
        <div className="side-menu-header">
          <h2 className="side-menu-title">InvestTrack</h2>
        </div>
        <nav className="side-menu-nav">
          <Link
            to="/dashboard"
            className={`side-menu-item ${location.pathname === '/dashboard' ? 'active' : ''}`}
          >
            Dashboard
          </Link>
          <Link
            to="/transactions"
            className={`side-menu-item ${
              location.pathname === '/transactions' || location.pathname.startsWith('/transacao')
                ? 'active'
                : ''
            }`}
          >
            Transaction
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
