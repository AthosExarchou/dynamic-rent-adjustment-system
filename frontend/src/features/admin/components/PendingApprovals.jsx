import React, { useEffect, useState } from 'react';
import { MapPin } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import styles from './AdminDashboard.module.css';

export default function PendingApprovals() {
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    fetchPendingListings(controller.signal);
    return () => controller.abort();
  }, []);

  const fetchPendingListings = async (signal) => {
    setLoading(true);
    try {
      const data = await apiClient('/listings/forapproval', signal ? { signal } : {});
      setListings(data);
      setError(null);
    } catch (err) {
      if (err.name === 'AbortError') return;
      console.error("Failed to fetch pending approvals:", err);
      setListings([]);
      setError("Failed to load pending approvals.");
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (id) => {
    setActionLoading(id);
    try {
      await apiClient(`/listings/approve/${id}`, { method: 'POST' });
      alert('Listing approved successfully.');
      await fetchPendingListings();
    } catch {
      alert('Failed to approve listing.');
    } finally {
      setActionLoading(null);
    }
  };

  const handleReject = async (id) => {
    setActionLoading(id);
    try {
      await apiClient(`/listings/reject/${id}`, { method: 'POST' });
      alert('Listing rejected.');
      await fetchPendingListings();
    } catch {
      alert('Failed to reject listing.');
    } finally {
      setActionLoading(null);
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
                <button onClick={() => handleApprove(l.id)} disabled={actionLoading === l.id} className={styles.approveBtn}>
                  {actionLoading === l.id ? 'Approving...' : 'Approve Listing'}
                </button>
                <button onClick={() => handleReject(l.id)} disabled={actionLoading === l.id} className={styles.rejectBtn}>
                  {actionLoading === l.id ? 'Rejecting...' : 'Reject Listing'}
                </button>
              </div>
            </div>
          </div>
        ))}
        {loading ? (
          <p className={styles.emptyState}>Loading pending approvals...</p>
        ) : error ? (
          <p className={styles.emptyState} style={{color: 'red'}}>{error}</p>
        ) : listings.length === 0 && (
          <p className={styles.emptyState}>No listings pending approval at this moment.</p>
        )}
      </div>
    </div>
  );
}
