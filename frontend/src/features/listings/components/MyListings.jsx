import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { MapPin } from 'lucide-react';
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
      setListings(data);
    } catch {
      // Mocks
      setListings([
        { id: 1, title: 'Luxury Penthouse in Athens Center', address: 'Panepistimiou 15, Athens', price: 1200, status: 'APPROVED', sizeM2: 120 },
        { id: 3, title: 'Unapproved Apartment near Stadium', address: 'Stadium St 4, Athens', price: 800, status: 'PENDING', sizeM2: 90 }
      ]);
    }
  };

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
    if (status === 'DISABLED') return styles.badgeDisabled;
    return styles.badgeOther;
  };

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>My Properties</h2>
      
      <div className={styles.grid}>
        {listings.map(l => (
          <div key={l.id} className={styles.card}>
            <div className={styles.cardContent}>
              <div className={styles.cardHeader}>
                <h3 className={styles.cardTitle}>{l.title}</h3>
                <span className={`${styles.badge} ${getStatusBadgeClass(l.status)}`}>{l.status}</span>
              </div>
              <p className={styles.address}>
                <MapPin size={16} /> {l.address}
              </p>
              
              <div className={styles.actions}>
                <Link to={`/my-listings/${l.id}/apps`} className={styles.appBtn}>Applications</Link>
                <button onClick={() => handleDelete(l.id)} className={styles.deleteBtn}>Delete</button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
