import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { MapPin, Building2, Eye, Pencil, Trash2, Users, Power, PowerOff, AlertCircle, Lock, Camera } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import { useAuth } from '../../auth';
import { getImageUrl, DEFAULT_LISTING_IMAGE, REMOTE_FALLBACK_IMAGE } from '../../../shared/utils/imageUtils';
import styles from './MyListings.module.css';

const truncateTitle = (title, maxLength = 35) => {
  if (!title) return 'this property';
  const trimmed = title.trim();
  if (trimmed.length <= maxLength) return trimmed;
  return `${trimmed.substring(0, maxLength)}...`;
};

export default function MyListings() {
  const { roles } = useAuth();
  const [listings, setListings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [listingToDelete, setListingToDelete] = useState(null);
  const [listingToToggle, setListingToToggle] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);

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

  const confirmDelete = (id, title) => setListingToDelete({ id, title });

  const handleDelete = async () => {
    if (!listingToDelete) return;
    try {
      await apiClient(`/listings/delete/${listingToDelete.id}`, { method: 'POST' });
      fetchMyListings();
      setListingToDelete(null);
    } catch (err) {
      alert(err.message || 'Failed to delete listing.');
      setListingToDelete(null);
    }
  };

  const confirmToggle = (id, title, action) => {
    setListingToToggle({ id, title, action });
  };

  const handleToggle = async () => {
    if (!listingToToggle) return;
    const { id, action } = listingToToggle;
    setActionLoading(id);
    try {
      await apiClient(`/listings/${action}/${id}`, { method: 'POST' });
      await fetchMyListings();
      setListingToToggle(null);
    } catch (err) {
      alert(err.message || `Failed to ${action} listing.`);
      setListingToToggle(null);
    } finally {
      setActionLoading(null);
    }
  };

  const getStatusBadgeClass = (status) => {
    if (status === 'APPROVED') return styles.badgeApproved;
    if (status === 'PENDING') return styles.badgePending;
    if (status === 'REJECTED') return styles.badgeRejected;
    if (status === 'DISABLED') return styles.badgeDisabled;
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
        {listings.map(l => {
          const isRented = Boolean(l.rented || l.status === 'RENTED');
          const hasImages = l.images && l.images.length > 0;
          const cardImageUrl = hasImages ? (getImageUrl(l.images[0]) || DEFAULT_LISTING_IMAGE) : DEFAULT_LISTING_IMAGE;

          return (
            <div key={l.id} className={`${styles.card} ${styles.cardHover}`}>
              <div className={styles.imageWrapper}>
                <Link to={`/listings/${l.id}`} state={{ from: '/my-listings' }} className={styles.imageLink} aria-label={`View ${l.title}`}>
                  <img
                    src={cardImageUrl}
                    alt={hasImages ? l.title : 'Generic demo preview'}
                    className={styles.image}
                    loading="lazy"
                    decoding="async"
                    onError={(e) => {
                      if (e.target.src.includes('demo/image1.jpg')) {
                        e.target.src = REMOTE_FALLBACK_IMAGE;
                      } else if (e.target.src !== DEFAULT_LISTING_IMAGE && e.target.src !== REMOTE_FALLBACK_IMAGE) {
                        e.target.src = DEFAULT_LISTING_IMAGE;
                      }
                    }}
                  />
                </Link>
                <span className={`${styles.imageStatusBadge} ${getStatusBadgeClass(l.status)}`}>
                  {l.status}
                </span>
                {hasImages ? (
                  l.images.length > 1 && (
                    <span className={styles.imageCountBadge} title={`${l.images.length} photos available`}>
                      <Camera size={13} /> {l.images.length}
                    </span>
                  )
                ) : (
                  <span className={styles.demoBadge} title="Demo image - no photos uploaded">
                    <Camera size={12} /> Demo Preview
                  </span>
                )}
              </div>
              <div className={styles.cardContent}>
                <div className={styles.cardTop}>
                  <h3 className={styles.cardTitle}>{l.title}</h3>
                </div>

                {l.subtitle && <p className={styles.subtitle}>{l.subtitle}</p>}
                
                <div className={styles.apartmentInfo}>
                  <p className={styles.address}>
                    <MapPin size={16} /> <span>{l.address}</span>
                  </p>
                  <p className={styles.priceInfo}>
                    <span className={styles.priceBadge}>{l.price} € /mo</span>
                  </p>
                </div>

                {l.status === 'REJECTED' && l.rejectionReason && (
                  <div className={styles.rejectionNotice}>
                    <AlertCircle size={16} className={styles.rejectionIcon} />
                    <div>
                      <strong>Rejection Reason:</strong>
                      <p>{l.rejectionReason}</p>
                    </div>
                  </div>
                )}

                {isRented && (
                  <div className={styles.rentedNotice}>
                    <Lock size={16} className={styles.rentedNoticeIcon} />
                    <div>
                      <strong>Active Lease</strong>
                      <p>This property is currently rented. Editing and deletion are locked during the active lease.</p>
                    </div>
                  </div>
                )}
                
                <div className={styles.cardFooter}>
                  <Link to={`/listings/${l.id}`} state={{ from: '/my-listings' }} className={`${styles.btn} ${styles.btnOutlineSecondary}`}>
                    <Eye size={16} /> View
                  </Link>

                  {isRented ? (
                    <button
                      className={`${styles.btn} ${styles.btnDisabledAction}`}
                      disabled
                      title="This property has an active rental agreement and cannot be edited"
                    >
                      <Lock size={14} /> Edit
                    </button>
                  ) : (
                    <Link to={`/listings/edit/${l.id}`} className={`${styles.btn} ${styles.btnEdit}`} title="Edit property details">
                      <Pencil size={16} /> Edit
                    </Link>
                  )}
                  
                  <Link to={`/my-listings/${l.id}/apps`} className={`${styles.btn} ${styles.btnPrimary}`}>
                    <Users size={16} /> Applications
                  </Link>

                  {l.status === 'APPROVED' && (
                    <button
                      onClick={() => confirmToggle(l.id, l.title, 'disable')}
                      className={`${styles.btn} ${styles.btnDisable}`}
                      disabled={actionLoading === l.id}
                      title="Pause listing (hide from public search)"
                    >
                      <PowerOff size={16} /> Disable
                    </button>
                  )}

                  {l.status === 'DISABLED' && (
                    <button
                      onClick={() => confirmToggle(l.id, l.title, 'enable')}
                      className={`${styles.btn} ${styles.btnEnable}`}
                      disabled={actionLoading === l.id}
                      title="Re-enable listing (publish to public search)"
                    >
                      <Power size={16} /> Enable
                    </button>
                  )}

                  <button
                    onClick={() => confirmDelete(l.id, l.title)}
                    className={`${styles.btn} ${isRented ? styles.btnDisabledAction : styles.btnDanger}`}
                    disabled={isRented}
                    title={isRented ? "Rented listings cannot be deleted during an active lease" : "Delete listing"}
                  >
                    <Trash2 size={16} /> Delete
                  </button>
                </div>
              </div>
            </div>
          );
        })}
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
          {!roles.includes('ADMIN') && (
            <Link to="/listings/new" className={`${styles.btn} ${styles.btnSuccess}`}>Create New Apartment</Link>
          )}
        </div>
      )}

      {listingToToggle && (
        <div className={styles.modalBackdrop}>
          <div className={styles.modalContent}>
            <h3 className={styles.modalTitle}>
              {listingToToggle.action === 'disable' ? 'Disable Listing' : 'Enable Listing'}
            </h3>
            <p className={styles.modalText}>
              Are you sure you want to {listingToToggle.action}{' '}
              <strong className={styles.modalHighlight}>
                &ldquo;{truncateTitle(listingToToggle.title)}&rdquo;
              </strong>
              ?{' '}
              {listingToToggle.action === 'disable'
                ? 'It will be hidden from public searches and cannot receive new rental applications.'
                : 'It will be published back to public searches.'}
            </p>
            <div className={styles.modalActions}>
              <button onClick={() => setListingToToggle(null)} className={styles.btn}>Cancel</button>
              <button
                onClick={handleToggle}
                className={`${styles.btn} ${listingToToggle.action === 'disable' ? styles.btnDisable : styles.btnEnable}`}
              >
                {listingToToggle.action === 'disable' ? 'Confirm Disable' : 'Confirm Enable'}
              </button>
            </div>
          </div>
        </div>
      )}

      {listingToDelete && (
        <div className={styles.modalBackdrop}>
          <div className={styles.modalContent}>
            <h3 className={styles.modalTitle}>Confirm Deletion</h3>
            <p className={styles.modalText}>
              Are you sure you want to delete{' '}
              <strong className={styles.modalHighlight}>
                &ldquo;{truncateTitle(listingToDelete.title)}&rdquo;
              </strong>
              ? This action cannot be undone.
            </p>
            <div className={styles.modalActions}>
              <button onClick={() => setListingToDelete(null)} className={styles.btn}>Cancel</button>
              <button onClick={handleDelete} className={`${styles.btn} ${styles.btnDanger}`}>Confirm Delete</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
