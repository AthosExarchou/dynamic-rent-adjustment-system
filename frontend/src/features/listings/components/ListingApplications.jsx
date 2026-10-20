import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { User, Mail, Phone, Users, CheckCircle, XCircle, ArrowLeft, Home, UserMinus } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import styles from './ListingApplications.module.css';

export default function ListingApplications() {
  const { listingId } = useParams();
  const [listing, setListing] = useState(null);
  const [applicants, setApplicants] = useState([]);
  const [tenant, setTenant] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);
  const isMounted = useRef(true);

  const fetchApplications = useCallback(async (signal) => {
    try {
      const data = await apiClient(`/listings/${listingId}/applications`, signal ? { signal } : {});
      if (!isMounted.current) return;
      setListing(data.listing || data);
      setApplicants(data.applicants && data.applicants.length > 0 ? data.applicants : []);
      setTenant(data.tenant || null);
      setError(null);
    } catch (err) {
      if (!isMounted.current || err.name === 'AbortError') return;
      console.error("Failed to fetch applications:", err);
      setListing(null);
      setApplicants([]);
      setTenant(null);
      setError(err.message || "Failed to load applications.");
    } finally {
      if (isMounted.current) setLoading(false);
    }
  }, [listingId]);

  useEffect(() => {
    isMounted.current = true;
    const controller = new AbortController();
    fetchApplications(controller.signal);
    return () => {
      isMounted.current = false;
      controller.abort();
    };
  }, [fetchApplications]);

  const handleApprove = async (tenantId) => {
    setActionLoading({ id: tenantId, type: 'approve' });
    try {
      await apiClient(`/owner/listings/${listingId}/approveApplicant/${tenantId}`, { method: 'POST' });
      if (!isMounted.current) return;
      alert('Application approved successfully!');
      await fetchApplications();
    } catch (err) {
      if (!isMounted.current) return;
      alert(err.message || 'Failed to approve application.');
    } finally {
      if (isMounted.current) setActionLoading(null);
    }
  };

  const handleReject = async (tenantId) => {
    setActionLoading({ id: tenantId, type: 'reject' });
    try {
      await apiClient(`/owner/listings/${listingId}/rejectApplicant/${tenantId}`, { method: 'POST' });
      if (!isMounted.current) return;
      alert('Application rejected.');
      await fetchApplications();
    } catch (err) {
      if (!isMounted.current) return;
      alert(err.message || 'Failed to reject application.');
    } finally {
      if (isMounted.current) setActionLoading(null);
    }
  };

  const handleUnassignTenant = async () => {
    if (!window.confirm("Are you sure you want to unassign this tenant? The property will become available for rent again.")) return;
    setActionLoading({ id: 'unassign', type: 'unassign' });
    try {
      await apiClient(`/listings/unassign/tenant/${listingId}`, { method: 'POST' });
      if (!isMounted.current) return;
      alert('Tenant unassigned successfully. The property is now available for rent.');
      await fetchApplications();
    } catch (err) {
      if (!isMounted.current) return;
      alert(err.message || 'Failed to unassign tenant.');
    } finally {
      if (isMounted.current) setActionLoading(null);
    }
  };

  if (loading) return (
    <div className={styles.loadingContainer}>
      <div className={styles.spinner}></div>
      <p>Loading applications...</p>
    </div>
  );

  const isRented = listing?.rented || listing?.status === 'RENTED';

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
          <span className={styles.titleText} title={listing?.title ? `Applications for "${listing.title}"` : undefined}>
            Applications for "{listing?.title || 'Property'}"
          </span>
        </h2>
      </div>

      <hr className={styles.divider} />

      {error ? (
        <div className={styles.errorState} style={{ color: 'red', textAlign: 'center', marginBottom: '1rem' }}>
          <p>{error}</p>
        </div>
      ) : (
        <>
          {isRented && (
            <div className={`${styles.activeRentalSection} ${styles.cardHover}`}>
              <div className={styles.activeRentalHeader}>
                <h3 className={styles.activeRentalTitle}>
                  <Home size={20} /> Active Tenancy
                </h3>
                <span className={styles.rentedBadgeActive}>Property Rented</span>
              </div>
              {tenant ? (
                <>
                  <div className={styles.activeRentalGrid}>
                    <div className={styles.activeRentalItem}>
                      <User size={16} /> <span><strong>{tenant.firstName} {tenant.lastName}</strong></span>
                    </div>
                    {tenant.email && (
                      <div className={styles.activeRentalItem}>
                        <Mail size={16} /> <span>{tenant.email}</span>
                      </div>
                    )}
                    {tenant.phoneNumber && (
                      <div className={styles.activeRentalItem}>
                        <Phone size={16} /> <span>{tenant.phoneNumber}</span>
                      </div>
                    )}
                  </div>
                  <button
                    onClick={handleUnassignTenant}
                    disabled={actionLoading?.type === 'unassign'}
                    className={styles.unassignBtn}
                  >
                    <UserMinus size={16} /> {actionLoading?.type === 'unassign' ? 'Unassigning...' : 'Unassign Tenant'}
                  </button>
                </>
              ) : (
                <p style={{ margin: 0, color: 'var(--text-secondary)' }}>
                  This property is currently rented.
                </p>
              )}
            </div>
          )}

          {applicants.length > 0 && (
            <div className={styles.grid}>
              {applicants.map(a => (
                <div key={a.id} className={`${styles.card} ${styles.cardHover}`}>
                  <div className={styles.cardHeader}>
                    <h3 className={styles.applicantName}>
                      <User size={18} className={styles.iconPrimary} />
                      <span>{a.firstName} {a.lastName}</span>
                    </h3>
                  </div>
                  <div className={styles.cardBody}>
                    <p className={styles.contactInfo}>
                      <Mail size={16} className={styles.iconSecondary} />
                      <span>{a.email}</span>
                    </p>
                    <p className={styles.contactInfo}>
                      <Phone size={16} className={styles.iconSecondary} />
                      <span>{a.phoneNumber}</span>
                    </p>
                    
                    {!isRented ? (
                      <div className={styles.actions}>
                        <button 
                          onClick={() => handleApprove(a.id)} 
                          disabled={actionLoading?.id === a.id}
                          className={`${styles.btn} ${styles.btnSuccess}`}
                        >
                          <CheckCircle size={16} /> {actionLoading?.id === a.id && actionLoading?.type === 'approve' ? 'Approving...' : 'Approve'}
                        </button>
                        <button 
                          onClick={() => handleReject(a.id)} 
                          disabled={actionLoading?.id === a.id}
                          className={`${styles.btn} ${styles.btnDanger}`}
                        >
                          <XCircle size={16} /> {actionLoading?.id === a.id && actionLoading?.type === 'reject' ? 'Rejecting...' : 'Reject'}
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
          )}

          {applicants.length === 0 && !isRented && (
            <div className={styles.emptyState}>
              <Users size={48} className={styles.emptyIcon} />
              <p>No applications received for this property yet.</p>
            </div>
          )}
        </>
      )}
    </div>
  );
}
