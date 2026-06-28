import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { MapPin, Building2, Eye, Trash2, Users } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import styles from './MyListings.module.css';

export default function MyListings() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [listingToDelete, setListingToDelete] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    fetchMyListings(controller.signal);
    return () => controller.abort();
  }, []);

  const fetchMyListings = async (signal) => {
    setLoading(true);
    try {
      const data = await apiClient('/listings/mylisting', signal ? { signal } : {});
      setListings(data);
      setError(null);
    } catch (err) {
      if (err.name === 'AbortError') return;
      console.error("Failed to fetch my listings:", err);
      setListings([]);
      setError("Failed to load listings.");
    } finally {
      setLoading(false);
    }
  };

  const confirmDelete = (id) => setListingToDelete(id);

  const handleDelete = async () => {
    if (!listingToDelete) return;
    try {
      await apiClient(`/listings/delete/${listingToDelete}`, { method: 'POST' });
      fetchMyListings();
      setListingToDelete(null);
    } catch {
      alert('Failed to delete listing.');
      setListingToDelete(null);
    }
  };

  const getStatusBadgeClass = (status) => {
    if (status === 'APPROVED') return styles.badgeApproved;
    if (status === 'PENDING') return styles.badgePending;
    if (status === 'DISABLED' || status === 'REJECTED') return styles.badgeDisabled;
    if (status === 'RENTED') return styles.badgeRented;
    return styles.badgeOther;
  };

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h2 className={styles.title}>
          <Building2 className={styles.titleIcon} size={28} /> My Properties
        </h2>
      </div>
      
      <hr className={styles.divider} />

      <div className={styles.grid}>
        {listings.map(l => (
          <div key={l.id} className={`${styles.card} ${styles.cardHover}`}>
            <div className={styles.cardContent}>
              <div className={styles.cardTop}>
                <h3 className={styles.cardTitle}>{l.title}</h3>
                <span className={`${styles.badge} ${getStatusBadgeClass(l.status)}`}>
                  {l.status}
                </span>
              </div>
              
              <div className={styles.apartmentInfo}>
                <p className={styles.address}>
                  <MapPin size={16} /> {l.address}
                </p>
                <p className={styles.priceInfo}>
                  <span className={styles.priceBadge}>{l.price} € /mo</span>
                </p>
              </div>
              
              <div className={styles.cardFooter}>
                <Link to={`/listings/${l.id}`} className={`${styles.btn} ${styles.btnOutlineSecondary}`}>
                  <Eye size={16} /> View
                </Link>
                
                <Link to={`/my-listings/${l.id}/apps`} className={`${styles.btn} ${styles.btnPrimary}`}>
                  <Users size={16} /> Applications
                </Link>

                <button onClick={() => confirmDelete(l.id)} className={`${styles.btn} ${styles.btnDanger}`}>
                  <Trash2 size={16} /> Delete
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
      
      {loading ? (
        <div className={styles.emptyState}>
          <p>Loading your properties...</p>
        </div>
      ) : error ? (
        <div className={styles.emptyState}>
          <p style={{color: 'red'}}>{error}</p>
        </div>
      ) : listings.length === 0 && (
        <div className={styles.emptyState}>
          <p>You have not listed any properties yet.</p>
          <Link to="/listings/new" className={`${styles.btn} ${styles.btnSuccess}`}>Create New Apartment</Link>
        </div>
      )}

      {listingToDelete && (
        <div style={{position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)',
          display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000}}>
          <div style={{backgroundColor: 'white', padding: '2rem', borderRadius: '8px', maxWidth: '400px', color: '#333'}}>
            <h3 style={{marginTop: 0}}>Confirm Deletion</h3>
            <p>Are you sure you want to delete this listing? This action cannot be undone.</p>
            <div style={{display: 'flex', gap: '1rem', marginTop: '1.5rem', justifyContent: 'flex-end'}}>
              <button onClick={() => setListingToDelete(null)} className={styles.btn}>Cancel</button>
              <button onClick={handleDelete} className={`${styles.btn} ${styles.btnDanger}`}>Confirm Delete</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
