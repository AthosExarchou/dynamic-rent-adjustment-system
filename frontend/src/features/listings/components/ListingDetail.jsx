import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
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
  PenSquare
} from 'lucide-react';
import apiClient from '../../../shared/api/client';
import { useAuth } from '../../auth';
import styles from './ListingDetail.module.css';

export default function ListingDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, roles } = useAuth();
  const [listing, setListing] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchDetail() {
      try {
        const data = await apiClient(`/listings/${id}`);
        if (data && data.id) {
          setListing(data);
        } else if (['1', '2', '3'].includes(String(id))) {
          setListing(getMockListing(id));
        } else {
          setListing(null);
        }
      } catch (err) {
        console.error("Failed to load listing detail:", err);
        if (['1', '2', '3'].includes(String(id))) {
          setListing(getMockListing(id));
        } else {
          setListing(null);
        }
      } finally {
        setLoading(false);
      }
    }
    fetchDetail();
  }, [id]);

  const getMockListing = (listingId) => ({
    id: listingId,
    title: 'Luxury Penthouse in Athens Center',
    subtitle: 'Stunning Acropolis view',
    description: 'A beautiful penthouse located in the historic center. Spanning 120 square meters, this listing offers state of the art finishes, floor heating, and modern energy efficiencies.',
    address: 'Panepistimiou 15, Athens',
    bedrooms: 3,
    bathrooms: 2,
    sizeM2: 120,
    price: 1200,
    floor: 3,
    yearBuilt: 2018,
    propertyType: 'APARTMENT',
    rentalDuration: 'LONG_TERM',
    status: 'APPROVED'
  });

  if (loading) return (
    <div className={styles.loadingContainer}>
      <div className={styles.spinner}></div>
      <p>Loading apartment details...</p>
    </div>
  );
  
  if (!listing) return (
    <div className={styles.loadingContainer}>
      <p>Listing not found.</p>
      <Link to="/listings" className={styles.backBtn}>Return to listings</Link>
    </div>
  );

  const isUserOnly = isAuthenticated && !roles.includes('ADMIN');

  return (
    <div className={styles.container}>
      
      <div className={styles.topActions}>
        <button onClick={() => navigate(-1)} className={styles.backLink}>
          <ArrowLeft size={20} /> Back
        </button>
      </div>

      <div className={`${styles.card} ${styles.cardHover}`}>
        <div className={styles.header}>
          <div className={styles.headerTitleRow}>
            <h3 className={styles.title}>
              <Building2 className={styles.titleIcon} size={28} />
              Apartment Details
            </h3>
            {listing.status === 'APPROVED' && (
              <span className={styles.statusBadge}>
                <CheckCircle2 size={16} /> Approved
              </span>
            )}
          </div>
        </div>
        
        <div className={styles.body}>
          <div className={styles.titleSection}>
            <h1 className={styles.mainTitle}>{listing.title}</h1>
            {listing.subtitle && <h4 className={styles.subtitle}>{listing.subtitle}</h4>}
            <div className={styles.priceTag}>
              <Euro size={24} /> {listing.price} / month
            </div>
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
            {isUserOnly && (
              <Link to={`/tenant/rent/${listing.id}`} className={`${styles.btn} ${styles.btnPrimary}`}>
                <PenSquare size={18} /> Apply for Rental
              </Link>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
