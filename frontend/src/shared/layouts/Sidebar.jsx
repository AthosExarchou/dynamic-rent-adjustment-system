import React from 'react';
import { NavLink } from 'react-router-dom';
import { Home, Building2, Key, CheckSquare, Users, Info, MessageSquare, X } from 'lucide-react';
import { useAuth } from '../../features/auth';
import styles from './Sidebar.module.css';

export default function Sidebar({ isOpen, onClose }) {
  const { isAuthenticated, roles } = useAuth();
  
  const isAdmin = roles.includes('ADMIN');
  const isOwner = roles.includes('OWNER');

  return (
    <>
      {/* Mobile overlay */}
      {isOpen && <div className={styles.mobileOverlay} onClick={onClose}></div>}
      
      <aside className={`${styles.sidebar} ${isOpen ? styles.open : ''}`}>
        <div className={styles.sidebarHeader}>
          <div className={styles.sidebarLogo}>DRAS</div>
          <button className={styles.closeBtn} onClick={onClose} aria-label="Close menu">
            <X size={24} />
          </button>
        </div>

        <nav className={styles.sidebarNav}>
          <NavLink to="/" className={({ isActive }) =>
              isActive ? `${styles.sidebarLink} ${styles.active}` : styles.sidebarLink} onClick={onClose} end>
            <Home size={20} />
            Home
          </NavLink>

          <NavLink to="/listings" className={({ isActive }) =>
              isActive ? `${styles.sidebarLink} ${styles.active}` : styles.sidebarLink} onClick={onClose}>
            <Building2 size={20} />
            Apartments
          </NavLink>

          {isOwner && (
            <NavLink to="/my-listings" className={({ isActive }) =>
                isActive ? `${styles.sidebarLink} ${styles.active}` : styles.sidebarLink} onClick={onClose}>
              <Key size={20} />
              My Apartments
            </NavLink>
          )}

          {isAdmin && (
            <>
              <NavLink to="/admin/approvals" className={({ isActive }) =>
                  isActive ? `${styles.sidebarLink} ${styles.active}` : styles.sidebarLink} onClick={onClose}>
                <CheckSquare size={20} />
                Approval Queue
              </NavLink>

              <NavLink to="/admin/users" className={({ isActive }) =>
                  isActive ? `${styles.sidebarLink} ${styles.active}` : styles.sidebarLink} onClick={onClose}>
                <Users size={20} />
                Users
              </NavLink>
            </>
          )}

          <div className={styles.divider}></div>

          <NavLink to="/about" className={({ isActive }) =>
              isActive ? `${styles.sidebarLink} ${styles.active}` : styles.sidebarLink} onClick={onClose}>
            <Info size={20} />
            About Us
          </NavLink>

          <NavLink to="/contact" className={({ isActive }) =>
              isActive ? `${styles.sidebarLink} ${styles.active}` : styles.sidebarLink} onClick={onClose}>
            <MessageSquare size={20} />
            Contact Us
          </NavLink>
        </nav>

        <div className={styles.sidebarFooter}>
          Version 1.0
        </div>
      </aside>
    </>
  );
}
