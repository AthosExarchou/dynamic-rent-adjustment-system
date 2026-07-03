import React, { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
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
  const [searchParams] = useSearchParams();
  const [listings, setListings] = useState([]);
  const [filter, setFilter] = useState({ title: '', minPrice: '', maxPrice: '' });

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // On mount: if the header search bar navigated here with ?search=... or ?title=..., auto-apply it
  useEffect(() => {
    const controller = new AbortController();
    const searchTerm = searchParams.get('search') || searchParams.get('title') || '';
    if (searchTerm) {
      setFilter(f => ({ ...f, title: searchTerm }));
      applyFilter({ title: searchTerm, minPrice: '', maxPrice: '' }, controller.signal);
    } else {
      fetchListings(controller.signal);
    }
    return () => controller.abort();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const fetchListings = async (signal) => {
    setLoading(true);
    try {
      const data = await apiClient('/listings', signal ? { signal } : {});
      const approved = data.filter(l => l.status === 'APPROVED' || l.approved);
      setListings(approved);
      setError(null);
    } catch (err) {
      if (err.name === 'AbortError') return;
      console.error("Failed to fetch listings:", err);
      setListings([]);
      setError("Failed to load listings.");
    } finally {
      setLoading(false);
    }
  };

  const applyFilter = async (filterValues, signal) => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (filterValues.title) params.append('title', filterValues.title);
      if (filterValues.minPrice) params.append('minPrice', filterValues.minPrice);
      if (filterValues.maxPrice) params.append('maxPrice', filterValues.maxPrice);

      const data = await apiClient(`/listings/filter?${params.toString()}`, signal ? { signal } : {});
      const approved = data.filter(l => l.status === 'APPROVED' || l.approved);
      setListings(approved);
      setError(null);
    } catch (err) {
      if (err.name === 'AbortError') return;
      console.warn("Filter request failed.");
      setListings([]);
      setError("Filter request failed.");
    } finally {
      setLoading(false);
    }
  };

  const handleFilterSubmit = async (e) => {
    e.preventDefault();
    await applyFilter(filter);
  };

  const isUserOnly = isAuthenticated && roles.includes('USER') && !roles.includes('OWNER') && !roles.includes('ADMIN');

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
      {loading ? (
        <div className={styles.emptyState}>
          <p>Loading apartments...</p>
        </div>
      ) : error ? (
        <div className={styles.emptyState}>
          <p style={{color: 'red'}}>{error}</p>
        </div>
      ) : listings.length > 0 ? (
        <div className={styles.grid}>
          {listings.map(l => (
            <div key={l.id} className={styles.card}>
              {l.images && l.images.length > 0 && (
                <img 
                  src={l.images[0]} 
                  alt={l.title} 
                  style={{ width: '100%', height: '180px', objectFit: 'cover',
                    borderTopLeftRadius: '12px', borderTopRightRadius: '12px', display: 'block' }}
                  onError={(e) => { e.target.style.display = 'none'; }}
                />
              )}
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
