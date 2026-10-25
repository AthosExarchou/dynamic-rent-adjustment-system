import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { MapPin, ClipboardCheck, Building2, Eye, Camera } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import { LISTING_CONSTRAINTS } from '../../listings/models/listing';
import styles from './AdminDashboard.module.css';
import modalStyles from './AdminModals.module.css';

export default function PendingApprovals() {
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);
  const [rejectTarget, setRejectTarget] = useState(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [rejectionError, setRejectionError] = useState('');

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
    setActionLoading({ id, type: 'approve' });
    try {
      await apiClient(`/listings/approve/${id}`, { method: 'POST' });
      alert('Listing approved successfully.');
      await fetchPendingListings();
    } catch (err) {
      alert(err.message || 'Failed to approve listing.');
    } finally {
      setActionLoading(null);
    }
  };

  const openRejectModal = (listing) => {
    setRejectTarget(listing);
    setRejectionReason('');
    setRejectionError('');
  };

  const closeRejectModal = () => {
    setRejectTarget(null);
    setRejectionReason('');
    setRejectionError('');
  };

  const handleReasonChange = (e) => {
    let value = e.target.value;
    if (value.length > LISTING_CONSTRAINTS.REJECTION_REASON_MAX) {
      value = value.slice(0, LISTING_CONSTRAINTS.REJECTION_REASON_MAX);
    }
    setRejectionReason(value);
    if (rejectionError) setRejectionError('');
  };

  const handleConfirmReject = async (e) => {
    e.preventDefault();
    const trimmed = rejectionReason.trim();
    if (!trimmed) {
      setRejectionError('Rejection reason is required.');
      return;
    }
    if (trimmed.length > LISTING_CONSTRAINTS.REJECTION_REASON_MAX) {
      setRejectionError(`Rejection reason cannot exceed ${LISTING_CONSTRAINTS.REJECTION_REASON_MAX} characters.`);
      return;
    }

    setActionLoading({ id: rejectTarget.id, type: 'reject' });
    setRejectionError('');
    try {
      await apiClient(`/listings/reject/${rejectTarget.id}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason: trimmed })
      });
      alert('Listing rejected.');
      closeRejectModal();
      await fetchPendingListings();
    } catch (err) {
      setRejectionError(err.message || 'Failed to reject listing.');
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h2 className={styles.title}>
          <ClipboardCheck className={styles.titleIcon} size={28} />
          Pending Listing Approvals
        </h2>
      </div>

      <hr className={styles.divider} />

      <div className={styles.grid}>
        {listings.map(l => (
          <div key={l.id} className={styles.card}>
            <div className={styles.imageWrapper}>
              <Link to={`/listings/${l.id}`} target="_blank" rel="noopener noreferrer" className={styles.imageLink} aria-label={`View details for ${l.title}`}>
                {l.images && l.images.length > 0 ? (
                  <img
                    src={l.images[0]}
                    alt={l.title}
                    className={styles.image}
                    onError={(e) => {
                      e.target.style.display = 'none';
                      if (e.target.nextSibling) {
                        e.target.nextSibling.style.display = 'flex';
                      }
                    }}
                  />
                ) : null}
                <div
                  className={styles.noImage}
                  style={{ display: l.images && l.images.length > 0 ? 'none' : 'flex' }}
                >
                  <Building2 size={36} />
                  <span>No image available</span>
                </div>
              </Link>
              {l.images && l.images.length > 1 && (
                <span className={styles.imageCountBadge} title={`${l.images.length} photos available`}>
                  <Camera size={13} /> {l.images.length}
                </span>
              )}
            </div>
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
                <button
                  onClick={() => handleApprove(l.id)}
                  disabled={actionLoading?.id === l.id}
                  className={styles.approveBtn}
                >
                  {actionLoading?.id === l.id && actionLoading?.type === 'approve' ? 'Approving...' : 'Approve Listing'}
                </button>
                <button
                  onClick={() => openRejectModal(l)}
                  disabled={actionLoading?.id === l.id}
                  className={styles.rejectBtn}
                >
                  Reject Listing
                </button>
                <Link
                  to={`/listings/${l.id}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={styles.viewBtn}
                >
                  <Eye size={16} /> View Details
                </Link>
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

      {rejectTarget && (
        <div className={modalStyles.modalOverlay}>
          <div className={modalStyles.modalCard}>
            <div className={modalStyles.modalHeader}>
              <h3>Reject Listing</h3>
              <button onClick={closeRejectModal} className={modalStyles.closeBtn} disabled={actionLoading?.id === rejectTarget.id}>&times;</button>
            </div>
            <div className={modalStyles.modalBody}>
              {rejectionError && <div className={modalStyles.error}>{rejectionError}</div>}
              <p className={modalStyles.instruction}>
                Please specify the reason for rejecting <strong>"{rejectTarget.title}"</strong>. The owner will see this feedback so they can correct and resubmit their listing.
              </p>
              <form onSubmit={handleConfirmReject}>
                <div className={modalStyles.formGroup}>
                  <label htmlFor="rejectionReasonInput" className={modalStyles.label}>
                    Reason for Rejection <span style={{ color: 'var(--color-danger, #ef4444)' }}>*</span>
                    <span style={{ fontWeight: 400, color: 'var(--text-secondary)', marginLeft: '0.5rem', fontSize: '0.8rem' }}>
                      (max {LISTING_CONSTRAINTS.REJECTION_REASON_MAX} characters)
                    </span>
                  </label>
                  <textarea
                    id="rejectionReasonInput"
                    className={modalStyles.input}
                    rows={4}
                    required
                    maxLength={LISTING_CONSTRAINTS.REJECTION_REASON_MAX}
                    placeholder="e.g. Please provide photos of all bedrooms and verify that the listed square footage matches official property documents."
                    value={rejectionReason}
                    onChange={handleReasonChange}
                    style={{ resize: 'vertical' }}
                    autoFocus
                  />
                  <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    fontSize: '0.8rem',
                    color: rejectionReason.length >= LISTING_CONSTRAINTS.REJECTION_REASON_MAX ? 'var(--color-danger, #ef4444)' : 'var(--text-secondary)',
                    marginTop: '0.25rem'
                  }}>
                    <span>
                      {rejectionReason.length >= LISTING_CONSTRAINTS.REJECTION_REASON_MAX
                        ? 'Maximum limit reached (500 characters)'
                        : `${LISTING_CONSTRAINTS.REJECTION_REASON_MAX - rejectionReason.length} characters remaining`}
                    </span>
                    <span>
                      {rejectionReason.length} / {LISTING_CONSTRAINTS.REJECTION_REASON_MAX}
                    </span>
                  </div>
                </div>
                <div className={modalStyles.modalFooter}>
                  <button
                    type="button"
                    onClick={closeRejectModal}
                    className={modalStyles.cancelBtn}
                    disabled={actionLoading?.id === rejectTarget.id}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className={modalStyles.dangerBtn}
                    disabled={actionLoading?.id === rejectTarget.id || !rejectionReason.trim() || rejectionReason.length > LISTING_CONSTRAINTS.REJECTION_REASON_MAX}
                  >
                    {actionLoading?.id === rejectTarget.id && actionLoading?.type === 'reject' ? 'Rejecting...' : 'Confirm Rejection'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
