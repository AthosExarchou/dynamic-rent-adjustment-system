import React, { useState, useEffect } from 'react';
import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { Home, Building2, Key, CheckSquare, Users, Info, MessageSquare, X, Search, Heart } from 'lucide-react';
import { useAuth } from '../../features/auth';
import { useFavourites } from '../../features/favourites';
import styles from './Sidebar.module.css';

export default function Sidebar({ isOpen, onClose }) {
  const { roles } = useAuth();
  const { favouriteCount, guestFavouritesPending } = useFavourites();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileSearch, setMobileSearch] = useState('');

  const isAdmin = roles.includes('ADMIN');
  const isOwner = roles.includes('OWNER');

  useEffect(() => {
    if (location.pathname === '/listings') {
      const params = new URLSearchParams(location.search);
      setMobileSearch(params.get('search') || params.get('title') || '');
    } else {
      setMobileSearch('');
    }
  }, [location.pathname, location.search]);

  const handleMobileSearch = (e) => {
    e.preventDefault();
    onClose();
    const query = mobileSearch.trim();
    if (query) {
      navigate(`/listings?search=${encodeURIComponent(query)}`);
    } else {
      navigate('/listings');
    }
  };

  const handleMobileClear = () => {
    setMobileSearch('');
    onClose();
    if (location.pathname === '/listings') {
      navigate('/listings');
    }
  };

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
          {/* Mobile Search Bar */}
          <form className={styles.mobileSearchForm} onSubmit={handleMobileSearch} role="search">
            <div className={styles.mobileSearchContainer}>
              <Search size={16} className={styles.mobileSearchIcon} />
              <input
                type="text"
                placeholder="Search listings..."
                value={mobileSearch}
                onChange={(e) => setMobileSearch(e.target.value)}
                className={styles.mobileSearchInput}
              />
              {mobileSearch && (
                <button
                  type="button"
                  onClick={handleMobileClear}
                  className={styles.mobileSearchClearBtn}
                  aria-label="Clear search"
                >
                  <X size={14} />
                </button>
              )}
            </div>
          </form>

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

          <NavLink to="/favourites" className={({ isActive }) =>
              isActive ? `${styles.sidebarLink} ${styles.active}` : styles.sidebarLink} onClick={onClose}>
            <Heart size={20} />
            Favourites
            {favouriteCount > 0 && <span className={styles.badge}>{favouriteCount}</span>}
            {favouriteCount === 0 && guestFavouritesPending > 0 && (
              <span className={styles.pendingBadge}>{guestFavouritesPending}</span>
            )}
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
