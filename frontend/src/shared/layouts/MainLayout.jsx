import React, { useState, useRef, useEffect } from 'react';
import { NavLink, Outlet, useNavigate, Link, useLocation } from 'react-router-dom';
import { Sun, Moon, UserCircle, LogIn, UserPlus, LogOut, Building2, ChevronRight, Search, Bell } from 'lucide-react';
import { useAuth } from '../../features/auth';
import apiClient from '../api/client';
import Sidebar from './Sidebar';
import styles from './MainLayout.module.css';

export default function MainLayout() {
  const { isAuthenticated, logout, roles, user } = useAuth();
  const navigate = useNavigate();
  const dropdownRef = useRef(null);
  
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem('theme') || 'light';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [isNotifOpen, setIsNotifOpen] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  
  const notifRef = useRef(null);

  // Close dropdowns when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsDropdownOpen(false);
      }
      if (notifRef.current && !notifRef.current.contains(event.target)) {
        setIsNotifOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);
  
  const handleLogout = async () => {
    await logout();
    setIsDropdownOpen(false);
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

  const location = useLocation();
  const pathnames = location.pathname.split('/').filter(x => x);

  const closeMobileMenu = () => setIsMobileMenuOpen(false);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/listings?search=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (isAuthenticated) {
      fetchNotifications();
    }
  }, [isAuthenticated]);

  const fetchNotifications = async () => {
    try {
      const data = await apiClient('/api/notifications');
      setNotifications(data || []);
      setUnreadCount((data || []).filter(n => !n.read).length);
    } catch (error) {
      console.error("Failed to fetch notifications", error);
    }
  };

  const handleMarkAsRead = async (id, currentReadStatus) => {
    if (currentReadStatus) return; // Already read
    try {
      await apiClient(`/api/notifications/${id}/read`, { method: 'PUT' });
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (error) {
      console.error("Failed to mark as read", error);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await apiClient('/api/notifications/read-all', { method: 'PUT' });
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (error) {
      console.error("Failed to mark all as read", error);
    }
  };

  const formatTimeAgo = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    const diffInSeconds = Math.floor((new Date() - date) / 1000);
    if (diffInSeconds < 60) return 'Just now';
    const diffInMinutes = Math.floor(diffInSeconds / 60);
    if (diffInMinutes < 60) return `${diffInMinutes} min ago`;
    const diffInHours = Math.floor(diffInMinutes / 60);
    if (diffInHours < 24) return `${diffInHours} hour${diffInHours > 1 ? 's' : ''} ago`;
    const diffInDays = Math.floor(diffInHours / 24);
    return `${diffInDays} day${diffInDays > 1 ? 's' : ''} ago`;
  };

  const generateBreadcrumbs = () => {
    if (pathnames.length === 0) return <span className={styles.breadcrumbActive}>Home</span>;
    return (
      <nav className={styles.breadcrumbs} aria-label="breadcrumb">
        <Link to="/" className={styles.breadcrumbLink}>Home</Link>
        {pathnames.map((value, index) => {
          const last = index === pathnames.length - 1;
          const to = `/${pathnames.slice(0, index + 1).join('/')}`;
          const title = value.charAt(0).toUpperCase() + value.slice(1).replace(/-/g, ' ');
          
          return (
            <React.Fragment key={to}>
              <ChevronRight size={14} className={styles.breadcrumbSeparator} />
              {last ? (
                <span className={styles.breadcrumbActive}>{title}</span>
              ) : (
                <Link to={to} className={styles.breadcrumbLink}>{title}</Link>
              )}
            </React.Fragment>
          );
        })}
      </nav>
    );
  };

  const navLinkClassName = ({ isActive }) => 
    isActive ? `${styles.navLink} ${styles.active}` : styles.navLink;

  return (
    <div className={styles.appContainer}>
      <Sidebar isOpen={isMobileMenuOpen} onClose={closeMobileMenu} />

      <div className={styles.mainWrapper}>
        {/* Header */}
        <header className={styles.header}>
          <div className={styles.container}>
            <div className={styles.topbar}>
              <div className={styles.topbarLeft}>
                {/* Mobile Menu Toggle */}
                <button 
                  className={styles.mobileToggle} 
                  onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                  aria-expanded={isMobileMenuOpen}
                  aria-label="Toggle navigation"
                >
                  <span className={`${styles.togglerBar} ${styles.bar1}`}></span>
                  <span className={`${styles.togglerBar} ${styles.bar2}`}></span>
                  <span className={`${styles.togglerBar} ${styles.bar3}`}></span>
                </button>
                
                {/* Brand is visible on mobile only since desktop has sidebar */}
                <Link to="/" className={styles.mobileBrand} onClick={closeMobileMenu}>
                  <Building2 size={24} /> DRAS
                </Link>

                {/* Desktop Breadcrumbs */}
                <div className={styles.desktopBreadcrumbs}>
                  {generateBreadcrumbs()}
                </div>
              </div>

              {/* Center Search Bar */}
              <div className={styles.topbarCenter}>
                <form className={styles.searchContainer} onSubmit={handleSearchSubmit}>
                  <Search className={styles.searchIcon} size={16} />
                  <input 
                    type="text" 
                    placeholder="Search apartments..." 
                    className={styles.searchInput} 
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                  />
                </form>
              </div>

              <div className={styles.navActions}>
                {/* Notification Bell */}
                <div className={styles.dropdown} ref={notifRef}>
                  <button 
                    className={styles.iconBtn} 
                    aria-label="Notifications"
                    onClick={() => setIsNotifOpen(!isNotifOpen)}
                  >
                    <Bell size={20} />
                    {unreadCount > 0 && <span className={styles.notificationDot}></span>}
                  </button>

                  {isNotifOpen && (
                    <div className={styles.notifDropdownMenu}>
                      <div className={styles.notifHeader}>
                        <h6 className={styles.notifTitle}>Notifications</h6>
                        {unreadCount > 0 && (
                          <button className={styles.notifClearBtn} onClick={handleMarkAllAsRead}>
                            Mark all as read
                          </button>
                        )}
                      </div>
                      <div className={styles.notifBody}>
                        {notifications.length === 0 ? (
                          <div className={styles.notifEmpty}>No new notifications</div>
                        ) : (
                          notifications.map(notif => (
                            <div 
                              key={notif.id} 
                              className={`${styles.notifItem} ${!notif.read ? styles.unread : ''}`}
                              onClick={() => handleMarkAsRead(notif.id, notif.read)}
                            >
                              <div className={styles.notifItemTitle}>{notif.title}</div>
                              <div className={styles.notifItemMsg}>{notif.message}</div>
                              <div className={styles.notifItemTime}>{formatTimeAgo(notif.createdAt)}</div>
                            </div>
                          ))
                        )}
                      </div>
                    </div>
                  )}
                </div>

                <button 
                  onClick={toggleTheme} 
                  className={styles.themeToggle} 
                  aria-label="Toggle theme" 
                >
                  {theme === 'light' ? <Moon size={20} /> : <Sun size={20} />}
                </button>

                {/* Account Dropdown */}
                <div className={styles.dropdown} ref={dropdownRef}>
                  <button 
                    className={styles.dropdownToggle}
                    onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                  >
                    <UserCircle size={18} /> Account
                  </button>
                  
                  {isDropdownOpen && (
                    <ul className={styles.dropdownMenu}>
                      {!isAuthenticated ? (
                        <>
                          <li>
                            <Link to="/login" className={styles.dropdownItem} onClick={() => setIsDropdownOpen(false)}>
                              <LogIn size={16} className={styles.iconPrimary} /> Login
                            </Link>
                          </li>
                          <li>
                            <Link to="/register" className={styles.dropdownItem} onClick={() => setIsDropdownOpen(false)}>
                              <UserPlus size={16} className={styles.iconSuccess} /> Register
                            </Link>
                          </li>
                        </>
                      ) : (
                        <>
                          <li>
                            <Link to="/profile" className={styles.dropdownItem} onClick={() => setIsDropdownOpen(false)}>
                              <UserCircle size={16} className={styles.iconInfo} /> My Profile
                            </Link>
                          </li>
                          <li>
                            <button onClick={handleLogout} className={styles.dropdownItem}>
                              <LogOut size={16} className={styles.iconDanger} /> Sign Out
                            </button>
                          </li>
                        </>
                      )}
                    </ul>
                  )}
                </div>
              </div>
            </div>
          </div>
        </header>

      {/* Main Content Area */}
      <main className={styles.mainContent}>
        <Outlet />
      </main>

      {/* Footer */}
      <footer className={styles.footer}>
        <div className={styles.container}>
          <div className={styles.footerGrid}>
            
            <div className={styles.footerCol}>
              <h5 className={styles.footerBrand}>Dynamic Rent Adjustment System</h5>
              <p className={styles.footerText}>
                Helping you find the perfect place to live, with a wide selection of apartments for rent.
                Trusted, secure, and easy to use.
              </p>
            </div>

            <div className={styles.footerCol}>
              <h6 className={styles.footerTitle}>Explore</h6>
              <ul className={styles.footerList}>
                <li><Link to="/" className={styles.footerLink}>Home</Link></li>
                <li><Link to="/listings" className={styles.footerLink}>Browse</Link></li>
                <li><Link to="/about" className={styles.footerLink}>About</Link></li>
                <li><Link to="/contact" className={styles.footerLink}>Contact</Link></li>
              </ul>
            </div>

            <div className={styles.footerCol}>
              <h6 className={styles.footerTitle}>Legal</h6>
              <ul className={styles.footerList}>
                <li><Link to="/privacy" className={styles.footerLink}>Privacy</Link></li>
                <li><Link to="/terms" className={styles.footerLink}>Terms</Link></li>
              </ul>
            </div>

            <div className={styles.footerCol}>
              <h6 className={styles.footerTitle}>Account</h6>
              {isAuthenticated ? (
                <>
                  <p className={styles.footerText}>Signed in as <strong>{user?.username}</strong></p>
                  <p className={styles.footerRoles}>Roles: <span>{roles.join(', ')}</span></p>
                </>
              ) : (
                <p className={styles.footerText}>You are not signed in.</p>
              )}
            </div>

          </div>

          <hr className={styles.footerDivider} />

          <div className={styles.footerBottom}>
            <p className={styles.copyright}>&copy; {new Date().getFullYear()} Dynamic Rent Adjustment System. All rights reserved.</p>
            <div className={styles.socials}>
              <a href="https://facebook.com" target="_blank" rel="noreferrer" className={styles.socialLink}><i className="fab fa-facebook-f fa-lg"></i></a>
              <a href="https://twitter.com" target="_blank" rel="noreferrer" className={styles.socialLink}><i className="fab fa-twitter fa-lg"></i></a>
              <a href="https://linkedin.com/in/athos-exarchou" target="_blank" rel="noreferrer" className={styles.socialLink}><i className="fab fa-linkedin-in fa-lg"></i></a>
              <a href="https://github.com/AthosExarchou" target="_blank" rel="noreferrer" className={styles.socialLink}><i className="fab fa-github fa-lg"></i></a>
            </div>
          </div>
        </div>
      </footer>
      </div>
    </div>
  );
}
