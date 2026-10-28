import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, Link, useLocation } from 'react-router-dom';
import { 
  Building2, 
  MapPin, 
  Bed, 
  Bath, 
  Maximize, 
  ArrowUpToLine, 
  Home, 
  Clock,
  ArrowLeft,
  CheckCircle2,
  Calendar,
  Euro,
  Pencil,
  PenSquare,
  PowerOff,
  Lock,
  AlertCircle,
  Camera,
  ChevronLeft,
  ChevronRight,
  Maximize2,
  X
} from 'lucide-react';
import apiClient from '../../../shared/api/client';
import { useAuth } from '../../auth';
import { FavouriteButton } from '../../favourites';
import { getImageUrl, DEFAULT_LISTING_IMAGE, REMOTE_FALLBACK_IMAGE } from '../../../shared/utils/imageUtils';
import styles from './ListingDetail.module.css';
import PriceDropIndicator, { computeAvgPricePerM2 } from './PriceDropIndicator';

export default function ListingDetail() {
  const { id } = useParams();
  const location = useLocation();
  const backPath = location.state?.from || '/listings';
  const backLabel = location.state?.from === '/my-listings' ? 'Back to My Properties' : 'Back to Listings';
  const { isAuthenticated, user, roles } = useAuth();
  const [listing, setListing] = useState(null);
  const [avgPricePerM2, setAvgPricePerM2] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  const [isLightboxOpen, setIsLightboxOpen] = useState(false);

  const [error, setError] = useState(null);
  const [appliedListingIds, setAppliedListingIds] = useState([]);
  const [rentingListingId, setRentingListingId] = useState(null);

  useEffect(() => {
    let active = true;
    if (isAuthenticated && roles.includes('USER')) {
      apiClient('/tenant/applications')
        .then(data => {
          if (!active) return;
          if (data?.appliedListingIds) {
            setAppliedListingIds(data.appliedListingIds);
          }
          if (data?.rentingListingId != null) {
            setRentingListingId(data.rentingListingId);
          }
        })
        .catch(() => {});
    }
    return () => { active = false; };
  }, [isAuthenticated, roles]);

  useEffect(() => {
    const controller = new AbortController();
    async function fetchDetail() {
      try {
        const data = await apiClient(`/listings/${id}`, { signal: controller.signal });
        if (data && data.id) {
          setListing(data);
          setError(null);
          // Fetch all approved listings to compute avg pricePerM2 for comparison
          try {
            const all = await apiClient('/listings', { signal: controller.signal });
            const approved = all.filter(l => l.status === 'APPROVED' || l.approved);
            setAvgPricePerM2(computeAvgPricePerM2(approved));
          } catch {
            // non-critical
          }
        } else {
          setListing(null);
          setError("Listing not found.");
        }
      } catch (err) {
        if (err.name === 'AbortError') return;
        console.error("Failed to load listing detail:", err);
        setListing(null);
        setError("Failed to load listing details.");
      } finally {
        setLoading(false);
      }
    }
    fetchDetail();
    return () => controller.abort();
  }, [id]);

  useEffect(() => {
    setActiveImageIndex(0);
    setIsLightboxOpen(false);
  }, [id]);

  const rawImages = Array.isArray(listing?.images)
    ? listing.images.filter(img => typeof img === 'string' && img.trim().length > 0)
    : [];
  const hasImages = rawImages.length > 0;

  useEffect(() => {
    if (!isLightboxOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        setIsLightboxOpen(false);
      } else if (e.key === 'ArrowLeft' && rawImages.length > 1) {
        setActiveImageIndex(prev => (prev - 1 + rawImages.length) % rawImages.length);
      } else if (e.key === 'ArrowRight' && rawImages.length > 1) {
        setActiveImageIndex(prev => (prev + 1) % rawImages.length);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isLightboxOpen, rawImages.length]);

  const trackElRef = useRef(null);
  const thumbnailItemRefs = useRef([]);

  const handleWheel = useCallback((e) => {
    const el = trackElRef.current;
    if (!el) return;
    if (el.scrollWidth > el.clientWidth && Math.abs(e.deltaY) > 0) {
      e.preventDefault();
      el.scrollLeft += e.deltaY;
    }
  }, []);

  const setTrackRef = useCallback((node) => {
    if (trackElRef.current) {
      trackElRef.current.removeEventListener('wheel', handleWheel);
    }
    trackElRef.current = node;
    if (node) {
      node.addEventListener('wheel', handleWheel, { passive: false });
    }
  }, [handleWheel]);

  useEffect(() => {
    const el = trackElRef.current;
    const activeThumb = thumbnailItemRefs.current[activeImageIndex];
    if (!el || !activeThumb) return;

    const elRect = el.getBoundingClientRect();
    const thumbRect = activeThumb.getBoundingClientRect();

    if (thumbRect.left < elRect.left || thumbRect.right > elRect.right) {
      activeThumb.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest',
        inline: 'center'
      });
    }
  }, [activeImageIndex]);

  if (loading) return (
    <div className={styles.loadingContainer}>
      <div className={styles.spinner}></div>
      <p>Loading property details...</p>
    </div>
  );
  
  if (error || !listing) return (
    <div className={styles.loadingContainer}>
      <p>{error || "Listing not found."}</p>
      <Link to="/listings" className={styles.backBtn}>Return to listings</Link>
    </div>
  );

  const isUserOnly = isAuthenticated && roles.includes('USER') && !roles.includes('OWNER') && !roles.includes('ADMIN');
  const canEdit = (user && listing?.owner?.userId === user.id) || roles.includes('ADMIN');
  const currentImageSrc = hasImages
    ? (getImageUrl(rawImages[activeImageIndex]) || DEFAULT_LISTING_IMAGE)
    : DEFAULT_LISTING_IMAGE;

  return (
    <div className={styles.container}>
      
      <div className={styles.topActions}>
        <Link to={backPath} className={styles.backLink}>
          <ArrowLeft size={20} /> {backLabel}
        </Link>
      </div>

      <div className={`${styles.card} ${styles.cardHover}`}>
        <div className={styles.header}>
          <div className={styles.headerTitleRow}>
            <h3 className={styles.title}>
              <Building2 className={styles.titleIcon} size={28} />
              Property Details
            </h3>
            {(listing.status === 'RENTED' || listing.rented) ? (
              <span className={styles.statusBadgeRented}>
                <Lock size={16} /> Rented
              </span>
            ) : listing.status === 'APPROVED' ? (
              <span className={styles.statusBadge}>
                <CheckCircle2 size={16} /> Approved
              </span>
            ) : listing.status === 'DISABLED' ? (
              <span className={styles.statusBadge} style={{ backgroundColor: '#f8d7da', color: '#842029', borderColor: '#f5c2c7' }}>
                <PowerOff size={16} /> Disabled
              </span>
            ) : listing.status === 'PENDING' ? (
              <span className={styles.statusBadge} style={{ backgroundColor: '#fff3cd', color: '#856404', borderColor: '#ffeeba' }}>
                <Clock size={16} /> Pending Approval
              </span>
            ) : listing.status === 'REJECTED' ? (
              <span className={styles.statusBadge} style={{ backgroundColor: '#f8d7da', color: '#842029', borderColor: '#f5c2c7' }}>
                <AlertCircle size={16} /> Rejected
              </span>
            ) : null}
          </div>
        </div>
        
        <div className={styles.body}>
          <div className={styles.titleSection}>
            <h1 className={styles.mainTitle}>{listing.title}</h1>
            {listing.subtitle && <h4 className={styles.subtitle}>{listing.subtitle}</h4>}
            <div className={styles.priceTag}>
              <Euro size={24} /> {listing.price} / month
              <PriceDropIndicator listing={listing} avgPricePerM2={avgPricePerM2} />
            </div>
          </div>

          {/* Property Image Gallery Showcase */}
          <div className={styles.galleryContainer}>
            <div className={styles.heroWrapper}>
              <img
                src={currentImageSrc}
                alt={hasImages ? (listing.title ? `${listing.title} view ${activeImageIndex + 1}` : 'Property view') : 'Generic demonstration image (No photos uploaded)'}
                className={styles.heroImage}
                decoding="async"
                onClick={() => setIsLightboxOpen(true)}
                onError={(e) => {
                  if (e.target.src.includes('demo/image1.jpg')) {
                    e.target.src = REMOTE_FALLBACK_IMAGE;
                  } else if (e.target.src !== DEFAULT_LISTING_IMAGE && e.target.src !== REMOTE_FALLBACK_IMAGE) {
                    e.target.src = DEFAULT_LISTING_IMAGE;
                  }
                }}
              />

              <div className={styles.photoCountBadge}>
                <Camera size={14} />
                <span>
                  {hasImages ? `${activeImageIndex + 1} / ${rawImages.length}` : 'Demo Preview (No photos uploaded)'}
                </span>
              </div>

              <button
                type="button"
                className={styles.expandButton}
                onClick={() => setIsLightboxOpen(true)}
                aria-label="View full size image"
                title="View full size"
              >
                <Maximize2 size={16} />
              </button>

              {hasImages && rawImages.length > 1 && (
                <>
                  <button
                    type="button"
                    className={`${styles.navButton} ${styles.navPrev}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      setActiveImageIndex(prev => (prev - 1 + rawImages.length) % rawImages.length);
                    }}
                    aria-label="Previous image"
                  >
                    <ChevronLeft size={22} />
                  </button>
                  <button
                    type="button"
                    className={`${styles.navButton} ${styles.navNext}`}
                    onClick={(e) => {
                      e.stopPropagation();
                      setActiveImageIndex(prev => (prev + 1) % rawImages.length);
                    }}
                    aria-label="Next image"
                  >
                    <ChevronRight size={22} />
                  </button>
                </>
              )}
            </div>

            {hasImages && rawImages.length > 1 && (
              <div
                ref={setTrackRef}
                className={styles.thumbnailsTrack}
              >
                {rawImages.map((imgUrl, idx) => {
                  const resolvedUrl = getImageUrl(imgUrl) || DEFAULT_LISTING_IMAGE;
                  const isActive = idx === activeImageIndex;
                  return (
                    <button
                      key={idx}
                      ref={(node) => { thumbnailItemRefs.current[idx] = node; }}
                      type="button"
                      className={`${styles.thumbnailButton} ${isActive ? styles.activeThumbnail : ''}`}
                      onClick={() => setActiveImageIndex(idx)}
                      aria-label={`Select photo ${idx + 1}`}
                    >
                      <img
                        src={resolvedUrl}
                        alt={`Thumbnail ${idx + 1}`}
                        className={styles.thumbnailImg}
                        loading="lazy"
                        decoding="async"
                        onError={(e) => {
                          if (e.target.src !== DEFAULT_LISTING_IMAGE) {
                            e.target.src = DEFAULT_LISTING_IMAGE;
                          }
                        }}
                      />
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          <hr className={styles.divider} />

          <div className={styles.descriptionSection}>
            <h5 className={styles.sectionHeading}>Description</h5>
            <p className={styles.descriptionText}>{listing.description}</p>
          </div>

          <div className={styles.specsSection}>
            <h5 className={styles.sectionHeading}>Property Specifications</h5>
            
            <div className={styles.specsGrid}>
              <div className={styles.specItem}>
                <div className={styles.specLabel}><MapPin size={16} /> Address</div>
                <div className={styles.specValue}>{listing.address}</div>
              </div>
              
              <div className={styles.specItem}>
                <div className={styles.specLabel}><Home size={16} /> Property Type</div>
                <div className={styles.specValue}>{listing.propertyType}</div>
              </div>

              <div className={styles.specItem}>
                <div className={styles.specLabel}><Maximize size={16} /> Area</div>
                <div className={styles.specValue}>{listing.sizeM2} m²</div>
              </div>

              <div className={styles.specItem}>
                <div className={styles.specLabel}><Bed size={16} /> Bedrooms</div>
                <div className={styles.specValue}>{listing.bedrooms}</div>
              </div>

              <div className={styles.specItem}>
                <div className={styles.specLabel}><Bath size={16} /> Bathrooms</div>
                <div className={styles.specValue}>{listing.bathrooms}</div>
              </div>

              <div className={styles.specItem}>
                <div className={styles.specLabel}><ArrowUpToLine size={16} /> Floor</div>
                <div className={styles.specValue}>{listing.floor || 'Ground'}</div>
              </div>

              <div className={styles.specItem}>
                <div className={styles.specLabel}><Calendar size={16} /> Year Built</div>
                <div className={styles.specValue}>{listing.yearBuilt}</div>
              </div>

              <div className={styles.specItem}>
                <div className={styles.specLabel}><Clock size={16} /> Rental Duration</div>
                <div className={styles.specValue}>{listing.rentalDuration}</div>
              </div>
            </div>
          </div>

          <div className={styles.actionsSection}>
            <FavouriteButton listing={listing} variant="button" showText={true} />

            {canEdit && !listing.rented && listing.status !== 'RENTED' && (
              <Link 
                to={`/listings/edit/${listing.id}`} 
                state={{ from: `/listings/${listing.id}` }}
                className={`${styles.btn} ${styles.btnEdit}`}
              >
                <Pencil size={18} /> Edit Property
              </Link>
            )}

            {canEdit && (listing.rented || listing.status === 'RENTED') && (
              <div className={styles.rentedNoticeBanner}>
                <Lock size={18} className={styles.rentedNoticeIcon} />
                <span>This property has an active rental agreement and cannot be edited until the rental concludes.</span>
              </div>
            )}

            {listing.status === 'DISABLED' ? (
              <div className={styles.appliedBadge} style={{ backgroundColor: '#f8d7da', borderColor: '#f5c2c7', color: '#842029' }}>
                <PowerOff size={18} /> Listing Currently Disabled
              </div>
            ) : (listing.status === 'RENTED' || listing.rented) ? (
              isUserOnly && (
                rentingListingId === Number(listing.id) ? (
                  <div className={styles.appliedBadge} style={{ backgroundColor: '#198754', borderColor: '#198754' }}>
                    <CheckCircle2 size={18} /> Currently Renting
                  </div>
                ) : (
                  <div className={styles.appliedBadge} style={{ backgroundColor: '#cfe2ff', borderColor: '#b6d4fe', color: '#084298' }}>
                    <Lock size={18} /> Currently Rented
                  </div>
                )
              )
            ) : isUserOnly && (
              rentingListingId === Number(listing.id) ? (
                <div className={styles.appliedBadge} style={{ backgroundColor: '#198754', borderColor: '#198754' }}>
                  <CheckCircle2 size={18} /> Currently Renting
                </div>
              ) : appliedListingIds.includes(Number(listing.id)) ? (
                <div className={styles.appliedBadge}>
                  <CheckCircle2 size={18} /> Application Submitted
                </div>
              ) : (
                <Link 
                  to={`/tenant/rent/${listing.id}`} 
                  state={{ fromListingDetail: true }}
                  className={`${styles.btn} ${styles.btnPrimary}`}
                >
                  <PenSquare size={18} /> Apply for Rental
                </Link>
              )
            )}
          </div>
        </div>
      </div>

      {/* Lightbox / Fullscreen Modal */}
      {isLightboxOpen && (
        <div
          className={styles.lightboxBackdrop}
          onClick={() => setIsLightboxOpen(false)}
          role="dialog"
          aria-modal="true"
        >
          <div className={styles.lightboxContainer} onClick={(e) => e.stopPropagation()}>
            <button
              type="button"
              className={styles.lightboxCloseBtn}
              onClick={() => setIsLightboxOpen(false)}
              aria-label="Close fullscreen modal"
            >
              <X size={26} />
            </button>

            <div className={styles.lightboxMedia}>
              <img
                src={currentImageSrc}
                alt={listing.title ? `${listing.title} full view` : 'Property view'}
                className={styles.lightboxImage}
                decoding="async"
                onError={(e) => {
                  if (e.target.src.includes('demo/image1.jpg')) {
                    e.target.src = REMOTE_FALLBACK_IMAGE;
                  } else if (e.target.src !== DEFAULT_LISTING_IMAGE && e.target.src !== REMOTE_FALLBACK_IMAGE) {
                    e.target.src = DEFAULT_LISTING_IMAGE;
                  }
                }}
              />
            </div>

            {hasImages && rawImages.length > 1 && (
              <>
                <button
                  type="button"
                  className={`${styles.lightboxNav} ${styles.lightboxNavPrev}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    setActiveImageIndex(prev => (prev - 1 + rawImages.length) % rawImages.length);
                  }}
                  aria-label="Previous photo"
                >
                  <ChevronLeft size={30} />
                </button>
                <button
                  type="button"
                  className={`${styles.lightboxNav} ${styles.lightboxNavNext}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    setActiveImageIndex(prev => (prev + 1) % rawImages.length);
                  }}
                  aria-label="Next photo"
                >
                  <ChevronRight size={30} />
                </button>
              </>
            )}

            <div className={styles.lightboxFooter}>
              <span>{hasImages ? `Photo ${activeImageIndex + 1} of ${rawImages.length}` : 'Generic demonstration preview (No photos uploaded)'}</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
