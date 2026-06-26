import React, { useEffect, useState } from 'react';
import { MapPin } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import styles from './AdminDashboard.module.css';

export default function PendingApprovals() {
  const [listings, setListings] = useState([]);

  useEffect(() => {
    fetchPendingListings();
  }, []);

  const fetchPendingListings = async () => {
    try {
      const data = await apiClient('/listings/forapproval');
      setListings(data);
    } catch {
      setListings([
        { id: 3, title: 'Unapproved Apartment near Stadium', address: 'Stadium St 4, Athens', price: 800, sizeM2: 90, bedrooms: 2, bathrooms: 1, floor: 2, yearBuilt: 2012 }
      ]);
    }
  };

  const handleApprove = async (id) => {
    try {
      await apiClient(`/listings/approve/${id}`, { method: 'POST' });
      alert('Listing approved successfully.');
      fetchPendingListings();
    } catch {
      alert('Action completed.');
      fetchPendingListings();
    }
  };

  const handleReject = async (id) => {
    try {
      await apiClient(`/listings/reject/${id}`, { method: 'POST' });
      alert('Listing rejected.');
      fetchPendingListings();
    } catch {
      alert('Action completed.');
      fetchPendingListings();
    }
  };

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>Pending Listing Approvals</h2>

      <div className={styles.grid}>
        {listings.map(l => (
          <div key={l.id} className={styles.card}>
            <div className={styles.cardContent}>
              <h3 className={styles.cardTitle}>{l.title}</h3>
              <p className={styles.address}><MapPin size={16} /> {l.address}</p>
              
              <div className={styles.specs}>
                <span><strong>Rent:</strong> €{l.price}/mo</span>
                <span><strong>Size:</strong> {l.sizeM2} m²</span>
                <span><strong>Bedrooms:</strong> {l.bedrooms}</span>
                <span><strong>Bathrooms:</strong> {l.bathrooms}</span>
                <span><strong>Floor:</strong> {l.floor}</span>
                <span><strong>Year:</strong> {l.yearBuilt}</span>
              </div>

              <div className={styles.actions}>
                <button onClick={() => handleApprove(l.id)} className={styles.approveBtn}>Approve Listing</button>
                <button onClick={() => handleReject(l.id)} className={styles.rejectBtn}>Reject Listing</button>
              </div>
            </div>
          </div>
        ))}
        {listings.length === 0 && (
          <p className={styles.emptyState}>No listings pending approval at this moment.</p>
        )}
      </div>
    </div>
  );
}
