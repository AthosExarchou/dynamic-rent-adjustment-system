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
  X,
  SlidersHorizontal,
  Compass,
  Calendar,
  Globe,
  ChevronDown,
  ChevronUp
} from 'lucide-react';
import { useAuth } from '../../auth';
import apiClient from '../../../shared/api/client';
import styles from './ListingList.module.css';
import PriceDropIndicator, { computeAvgPricePerM2 } from './PriceDropIndicator';
import DatePicker from '../../../shared/components/DatePicker';
import { LISTING_CONSTRAINTS } from '../models/listing';

const getTodayStr = () => {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

const offsetDateStr = (dateStr, daysOffset) => {
  if (!dateStr) return '';
  const [y, m, d] = dateStr.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  date.setDate(date.getDate() + daysOffset);
  const nextY = date.getFullYear();
  const nextM = String(date.getMonth() + 1).padStart(2, '0');
  const nextD = String(date.getDate()).padStart(2, '0');
  return `${nextY}-${nextM}-${nextD}`;
};

export default function ListingList() {
  const { isAuthenticated, roles } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const [listings, setListings] = useState([]);
  const [avgPricePerM2, setAvgPricePerM2] = useState(null);
  const [filter, setFilter] = useState({
    title: '',
    type: '',
    minPrice: '',
    maxPrice: '',
    municipality: '',
    district: '',
    minBedrooms: '',
    maxBedrooms: '',
    minBathrooms: '',
    maxBathrooms: '',
    minYear: '',
    maxYear: '',
    updatedAfter: '',
    updatedBefore: '',
    externalOnly: '',
  });
  const [isAdvancedOpen, setIsAdvancedOpen] = useState(false);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [appliedListingIds, setAppliedListingIds] = useState([]);
  const [rentingListingId, setRentingListingId] = useState(null);

  const todayStr = getTodayStr();
  const currentMaxYear = new Date().getFullYear() + LISTING_CONSTRAINTS.YEAR_BUILT_MAX_OFFSET;

  const searchTerm = searchParams.get('search') || searchParams.get('title') || '';
  const hasActiveFilters = Array.from(searchParams.keys()).length > 0 || Object.values(filter).some(v => v !== '');
  const activeFiltersCount = Object.entries(filter).filter(([k, v]) => {
    if (k === 'title') return (v || '').trim() !== '';
    return v !== '' && v !== null && v !== undefined;
  }).length;

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

  // Whenever URL search parameters change, fetch and filter listings
  useEffect(() => {
    const controller = new AbortController();
    const paramsObj = Object.fromEntries(searchParams.entries());
    const hasActive = Object.keys(paramsObj).length > 0;

    // Keep form filter state in sync with URL parameters
    setFilter(prev => {
      const next = { ...prev };
      Object.keys(next).forEach(key => {
        if (key === 'title') {
          next.title = paramsObj.search || paramsObj.title || '';
        } else {
          next[key] = paramsObj[key] || '';
        }
      });
      return next;
    });

    // Auto-open advanced filters if any advanced criteria are present in URL
    if (
      paramsObj.municipality ||
      paramsObj.district ||
      paramsObj.minBedrooms ||
      paramsObj.maxBedrooms ||
      paramsObj.minBathrooms ||
      paramsObj.maxBathrooms ||
      paramsObj.minYear ||
      paramsObj.maxYear ||
      paramsObj.updatedAfter ||
      paramsObj.updatedBefore ||
      paramsObj.externalOnly
    ) {
      setIsAdvancedOpen(true);
    }

    if (hasActive) {
      applyFilter(paramsObj, controller.signal);
    } else {
      fetchListings(controller.signal);
    }

    return () => controller.abort();
  }, [searchParams]);

  const fetchListings = async (signal) => {
    setLoading(true);
    try {
      const data = await apiClient('/listings', signal ? { signal } : {});
      const approved = data.filter(l => l.status === 'APPROVED' || l.approved);
      setListings(approved);
      setAvgPricePerM2(computeAvgPricePerM2(approved));
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
      
      // Dynamically transfer all non-empty filter parameters
      Object.entries(filterValues).forEach(([key, val]) => {
        if (val !== undefined && val !== null && String(val).trim() !== '') {
          params.append(key, String(val).trim());
        }
      });

      // Maintain title & search compatibility for keyword searches
      const searchKeyword = filterValues.search || filterValues.title;
      if (searchKeyword && !params.has('search')) params.append('search', searchKeyword.trim());
      if (searchKeyword && !params.has('title')) params.append('title', searchKeyword.trim());

      const data = await apiClient(`/listings/filter?${params.toString()}`, signal ? { signal } : {});
      const approved = data.filter(l => l.status === 'APPROVED' || l.approved);
      setListings(approved);
      setAvgPricePerM2(computeAvgPricePerM2(approved));
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

  /** Clamps a numeric filter field to [min, max] when the input loses focus (mirrors ListingForm). */
  const clampField = (field, min, max) => () => {
    const val = filter[field];
    if (val === '' || val === null || val === undefined) return;
    const num = Number(val);
    if (isNaN(num)) return;
    const clamped = String(Math.min(Math.max(num, min), max));
    if (clamped !== String(val)) {
      setFilter(prev => ({ ...prev, [field]: clamped }));
    }
  };

  /** Helper to clamp a value to [min, max] */
  const clampValue = (val, min, max) => {
    if (val === '' || val === null || val === undefined) return val;
    const num = Number(val);
    if (isNaN(num)) return val;
    return String(Math.min(Math.max(num, min), max));
  };

  const handleFilterSubmit = (e) => {
    e.preventDefault();
    const currentMaxYear = new Date().getFullYear() + LISTING_CONSTRAINTS.YEAR_BUILT_MAX_OFFSET;
    const todayStr = getTodayStr();

    // Auto-clamp numeric values if user submitted directly via Enter key
    const clampedMinPrice = clampValue(filter.minPrice, LISTING_CONSTRAINTS.PRICE_MIN, LISTING_CONSTRAINTS.PRICE_MAX);
    const clampedMaxPrice = clampValue(filter.maxPrice, LISTING_CONSTRAINTS.PRICE_MIN, LISTING_CONSTRAINTS.PRICE_MAX);
    const clampedMinBedrooms = clampValue(filter.minBedrooms, LISTING_CONSTRAINTS.BEDROOMS_MIN, LISTING_CONSTRAINTS.BEDROOMS_MAX);
    const clampedMaxBedrooms = clampValue(filter.maxBedrooms, LISTING_CONSTRAINTS.BEDROOMS_MIN, LISTING_CONSTRAINTS.BEDROOMS_MAX);
    const clampedMinBathrooms = clampValue(filter.minBathrooms, LISTING_CONSTRAINTS.BATHROOMS_MIN, LISTING_CONSTRAINTS.BATHROOMS_MAX);
    const clampedMaxBathrooms = clampValue(filter.maxBathrooms, LISTING_CONSTRAINTS.BATHROOMS_MIN, LISTING_CONSTRAINTS.BATHROOMS_MAX);
    const clampedMinYear = clampValue(filter.minYear, LISTING_CONSTRAINTS.YEAR_BUILT_MIN, currentMaxYear);
    const clampedMaxYear = clampValue(filter.maxYear, LISTING_CONSTRAINTS.YEAR_BUILT_MIN, currentMaxYear);

    const activeFilter = {
      ...filter,
      minPrice: clampedMinPrice,
      maxPrice: clampedMaxPrice,
      minBedrooms: clampedMinBedrooms,
      maxBedrooms: clampedMaxBedrooms,
      minBathrooms: clampedMinBathrooms,
      maxBathrooms: clampedMaxBathrooms,
      minYear: clampedMinYear,
      maxYear: clampedMaxYear,
    };
    setFilter(activeFilter);

    // Range relationships
    if (activeFilter.minPrice !== '' && activeFilter.maxPrice !== '' && Number(activeFilter.minPrice) > Number(activeFilter.maxPrice)) {
      setError("Minimum rent cannot be greater than maximum rent.");
      return;
    }
    if (activeFilter.minBedrooms !== '' && activeFilter.maxBedrooms !== '' && Number(activeFilter.minBedrooms) > Number(activeFilter.maxBedrooms)) {
      setError("Minimum bedrooms cannot be greater than maximum bedrooms.");
      return;
    }
    if (activeFilter.minBathrooms !== '' && activeFilter.maxBathrooms !== '' && Number(activeFilter.minBathrooms) > Number(activeFilter.maxBathrooms)) {
      setError("Minimum bathrooms cannot be greater than maximum bathrooms.");
      return;
    }
    if (activeFilter.minYear !== '' && activeFilter.maxYear !== '' && Number(activeFilter.minYear) > Number(activeFilter.maxYear)) {
      setError("Minimum year cannot be greater than maximum year.");
      return;
    }

    // Updated Dates bounds & range
    if (activeFilter.updatedAfter && activeFilter.updatedAfter > todayStr) {
      setError("Updated After date cannot be in the future.");
      return;
    }
    if (activeFilter.updatedBefore && activeFilter.updatedBefore > todayStr) {
      setError("Updated Before date cannot be in the future.");
      return;
    }
    if (activeFilter.updatedAfter && activeFilter.updatedBefore && activeFilter.updatedAfter >= activeFilter.updatedBefore) {
      setError("Updated After date must be earlier than Updated Before date.");
      return;
    }
    setError(null);

    const params = new URLSearchParams();
    Object.entries(filter).forEach(([key, val]) => {
      const trimmed = typeof val === 'string' ? val.trim() : val;
      if (trimmed !== '' && trimmed !== null && trimmed !== undefined) {
        // Map 'title' to 'search' in URL params for unified keyword search
        params.set(key === 'title' ? 'search' : key, trimmed);
      }
    });
    setSearchParams(params);
  };

  const handleFilterReset = () => {
    setError(null);
    setFilter({
      title: '',
      type: '',
      minPrice: '',
      maxPrice: '',
      municipality: '',
      district: '',
      minBedrooms: '',
      maxBedrooms: '',
      minBathrooms: '',
      maxBathrooms: '',
      minYear: '',
      maxYear: '',
      updatedAfter: '',
      updatedBefore: '',
      externalOnly: '',
    });
    setSearchParams({});
  };

  const isUserOnly = isAuthenticated && roles.includes('USER') && !roles.includes('OWNER') && !roles.includes('ADMIN');
  const isOwner = isAuthenticated && roles.includes('OWNER');

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h2 className={styles.pageTitle}>
          <Building2 className={styles.pageIcon} size={28} /> Apartments
        </h2>
        {isUserOnly && (
          <Link to="/owner/new" className={`${styles.btn} ${styles.btnSuccess} ${styles.pulseBtn}`}>
            <Plus size={18} /> Become an Owner to List
          </Link>
        )}
        {isOwner && (
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
            <Filter size={20} /> Filter Apartments {activeFiltersCount > 0 && <span style={{ fontSize: '0.85rem', opacity: 0.9 }}>({activeFiltersCount} active)</span>}
          </h5>
        </div>
        <div className={styles.filterBody}>
          <form onSubmit={handleFilterSubmit} className={styles.filterForm}>
            {/* Primary Filters */}
            <div className={styles.formGroup}>
              <label className={styles.label}>
                <Search size={16} /> Search / Title
              </label>
              <input 
                type="text" 
                value={filter.title} 
                onChange={e => setFilter({...filter, title: e.target.value})} 
                className={styles.input} 
                maxLength={LISTING_CONSTRAINTS.TITLE_MAX}
                placeholder="e.g. Studio, Athens..." 
              />
            </div>

            <div className={styles.formGroup}>
              <label className={styles.label}>
                <Home size={16} /> Property Type
              </label>
              <select 
                value={filter.type} 
                onChange={e => setFilter({...filter, type: e.target.value})}
                className={styles.input}
              >
                <option value="">All Property Types</option>
                <option value="APARTMENT">Apartment</option>
                <option value="STUDIO">Studio</option>
                <option value="HOUSE">House</option>
                <option value="MAISONETTE">Maisonette</option>
                <option value="LOFT">Loft</option>
                <option value="VILLA">Villa</option>
                <option value="OTHER">Other</option>
              </select>
            </div>

            <div className={styles.formGroup}>
              <label className={styles.label}>
                <Euro size={16} /> Min Rent (€)
              </label>
              <input 
                type="number" 
                min={LISTING_CONSTRAINTS.PRICE_MIN}
                max={LISTING_CONSTRAINTS.PRICE_MAX}
                value={filter.minPrice} 
                onChange={e => setFilter({...filter, minPrice: e.target.value})} 
                onBlur={clampField('minPrice', LISTING_CONSTRAINTS.PRICE_MIN, LISTING_CONSTRAINTS.PRICE_MAX)}
                className={styles.input} 
                placeholder="e.g. 300" 
              />
            </div>

            <div className={styles.formGroup}>
              <label className={styles.label}>
                <Euro size={16} /> Max Rent (€)
              </label>
              <input 
                type="number" 
                min={LISTING_CONSTRAINTS.PRICE_MIN}
                max={LISTING_CONSTRAINTS.PRICE_MAX}
                value={filter.maxPrice} 
                onChange={e => setFilter({...filter, maxPrice: e.target.value})} 
                onBlur={clampField('maxPrice', LISTING_CONSTRAINTS.PRICE_MIN, LISTING_CONSTRAINTS.PRICE_MAX)}
                className={styles.input} 
                placeholder="e.g. 1500" 
              />
            </div>

            {/* Advanced Filters Toggle */}
            <div className={styles.advancedToggleWrapper}>
              <button 
                type="button" 
                onClick={() => setIsAdvancedOpen(!isAdvancedOpen)}
                className={styles.advancedToggleBtn}
              >
                <SlidersHorizontal size={16} />
                {isAdvancedOpen ? 'Hide Advanced Filters' : 'More Filters (Location, Rooms, Year, Date, Origin)'}
                {isAdvancedOpen ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
              </button>
            </div>

            {/* Advanced Filters Expandable Grid */}
            {isAdvancedOpen && (
              <div className={styles.advancedGrid}>
                {/* Location: Municipality & District */}
                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    <MapPin size={16} /> Municipality (Δήμος)
                  </label>
                  <input 
                    type="text" 
                    value={filter.municipality} 
                    onChange={e => setFilter({...filter, municipality: e.target.value})} 
                    className={styles.input} 
                    maxLength={LISTING_CONSTRAINTS.ADDRESS_MAX}
                    placeholder="e.g. Αθηναίων, Καλλιθέας" 
                  />
                </div>

                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    <Compass size={16} /> District / Area (Περιοχή)
                  </label>
                  <input 
                    type="text" 
                    value={filter.district} 
                    onChange={e => setFilter({...filter, district: e.target.value})} 
                    className={styles.input} 
                    maxLength={LISTING_CONSTRAINTS.SUBTITLE_MAX}
                    placeholder="e.g. Κυψέλη, Παγκράτι" 
                  />
                </div>

                {/* Rooms: Bedrooms & Bathrooms */}
                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    <Bed size={16} /> Min Bedrooms
                  </label>
                  <input 
                    type="number" 
                    min={LISTING_CONSTRAINTS.BEDROOMS_MIN}
                    max={LISTING_CONSTRAINTS.BEDROOMS_MAX}
                    value={filter.minBedrooms} 
                    onChange={e => setFilter({...filter, minBedrooms: e.target.value})} 
                    onBlur={clampField('minBedrooms', LISTING_CONSTRAINTS.BEDROOMS_MIN, LISTING_CONSTRAINTS.BEDROOMS_MAX)}
                    className={styles.input} 
                    placeholder="e.g. 1" 
                  />
                </div>

                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    <Bed size={16} /> Max Bedrooms
                  </label>
                  <input 
                    type="number" 
                    min={LISTING_CONSTRAINTS.BEDROOMS_MIN}
                    max={LISTING_CONSTRAINTS.BEDROOMS_MAX}
                    value={filter.maxBedrooms} 
                    onChange={e => setFilter({...filter, maxBedrooms: e.target.value})} 
                    onBlur={clampField('maxBedrooms', LISTING_CONSTRAINTS.BEDROOMS_MIN, LISTING_CONSTRAINTS.BEDROOMS_MAX)}
                    className={styles.input} 
                    placeholder="e.g. 4" 
                  />
                </div>

                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    <Bath size={16} /> Min Bathrooms
                  </label>
                  <input 
                    type="number" 
                    min={LISTING_CONSTRAINTS.BATHROOMS_MIN}
                    max={LISTING_CONSTRAINTS.BATHROOMS_MAX}
                    value={filter.minBathrooms} 
                    onChange={e => setFilter({...filter, minBathrooms: e.target.value})} 
                    onBlur={clampField('minBathrooms', LISTING_CONSTRAINTS.BATHROOMS_MIN, LISTING_CONSTRAINTS.BATHROOMS_MAX)}
                    className={styles.input} 
                    placeholder="e.g. 1" 
                  />
                </div>

                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    <Bath size={16} /> Max Bathrooms
                  </label>
                  <input 
                    type="number" 
                    min={LISTING_CONSTRAINTS.BATHROOMS_MIN}
                    max={LISTING_CONSTRAINTS.BATHROOMS_MAX}
                    value={filter.maxBathrooms} 
                    onChange={e => setFilter({...filter, maxBathrooms: e.target.value})} 
                    onBlur={clampField('maxBathrooms', LISTING_CONSTRAINTS.BATHROOMS_MIN, LISTING_CONSTRAINTS.BATHROOMS_MAX)}
                    className={styles.input} 
                    placeholder="e.g. 3" 
                  />
                </div>

                {/* Year Built */}
                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    <Clock size={16} /> Min Year Built
                  </label>
                  <input 
                    type="number" 
                    min={LISTING_CONSTRAINTS.YEAR_BUILT_MIN}
                    max={currentMaxYear}
                    value={filter.minYear} 
                    onChange={e => setFilter({...filter, minYear: e.target.value})} 
                    onBlur={clampField('minYear', LISTING_CONSTRAINTS.YEAR_BUILT_MIN, currentMaxYear)}
                    className={styles.input} 
                    placeholder="e.g. 1990" 
                  />
                </div>

                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    <Clock size={16} /> Max Year Built
                  </label>
                  <input 
                    type="number" 
                    min={LISTING_CONSTRAINTS.YEAR_BUILT_MIN}
                    max={currentMaxYear}
                    value={filter.maxYear} 
                    onChange={e => setFilter({...filter, maxYear: e.target.value})} 
                    onBlur={clampField('maxYear', LISTING_CONSTRAINTS.YEAR_BUILT_MIN, currentMaxYear)}
                    className={styles.input} 
                    placeholder="e.g. 2024" 
                  />
                </div>

                {/* Updated Date Range */}
                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    <Calendar size={16} /> Updated After
                  </label>
                  <DatePicker
                    value={filter.updatedAfter}
                    onChange={(val) => {
                      if (val && filter.updatedBefore && val >= filter.updatedBefore) {
                        setFilter({ ...filter, updatedAfter: val, updatedBefore: '' });
                      } else {
                        setFilter({ ...filter, updatedAfter: val });
                      }
                    }}
                    placeholder="Select start date"
                    max={
                      filter.updatedBefore
                        ? (offsetDateStr(filter.updatedBefore, -1) < todayStr ? offsetDateStr(filter.updatedBefore, -1) : todayStr)
                        : todayStr
                    }
                  />
                </div>

                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    <Calendar size={16} /> Updated Before
                  </label>
                  <DatePicker
                    value={filter.updatedBefore}
                    onChange={(val) => {
                      if (val && filter.updatedAfter && val <= filter.updatedAfter) {
                        setFilter({ ...filter, updatedBefore: val, updatedAfter: '' });
                      } else {
                        setFilter({ ...filter, updatedBefore: val });
                      }
                    }}
                    placeholder="Select end date"
                    min={filter.updatedAfter ? offsetDateStr(filter.updatedAfter, 1) : undefined}
                    max={todayStr}
                    align="right"
                  />
                </div>

                {/* Listing Origin / Source */}
                <div className={styles.formGroup}>
                  <label className={styles.label}>
                    <Globe size={16} /> Listing Origin
                  </label>
                  <select 
                    value={filter.externalOnly} 
                    onChange={e => setFilter({...filter, externalOnly: e.target.value})} 
                    className={styles.input}
                  >
                    <option value="">All Origins</option>
                    <option value="false">Direct (DRAS Platform)</option>
                    <option value="true">External (Scraped Listings)</option>
                  </select>
                </div>
              </div>
            )}

            <div className={styles.filterBtnWrapper}>
              <button type="submit" className={`${styles.btn} ${styles.btnPrimary}`}>
                <Check size={18} /> Apply Filters {activeFiltersCount > 0 && `(${activeFiltersCount})`}
              </button>
              {hasActiveFilters && (
                <button 
                  type="button" 
                  onClick={handleFilterReset}
                  className={`${styles.btn} ${styles.btnOutlineSecondary}`}
                >
                  <X size={18} /> Reset
                </button>
              )}
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
                    <PriceDropIndicator listing={l} avgPricePerM2={avgPricePerM2} />
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
                    rentingListingId === Number(l.id) ? (
                      <span className={`${styles.btn} ${styles.btnApplied} ${styles.actionBtn}`} style={{ backgroundColor: '#198754', borderColor: '#198754' }} title="You are currently renting this property">
                        <Check size={16} /> Renting
                      </span>
                    ) : appliedListingIds.includes(Number(l.id)) ? (
                      <span className={`${styles.btn} ${styles.btnApplied} ${styles.actionBtn}`} title="You have already applied for this property">
                        <Check size={16} /> Applied
                      </span>
                    ) : (
                      <Link to={`/tenant/rent/${l.id}`} className={`${styles.btn} ${styles.btnPrimary} ${styles.actionBtn}`}>
                        Apply for Rental
                      </Link>
                    )
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className={styles.emptyState}>
          <p>
            {searchTerm 
              ? `No apartments found matching "${searchTerm}".` 
              : 'No apartments currently available.'}
          </p>
          {hasActiveFilters && (
            <button 
              type="button" 
              onClick={handleFilterReset} 
              className={`${styles.btn} ${styles.btnOutlineSecondary}`}
              style={{ marginTop: '1rem' }}
            >
              Clear filters
            </button>
          )}
        </div>
      )}
      
      {/* Bottom CTA */}
      <div className={styles.bottomCta}>
        {isUserOnly && (
          <Link to="/owner/new" className={`${styles.btn} ${styles.btnOutlineSuccess} ${styles.pulseBtn}`}>
            <Plus size={18} /> Become an Owner to List
          </Link>
        )}
        {isOwner && (
          <Link to="/listings/new" className={`${styles.btn} ${styles.btnOutlineSuccess} ${styles.pulseBtn}`}>
            <Plus size={18} /> Create New Apartment
          </Link>
        )}
      </div>
    </div>
  );
}
