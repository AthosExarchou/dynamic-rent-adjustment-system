import React, { useState } from 'react';
import { NavLink, Outlet, useNavigate, Link } from 'react-router-dom';
import { Sun, Moon } from 'lucide-react';
import { useAuth } from '../../features/auth';
import styles from './MainLayout.module.css';

export default function MainLayout() {
  const { isAuthenticated, logout, roles, user } = useAuth();
  const navigate = useNavigate();
  
  // Theme state initialized from localStorage with SSR fallback
  const [theme, setTheme] = useState(() => {
    const savedTheme = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);
    return savedTheme;
  });
  
  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  const toggleTheme = () => {
    const nextTheme = theme === 'light' ? 'dark' : 'light';
    setTheme(nextTheme);
    localStorage.setItem('theme', nextTheme);
    document.documentElement.setAttribute('data-theme', nextTheme);
  };

  const isAdmin = roles.includes('ADMIN');
  const isOwner = roles.includes('OWNER');
  const isUser = isAuthenticated;

  // Helper for NavLink classnames
  const navLinkClassName = ({ isActive }) => 
    isActive ? `${styles.navLink} ${styles.active}` : styles.navLink;

  return (
    <div className={styles.layout}>
      {/* Header / Navbar */}
      <nav className={styles.navbar}>
        <div className={styles.navContainer}>
          {/* Logo */}
          <Link to="/" className={styles.brand}>
            DRAS Platform
          </Link>

          {/* Links */}
          <div className={styles.navLinks}>
            <NavLink to="/" className={navLinkClassName} end>Home</NavLink>
            <NavLink to="/listings" className={navLinkClassName}>Listings</NavLink>
            <NavLink to="/about" className={navLinkClassName}>About</NavLink>
            <NavLink to="/contact" className={navLinkClassName}>Contact</NavLink>

            {/* Role Specific Links */}
            {isUser && <NavLink to="/profile" className={navLinkClassName}>My Profile</NavLink>}
            {isOwner && <NavLink to="/my-listings" className={navLinkClassName}>My Listings</NavLink>}
            {isAdmin && (
              <>
                <NavLink to="/admin/approvals" className={navLinkClassName}>Approvals</NavLink>
                <NavLink to="/admin/users" className={navLinkClassName}>Users</NavLink>
              </>
            )}
          </div>

          {/* Right Actions */}
          <div className={styles.actions}>
            {/* Theme Toggle */}
            <button 
              onClick={toggleTheme} 
              className={styles.themeToggle} 
              aria-label="Toggle theme" 
              title="Toggle theme"
            >
              {theme === 'light' ? <Moon size={18} /> : <Sun size={18} />}
            </button>

            {isAuthenticated ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <span className={styles.userWelcome}>
                  Hi, {user?.username}
                </span>
                <button onClick={handleLogout} className={styles.logoutBtn}>
                  Logout
                </button>
              </div>
            ) : (
              <div style={{ display: 'flex', gap: '0.75rem' }}>
                <Link to="/login" className={styles.loginBtn}>Login</Link>
                <Link to="/register" className={styles.registerBtn}>Register</Link>
              </div>
            )}
          </div>
        </div>
      </nav>

      {/* Main Content Area */}
      <main className={styles.mainContent}>
        <Outlet />
      </main>

      {/* Footer */}
      <footer className={styles.footer}>
        <div className={styles.footerLinks}>
          <Link to="/privacy" className={styles.footerLink}>Privacy Policy</Link> |{' '}
          <Link to="/terms" className={styles.footerLink}>Terms of Service</Link>
        </div>
        <div>
          &copy; {new Date().getFullYear()} Dynamic Rent Adjustment System (DRAS). All rights reserved.
        </div>
      </footer>
    </div>
  );
}
