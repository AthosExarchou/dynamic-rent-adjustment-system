import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  FileText,
  Home,
  MapPin,
  Euro,
  Calendar,
  CheckCircle2,
  Search,
  Clock,
  Building2
} from 'lucide-react';
import apiClient from '../../../shared/api/client';
import { getImageUrl, DEFAULT_LISTING_IMAGE, REMOTE_FALLBACK_IMAGE } from '../../../shared/utils/imageUtils';
import styles from './MyApplications.module.css';

const formatAppliedDate = (dateString) => {
  if (!dateString) return null;
  try {
    const d = new Date(dateString);
    if (isNaN(d.getTime())) return null;
    return d.toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  } catch {
    return null;
  }
};

const resolveImage = (image) => {
  return image ? (getImageUrl(image) || DEFAULT_LISTING_IMAGE) : DEFAULT_LISTING_IMAGE;
};

const handleImageError = (e) => {
  if (e.target.src.includes('demo/image1.jpg')) {
    e.target.src = REMOTE_FALLBACK_IMAGE;
  } else if (e.target.src !== DEFAULT_LISTING_IMAGE && e.target.src !== REMOTE_FALLBACK_IMAGE) {
    e.target.src = DEFAULT_LISTING_IMAGE;
  }
};

function PropertyThumb({ listing }) {
  const hasImage = Boolean(listing.image);
  return (
    <div className={styles.imageWrapper}>
      <Link to={`/listings/${listing.listingId}`} className={styles.imageLink} aria-label={`View ${listing.title || 'property'}`}>
        <img
          src={resolveImage(listing.image)}
          referrerPolicy="no-referrer"
          alt={listing.title || 'Property preview'}
          className={styles.image}
          loading="lazy"
          decoding="async"
          onError={handleImageError}
        />
      </Link>
      {!hasImage && (
        <span className={styles.demoBadge} title="Demo image - no photos uploaded">
          Demo Preview
        </span>
      )}
    </div>
  );
}

function ListingDetails({ listing }) {
  return (
    <div className={styles.details}>
      <h3 className={styles.cardTitle}>{listing.title || 'Property'}</h3>
      {listing.subtitle && <p className={styles.subtitle}>{listing.subtitle}</p>}

      <p className={styles.address}>
        <MapPin size={16} /> <span>{listing.address || 'Address unavailable'}</span>
      </p>

      <div className={styles.metaRow}>
        <span className={styles.priceBadge}>
          <Euro size={14} /> {listing.price} /mo
        </span>
        {listing.propertyType && (
          <span className={styles.metaItem}>
            <Home size={14} /> {listing.propertyType}
          </span>
        )}
        {listing.sizeM2 != null && (
          <span className={styles.metaItem}>{listing.sizeM2} m²</span>
        )}
        {listing.bedrooms != null && (
          <span className={styles.metaItem}>{listing.bedrooms} Bed</span>
        )}
        {listing.bathrooms != null && (
          <span className={styles.metaItem}>{listing.bathrooms} Bath</span>
        )}
      </div>
    </div>
  );
}

export default function MyApplications() {
  const [applications, setApplications] = useState([]);
  const [rentingListing, setRentingListing] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const controller = new AbortController();

    const fetchApplications = async () => {
      setLoading(true);
      try {
        const data = await apiClient('/tenant/applications', { signal: controller.signal });
        setApplications(Array.isArray(data?.applications) ? data.applications : []);
        setRentingListing(data?.rentingListing || null);
        setError(null);
      } catch (err) {
        if (err.name === 'AbortError') return;
        console.error('Failed to fetch tenant applications:', err);
        setApplications([]);
        setRentingListing(null);
        setError('Failed to load your applications.');
      } finally {
        setLoading(false);
      }
    };

    fetchApplications();
    return () => controller.abort();
  }, []);

  const hasAnything = Boolean(rentingListing) || applications.length > 0;

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h2 className={styles.title}>
          <FileText className={styles.titleIcon} size={28} /> My Applications
        </h2>
        <Link to="/listings" className={`${styles.btn} ${styles.btnPrimary} ${styles.headerBtn}`}>
          <Search size={18} /> Browse Listings
        </Link>
      </div>

      <hr className={styles.divider} />

      {loading ? (
        <div className={styles.emptyState}>
          <p>Loading your applications...</p>
        </div>
      ) : error ? (
        <div className={styles.emptyState}>
          <p className={styles.errorText}>{error}</p>
        </div>
      ) : !hasAnything ? (
        <div className={styles.emptyState}>
          <FileText size={48} className={styles.emptyIcon} />
          <p>You have not applied for any properties yet.</p>
          <Link to="/listings" className={`${styles.btn} ${styles.btnPrimary}`}>
            <Building2 size={18} /> Browse Listings
          </Link>
        </div>
      ) : (
        <>
          {rentingListing && (
            <section className={styles.section}>
              <h5 className={styles.sectionHeading}>
                <Home size={18} /> Current Rental
              </h5>

              <div className={`${styles.card} ${styles.rentalCard}`}>
                <PropertyThumb listing={rentingListing} />
                <div className={styles.cardContent}>
                  <div className={styles.statusRow}>
                    <span className={`${styles.badge} ${styles.badgeRenting}`}>
                      <CheckCircle2 size={14} /> Currently Renting
                    </span>
                  </div>

                  <ListingDetails listing={rentingListing} />

                  <div className={styles.cardFooter}>
                    <Link
                      to={`/listings/${rentingListing.listingId}`}
                      className={`${styles.btn} ${styles.btnOutlineSecondary}`}
                    >
                      View Property
                    </Link>
                  </div>
                </div>
              </div>
            </section>
          )}

          {applications.length > 0 && (
            <section className={styles.section}>
              <h5 className={styles.sectionHeading}>
                <Clock size={18} /> Pending Applications
                <span className={styles.countBadge}>{applications.length}</span>
              </h5>

              <div className={styles.grid}>
                {applications.map((app) => {
                  const appliedDate = formatAppliedDate(app.appliedAt);
                  const listingUnavailable = app.status && app.status !== 'APPROVED';

                  return (
                    <div key={app.applicationId ?? `listing-${app.listingId}`} className={`${styles.card} ${styles.cardHover}`}>
                      <PropertyThumb listing={app} />
                      <div className={styles.cardContent}>
                        <div className={styles.statusRow}>
                          <span className={`${styles.badge} ${styles.badgePending}`}>
                            <Clock size={14} /> Awaiting owner review
                          </span>
                          {listingUnavailable && (
                            <span className={`${styles.badge} ${styles.badgeUnavailable}`}>
                              Listing {app.status.toLowerCase()}
                            </span>
                          )}
                        </div>

                        <ListingDetails listing={app} />

                        <div className={styles.applicationMeta}>
                          {appliedDate && (
                            <span className={styles.appliedDate}>
                              <Calendar size={14} /> Applied on {appliedDate}
                            </span>
                          )}
                          {app.notes && (
                            <div className={styles.notesBox}>
                              <FileText size={14} className={styles.notesIcon} />
                              <p>&ldquo;{app.notes}&rdquo;</p>
                            </div>
                          )}
                        </div>

                        <div className={styles.cardFooter}>
                          <Link
                            to={`/listings/${app.listingId}`}
                            className={`${styles.btn} ${styles.btnOutlineSecondary}`}
                          >
                            View Property
                          </Link>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </section>
          )}
        </>
      )}
    </div>
  );
}
