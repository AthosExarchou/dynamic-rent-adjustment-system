import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { MapPin, Building2, Eye, Trash2, Users } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import styles from './MyListings.module.css';

export default function MyListings() {
  const [listings, setListings] = useState([]);

  useEffect(() => {
    fetchMyListings();
  }, []);

  const fetchMyListings = async () => {
    try {
      const data = await apiClient('/listings/mylisting');
      if (data.length > 0) {
        setListings(data);
      } else {
        setListings(getMockListings());
      }
    } catch (err) {
      console.error("Failed to fetch my listings:", err);
      setListings(getMockListings());
    }
  };

  const getMockListings = () => [
    { id: 1, title: 'Luxury Penthouse in Athens Center', address: 'Panepistimiou 15, Athens', price: 1200, status: 'APPROVED', sizeM2: 120, rented: false },
    { id: 3, title: 'Unapproved Apartment near Stadium', address: 'Stadium St 4, Athens', price: 800, status: 'PENDING', sizeM2: 90, rented: false }
  ];

  const handleDelete = async (id) => {
    if (!confirm('Are you sure you want to delete this listing?')) return;
    try {
      await apiClient(`/listings/delete/${id}`, { method: 'POST' });
      fetchMyListings();
    } catch {
      alert('Failed to delete listing.');
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

                <button onClick={() => handleDelete(l.id)} className={`${styles.btn} ${styles.btnDanger}`}>
                  <Trash2 size={16} /> Delete
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
      
      {listings.length === 0 && (
        <div className={styles.emptyState}>
          <p>You have not listed any properties yet.</p>
          <Link to="/listings/new" className={`${styles.btn} ${styles.btnSuccess}`}>Create New Apartment</Link>
        </div>
      )}
    </div>
  );
}
