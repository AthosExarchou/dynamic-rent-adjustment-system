import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import apiClient from '../../../shared/api/client';
import { useAuth } from '../../auth';
import styles from './ListingDetail.module.css';

export default function ListingDetail() {
  const { id } = useParams();
  const { isAuthenticated, roles } = useAuth();
  const [listing, setListing] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchDetail() {
      try {
        const data = await apiClient(`/listings/${id}`);
        setListing(data);
      } catch {
        setListing({
          id,
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
          rentalDuration: 'LONG_TERM'
        });
      } finally {
        setLoading(false);
      }
    }
    fetchDetail();
  }, [id]);

  if (loading) return <div>Loading...</div>;
  if (!listing) return <div>Listing not found.</div>;

  const isUserOnly = isAuthenticated && !roles.includes('ADMIN');

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div className={styles.header}>
          <h1 className={styles.title}>{listing.title}</h1>
          {listing.subtitle && <h4 className={styles.subtitle}>{listing.subtitle}</h4>}
        </div>
        <div className={styles.content}>
          <p className={styles.description}>{listing.description}</p>
          
          <h3 className={styles.sectionTitle}>Property Specs</h3>
          <div className={styles.specsGrid}>
            <p><strong>Address:</strong> {listing.address}</p>
            <p><strong>Price / Rent:</strong> €{listing.price}/mo</p>
            <p><strong>Size:</strong> {listing.sizeM2} m²</p>
            <p><strong>Floor:</strong> {listing.floor}</p>
            <p><strong>Bedrooms:</strong> {listing.bedrooms}</p>
            <p><strong>Bathrooms:</strong> {listing.bathrooms}</p>
            <p><strong>Type:</strong> {listing.propertyType}</p>
            <p><strong>Rental Duration:</strong> {listing.rentalDuration}</p>
            <p><strong>Year Built:</strong> {listing.yearBuilt}</p>
          </div>

          <div className={styles.actions}>
            <Link to="/listings" className={styles.backBtn}>&larr; Back to Listings</Link>
            {isUserOnly && (
              <Link to={`/tenant/rent/${listing.id}`} className={styles.applyBtn}>Apply for Rental</Link>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
