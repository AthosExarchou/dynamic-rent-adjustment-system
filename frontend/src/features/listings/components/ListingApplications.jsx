import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { User, Mail, Phone, Users, CheckCircle, XCircle, ArrowLeft } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import styles from './ListingApplications.module.css';

export default function ListingApplications() {
  const { listingId } = useParams();
  const [listing, setListing] = useState(null);
  const [applicants, setApplicants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const isMounted = React.useRef(true);

  useEffect(() => {
    isMounted.current = true;
    const controller = new AbortController();
    fetchApplications(controller.signal);
    return () => {
      isMounted.current = false;
      controller.abort();
    };
  }, [fetchApplications]);

  const fetchApplications = useCallback(async (signal) => {
    try {
      const data = await apiClient(`/listings/${listingId}/applications`, signal ? { signal } : {});
      if (!isMounted.current) return;
      setListing(data);
      if (data.applicants && data.applicants.length > 0) {
        setApplicants(data.applicants);
      } else {
        setApplicants([]);
      }
      setError(null);
    } catch (err) {
      if (!isMounted.current || err.name === 'AbortError') return;
      console.error("Failed to fetch applications:", err);
      setListing(null);
      setApplicants([]);
      setError("Failed to load applications.");
    } finally {
      if (isMounted.current) setLoading(false);
    }
  }, [listingId]);

  const handleApprove = async (tenantId) => {
    try {
      await apiClient(`/owner/listings/${listingId}/approveApplicant/${tenantId}`, { method: 'POST' });
      if (!isMounted.current) return;
      alert('Application approved successfully!');
      fetchApplications();
    } catch (err) {
      if (!isMounted.current) return;
      alert('Failed to approve application.');
    }
  };

  const handleReject = async (tenantId) => {
    try {
      await apiClient(`/owner/listings/${listingId}/rejectApplicant/${tenantId}`, { method: 'POST' });
      if (!isMounted.current) return;
      alert('Application rejected.');
      fetchApplications();
    } catch (err) {
      if (!isMounted.current) return;
      alert('Failed to reject application.');
    }
  };

  if (loading) return (
    <div className={styles.loadingContainer}>
      <div className={styles.spinner}></div>
      <p>Loading applications...</p>
    </div>
  );

  return (
    <div className={styles.container}>
      <div className={styles.topActions}>
        <Link to="/my-listings" className={styles.backLink}>
          <ArrowLeft size={18} /> Back to My Properties
        </Link>
      </div>

      <div className={styles.header}>
        <h2 className={styles.title}>
          <Users className={styles.titleIcon} size={28} />
          Applications for "{listing?.title || 'Property'}"
        </h2>
      </div>

      <hr className={styles.divider} />

      {error && (
        <div className={styles.errorState} style={{ color: 'red', textAlign: 'center', marginBottom: '1rem' }}>
          <p>{error}</p>
        </div>
      )}

      <div className={styles.grid}>
        {applicants.map(a => (
          <div key={a.id} className={`${styles.card} ${styles.cardHover}`}>
            <div className={styles.cardHeader}>
              <h3 className={styles.applicantName}>
                <User size={18} className={styles.iconPrimary} /> {a.firstName} {a.lastName}
              </h3>
            </div>
            <div className={styles.cardBody}>
              <p className={styles.contactInfo}>
                <Mail size={16} className={styles.iconSecondary} /> {a.email}
              </p>
              <p className={styles.contactInfo}>
                <Phone size={16} className={styles.iconSecondary} /> {a.phoneNumber}
              </p>
              
              {!listing?.rented ? (
                <div className={styles.actions}>
                  <button onClick={() => handleApprove(a.id)} className={`${styles.btn} ${styles.btnSuccess}`}>
                    <CheckCircle size={16} /> Approve
                  </button>
                  <button onClick={() => handleReject(a.id)} className={`${styles.btn} ${styles.btnDanger}`}>
                    <XCircle size={16} /> Reject
                  </button>
                </div>
              ) : (
                <div className={styles.rentedNotice}>
                  <span className={styles.rentedBadge}>Property Rented</span>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
      
      {applicants.length === 0 && (
        <div className={styles.emptyState}>
          <Users size={48} className={styles.emptyIcon} />
          <p>No applications received for this property yet.</p>
        </div>
      )}
    </div>
  );
}
