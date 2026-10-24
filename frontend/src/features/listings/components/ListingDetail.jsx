import React, { useEffect, useState } from 'react';
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
  AlertCircle
} from 'lucide-react';
import apiClient from '../../../shared/api/client';
import { useAuth } from '../../auth';
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



  if (loading) return (
    <div className={styles.loadingContainer}>
      <div className={styles.spinner}></div>
      <p>Loading apartment details...</p>
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
              Apartment Details
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

          <hr className={styles.divider} />

          {listing.images && listing.images.length > 0 && (
            <div className={styles.imageGallery}
                 style={{display: 'flex', gap: '10px', overflowX: 'auto',
                   marginBottom: '1.5rem', padding: '10px 0', scrollbarWidth: 'thin' }}>
              {listing.images.map((imgUrl, idx) => (
                <img 
                  key={idx} 
                  src={imgUrl} 
                  alt={`Listing view ${idx + 1}`} 
                  style={{ height: '300px', objectFit: 'cover',
                    borderRadius: '8px', flexShrink: 0, border: '1px solid #eaeaea' }}
                  onError={(e) => { e.target.style.display = 'none'; }}
                />
              ))}
            </div>
          )}

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
    </div>
  );
}
