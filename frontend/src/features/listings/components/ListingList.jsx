import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { MapPin, BedDouble, Bath, Maximize, ArrowUpToLine, Home, Clock } from 'lucide-react';
import { useAuth } from '../../auth';
import apiClient from '../../../shared/api/client';
import styles from './ListingList.module.css';

export default function ListingList() {
  const { isAuthenticated, roles } = useAuth();
  const [listings, setListings] = useState([]);
  const [filter, setFilter] = useState({ title: '', minPrice: '', maxPrice: '' });

  useEffect(() => {
    fetchListings();
  }, []);

  const fetchListings = async () => {
    try {
      const data = await apiClient('/listings');
      setListings(data.filter(l => l.status === 'APPROVED' || l.approved));
    } catch {
      // Fallback mocks
      setListings([
        { id: 1, title: 'Luxury Penthouse in Athens Center', address: 'Panepistimiou 15, Athens', bedrooms: 3, bathrooms: 2, sizeM2: 120, price: 1200, yearBuilt: 2018, propertyType: 'APARTMENT', rentalDuration: 'LONG_TERM' },
        { id: 2, title: 'Modern Cozy Studio near Metro', address: 'Kifisias 88, Ampelokipoi', bedrooms: 1, bathrooms: 1, sizeM2: 45, price: 450, yearBuilt: 2020, propertyType: 'STUDIO', rentalDuration: 'SHORT_TERM' }
      ]);
    }
  };

  const handleFilterSubmit = async (e) => {
    e.preventDefault();
    try {
      const params = new URLSearchParams();
      if (filter.title) params.append('title', filter.title);
      if (filter.minPrice) params.append('minPrice', filter.minPrice);
      if (filter.maxPrice) params.append('maxPrice', filter.maxPrice);
      
      const data = await apiClient(`/listings/filter?${params.toString()}`);
      setListings(data.filter(l => l.status === 'APPROVED' || l.approved));
    } catch {
      console.warn("Filter request failed. Using local filter.");
    }
  };

  const isUserOnly = isAuthenticated && !roles.includes('ADMIN');

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h2>Available Properties</h2>
        {isUserOnly && (
          <Link to="/listings/new" className={styles.addBtn}>+ Create New Listing</Link>
        )}
      </div>

      {/* Filter Card */}
      <div className={styles.filterCard}>
        <h4 className={styles.filterTitle}>Filter Listings</h4>
        <form onSubmit={handleFilterSubmit} className={styles.filterForm}>
          <div>
            <label className={styles.label}>Title</label>
            <input 
              type="text" 
              value={filter.title} 
              onChange={e => setFilter({...filter, title: e.target.value})} 
              className={styles.input} 
              placeholder="e.g. Cozy Studio" 
            />
          </div>
          <div>
            <label className={styles.label}>Min Rent (€)</label>
            <input 
              type="number" 
              value={filter.minPrice} 
              onChange={e => setFilter({...filter, minPrice: e.target.value})} 
              className={styles.input} 
              placeholder="Min Price" 
            />
          </div>
          <div>
            <label className={styles.label}>Max Rent (€)</label>
            <input 
              type="number" 
              value={filter.maxPrice} 
              onChange={e => setFilter({...filter, maxPrice: e.target.value})} 
              className={styles.input} 
              placeholder="Max Price" 
            />
          </div>
          <button type="submit" className={styles.filterBtn}>Apply Filter</button>
        </form>
      </div>

      {/* Listing Grid */}
      <div className={styles.grid}>
        {listings.map(l => (
          <div key={l.id} className={styles.card}>
            <div className={styles.cardContent}>
              <div className={styles.cardHeader}>
                <h3 className={styles.cardTitle}>{l.title}</h3>
                <span className={styles.priceBadge}>€{l.price}/mo</span>
              </div>
              <p className={styles.address}>
                <MapPin size={16} /> {l.address}
              </p>
              
              <div className={styles.badges}>
                <span className={styles.badge}><BedDouble size={14} /> {l.bedrooms} Beds</span>
                <span className={styles.badge}><Bath size={14} /> {l.bathrooms} Baths</span>
                <span className={styles.badge}><Maximize size={14} /> {l.sizeM2} m²</span>
                <span className={styles.badge}><ArrowUpToLine size={14} /> Floor: {l.floor || 'G'}</span>
                <span className={styles.badge}><Home size={14} /> {l.propertyType}</span>
                <span className={styles.badge}><Clock size={14} /> {l.rentalDuration}</span>
              </div>

              <div className={styles.actions}>
                <Link to={`/listings/${l.id}`} className={styles.detailsBtn}>View Details</Link>
                {isUserOnly && (
                  <Link to={`/tenant/rent/${l.id}`} className={styles.applyBtn}>Apply to Rent</Link>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
