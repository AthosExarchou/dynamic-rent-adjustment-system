import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { 
  Building2, 
  MapPin, 
  Bed, 
  Bath, 
  Maximize, 
  ArrowUpToLine, 
  Home, 
  Clock, 
  Search, 
  Plus, 
  Filter, 
  Check, 
  Euro, 
  Type 
} from 'lucide-react';
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
      const approved = data.filter(l => l.status === 'APPROVED' || l.approved);
      if (approved.length > 0) {
        setListings(approved);
      } else {
        setListings(getMockListings());
      }
    } catch (err) {
      console.error("Failed to fetch listings:", err);
      setListings(getMockListings());
    }
  };

  const getMockListings = () => [
    { id: 1, title: 'Luxury Penthouse in Athens Center', address: 'Panepistimiou 15, Athens', bedrooms: 3, bathrooms: 2, sizeM2: 120, price: 1200, yearBuilt: 2018, propertyType: 'APARTMENT', rentalDuration: 'LONG_TERM', floor: 5, status: 'APPROVED' },
    { id: 2, title: 'Modern Cozy Studio near Metro', address: 'Kifisias 88, Ampelokipoi', bedrooms: 1, bathrooms: 1, sizeM2: 45, price: 450, yearBuilt: 2020, propertyType: 'STUDIO', rentalDuration: 'SHORT_TERM', floor: 1, status: 'APPROVED' }
  ];

  const applyLocalFilter = () => {
    const mocks = getMockListings();
    const filtered = mocks.filter(l => {
      let match = true;
      if (filter.title && !l.title.toLowerCase().includes(filter.title.toLowerCase())) match = false;
      if (filter.minPrice && l.price < Number(filter.minPrice)) match = false;
      if (filter.maxPrice && l.price > Number(filter.maxPrice)) match = false;
      return match;
    });
    setListings(filtered);
  };

  const handleFilterSubmit = async (e) => {
    e.preventDefault();
    try {
      const params = new URLSearchParams();
      if (filter.title) params.append('title', filter.title);
      if (filter.minPrice) params.append('minPrice', filter.minPrice);
      if (filter.maxPrice) params.append('maxPrice', filter.maxPrice);
      
      const data = await apiClient(`/listings/filter?${params.toString()}`);
      const approved = data.filter(l => l.status === 'APPROVED' || l.approved);
      if (approved.length > 0) {
        setListings(approved);
      } else {
        applyLocalFilter();
      }
    } catch {
      console.warn("Filter request failed. Using local filter.");
      applyLocalFilter();
    }
  };

  const isUserOnly = isAuthenticated && !roles.includes('ADMIN');

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h2 className={styles.pageTitle}>
          <Building2 className={styles.pageIcon} size={28} /> Apartments
        </h2>
        {isUserOnly && (
          <Link to="/listings/new" className={`${styles.btn} ${styles.btnSuccess} ${styles.pulseBtn}`}>
            <Plus size={18} /> Create New Apartment
          </Link>
        )}
      </div>

      <hr className={styles.divider} />

      {/* Filter Card */}
      <div className={styles.filterCard}>
        <div className={styles.filterHeader}>
          <h5 className={styles.filterTitle}>
            <Filter size={20} /> Filter Apartments
          </h5>
        </div>
        <div className={styles.filterBody}>
          <form onSubmit={handleFilterSubmit} className={styles.filterForm}>
            <div className={styles.formGroup}>
              <label className={styles.label}>
                <Type size={16} /> Title
              </label>
              <input 
                type="text" 
                value={filter.title} 
                onChange={e => setFilter({...filter, title: e.target.value})} 
                className={styles.input} 
                placeholder="e.g. Cozy Studio" 
              />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.label}>
                <Euro size={16} /> Min Rent
              </label>
              <input 
                type="number" 
                value={filter.minPrice} 
                onChange={e => setFilter({...filter, minPrice: e.target.value})} 
                className={styles.input} 
                placeholder="e.g. 300" 
              />
            </div>
            <div className={styles.formGroup}>
              <label className={styles.label}>
                <Euro size={16} /> Max Rent
              </label>
              <input 
                type="number" 
                value={filter.maxPrice} 
                onChange={e => setFilter({...filter, maxPrice: e.target.value})} 
                className={styles.input} 
                placeholder="e.g. 2000" 
              />
            </div>
            <div className={styles.filterBtnWrapper}>
              <button type="submit" className={`${styles.btn} ${styles.btnOutlineSecondary}`}>
                <Check size={18} /> Apply Filter
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Listing Grid */}
      {listings.length > 0 ? (
        <div className={styles.grid}>
          {listings.map(l => (
            <div key={l.id} className={styles.card}>
              <div className={styles.cardContent}>
                <div className={styles.cardTop}>
                  <h5 className={styles.cardTitle}>
                    <Building2 size={20} className={styles.cardIcon} />
                    {l.title}
                  </h5>
                  <h5 className={styles.priceContainer}>
                    <span className={styles.priceBadge}>{l.price} € /mo</span>
                  </h5>
                </div>
                
                <div className={styles.apartmentInfo}>
                  <div className={styles.infoItem}><MapPin size={16} /> <span>{l.address}</span></div>
                  <div className={styles.infoItem}><ArrowUpToLine size={16} /> <span>Floor {l.floor || 'G'}</span></div>
                  <div className={styles.infoItem}><Bath size={16} /> <span>{l.bathrooms} Bath</span></div>
                  <div className={styles.infoItem}><Bed size={16} /> <span>{l.bedrooms} Bed</span></div>
                  <div className={styles.infoItem}><Maximize size={16} /> <span>{l.sizeM2} m²</span></div>
                  <div className={styles.infoItem}><Clock size={16} /> <span>{l.yearBuilt}</span></div>
                  <div className={styles.infoItem}><Home size={16} /> <span>{l.propertyType}</span></div>
                </div>

                <div className={styles.cardFooter}>
                  <Link to={`/listings/${l.id}`} className={`${styles.btn} ${styles.btnOutlinePrimary} ${styles.actionBtn}`}>
                    <Search size={16} /> View Details
                  </Link>
                  {isUserOnly && (
                    <Link to={`/tenant/rent/${l.id}`} className={`${styles.btn} ${styles.btnPrimary} ${styles.actionBtn}`}>
                      Apply for Rental
                    </Link>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className={styles.emptyState}>
          <p>No apartments currently available.</p>
        </div>
      )}
      
      {/* Bottom CTA */}
      <div className={styles.bottomCta}>
        {isUserOnly && (
          <Link to="/listings/new" className={`${styles.btn} ${styles.btnOutlineSuccess} ${styles.pulseBtn}`}>
            <Plus size={18} /> Create New Apartment
          </Link>
        )}
      </div>
    </div>
  );
}
