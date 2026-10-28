import React, { useEffect, useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  Heart,
  Building2,
  MapPin,
  Bed,
  Bath,
  Maximize,
  ArrowUpToLine,
  Calendar,
  Search,
  Trash2,
  ArrowRight,
  Info,
  Camera
} from 'lucide-react';
import { useFavourites } from '../context/FavouritesContext';
import { useAuth } from '../../auth';
import FavouriteButton from './FavouriteButton';
import PriceDropIndicator, { computeAvgPricePerM2 } from '../../listings/components/PriceDropIndicator';
import { getImageUrl, DEFAULT_LISTING_IMAGE, REMOTE_FALLBACK_IMAGE } from '../../../shared/utils/imageUtils';
import styles from './FavouritesPage.module.css';

export default function FavouritesPage() {
  const {
    favourites,
    favouriteIds,
    fetchFavourites,
    removeFavourite,
    loading,
    guestFavouritesPending,
    importGuestFavourites,
    discardGuestFavourites,
    syncing
  } = useFavourites();
  const { isAuthenticated, user, roles } = useAuth();
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState('newest');

  const isUserOnly = isAuthenticated && roles.includes('USER') && !roles.includes('OWNER') && !roles.includes('ADMIN');

  useEffect(() => {
    const controller = new AbortController();
    fetchFavourites(controller.signal);
    return () => controller.abort();
  }, [fetchFavourites]);

  const avgPricePerM2 = useMemo(() => {
    return computeAvgPricePerM2(favourites);
  }, [favourites]);

  // Filtered and sorted listings
  const displayedListings = useMemo(() => {
    let result = [...favourites];

    // Filter by search keyword
    const term = searchTerm.trim().toLowerCase();
    if (term) {
      result = result.filter(item => {
        const title = (item.title || '').toLowerCase();
        const subtitle = (item.subtitle || '').toLowerCase();
        const description = (item.description || '').toLowerCase();
        const address = (item.address || '').toLowerCase();
        const type = (item.propertyType || '').toLowerCase();
        return title.includes(term) || subtitle.includes(term) || description.includes(term) || address.includes(term) || type.includes(term);
      });
    }

    // Sort listings
    if (sortBy === 'price-asc') {
      result.sort((a, b) => (Number(a.price) || 0) - (Number(b.price) || 0));
    } else if (sortBy === 'price-desc') {
      result.sort((a, b) => (Number(b.price) || 0) - (Number(a.price) || 0));
    } else if (sortBy === 'size-desc') {
      result.sort((a, b) => (Number(b.sizeM2) || 0) - (Number(a.sizeM2) || 0));
    } else {
      // Newest / default: by id descending
      result.sort((a, b) => (Number(b.id) || 0) - (Number(a.id) || 0));
    }

    return result;
  }, [favourites, searchTerm, sortBy]);

  const handleClearAll = () => {
    if (window.confirm('Are you sure you want to remove all properties from your favourites?')) {
      favouriteIds.forEach(id => removeFavourite(id));
    }
  };

  return (
    <div className={styles.container}>
      {/* Page Header */}
      <div className={styles.pageHeader}>
        <div className={styles.titleArea}>
          <div className={styles.headerIconWrapper}>
            <Heart size={26} className={styles.headerIcon} fill="currentColor" />
          </div>
          <div>
            <h1 className={styles.pageTitle}>My Favourite Listings</h1>
            <p className={styles.pageSubtitle}>
              Properties you have saved for easy comparison and quick rental applications
            </p>
          </div>
        </div>

        <div className={styles.headerActions}>
          <span className={styles.countBadge}>
            <Heart size={16} fill="currentColor" />
            {favourites.length} {favourites.length === 1 ? 'Saved Property' : 'Saved Properties'}
          </span>
        </div>
      </div>

      {/* Guest Notice Banner */}
      {!isAuthenticated && favourites.length > 0 && (
        <div className={styles.guestBanner}>
          <div className={styles.guestBannerLeft}>
            <Info size={20} />
            <span>
              You are currently viewing favourites stored in this browser.{' '}
              <Link to="/login" className={styles.bannerLink}>Sign in</Link> or{' '}
              <Link to="/register" className={styles.bannerLink}>create an account</Link> to sync your favourites across all your devices.
            </span>
          </div>
        </div>
      )}

      {/* Explicit Opt-In Guest Favourites Sync Banner */}
      {isAuthenticated && guestFavouritesPending > 0 && (
        <div className={styles.syncBanner} role="region" aria-label="Guest favourites prompt">
          <div className={styles.syncBannerLeft}>
            <div className={styles.syncBannerIconWrapper}>
              <Heart size={20} className={styles.syncBannerIcon} fill="currentColor" />
            </div>
            <div className={styles.syncBannerContent}>
              <h4 className={styles.syncBannerTitle}>Guest Favourites Found</h4>
              <p className={styles.syncBannerDesc}>
                You saved <strong>{guestFavouritesPending}</strong> {guestFavouritesPending === 1 ? 'property' : 'properties'} on this device before signing in.
                Would you like to import {guestFavouritesPending === 1 ? 'it' : 'them'} into your account ({user?.email || user?.username})?
              </p>
            </div>
          </div>
          <div className={styles.syncBannerActions}>
            <button
              type="button"
              onClick={importGuestFavourites}
              disabled={syncing}
              className={styles.syncImportBtn}
            >
              {syncing ? 'Importing...' : 'Import to Account'}
            </button>
            <button
              type="button"
              onClick={discardGuestFavourites}
              disabled={syncing}
              className={styles.syncDiscardBtn}
            >
              Discard
            </button>
          </div>
        </div>
      )}

      {/* Controls Bar */}
      {favourites.length > 0 && (
        <div className={styles.controlsBar}>
          <div className={styles.searchBox}>
            <Search size={18} className={styles.searchIcon} />
            <input
              type="text"
              placeholder="Search favourites by title, address, description, type..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className={styles.searchInput}
            />
          </div>

          <div className={styles.filterGroup}>
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className={styles.sortSelect}
              aria-label="Sort favourites"
            >
              <option value="newest">Sort by: Recently Added</option>
              <option value="price-asc">Sort by: Price: Low to High</option>
              <option value="price-desc">Sort by: Price: High to Low</option>
              <option value="size-desc">Sort by: Size: Largest First</option>
            </select>

            <button
              type="button"
              onClick={handleClearAll}
              className={styles.clearAllBtn}
              title="Remove all favourites"
            >
              <Trash2 size={16} /> Clear All
            </button>
          </div>
        </div>
      )}

      {/* Content Area */}
      {loading ? (
        <div className={styles.loadingContainer}>
          <div className={styles.spinner}></div>
          <p>Loading your favourite listings...</p>
        </div>
      ) : favourites.length === 0 ? (
        <div className={styles.emptyState}>
          <div className={styles.emptyIconWrapper}>
            <Heart size={36} />
          </div>
          <h2 className={styles.emptyTitle}>No favourite listings yet</h2>
          <p className={styles.emptyDesc}>
            You haven't saved any apartments to your favourites list yet. Browse our verified listings and click the heart icon on any property to save it here.
          </p>
          <Link to="/listings" className={styles.browseBtn}>
            <Building2 size={18} /> Browse Apartments <ArrowRight size={16} />
          </Link>
        </div>
      ) : displayedListings.length === 0 ? (
        <div className={styles.emptyState}>
          <div className={styles.emptyIconWrapper}>
            <Search size={36} />
          </div>
          <h2 className={styles.emptyTitle}>No matching favourites</h2>
          <p className={styles.emptyDesc}>
            No saved properties match your search for "{searchTerm}".
          </p>
          <button
            type="button"
            onClick={() => setSearchTerm('')}
            className={styles.browseBtn}
          >
            Clear Search
          </button>
        </div>
      ) : (
        <div className={styles.grid}>
          {displayedListings.map(listing => {
            const hasImages = listing.images && listing.images.length > 0;
            const cardImageUrl = hasImages ? (getImageUrl(listing.images[0]) || DEFAULT_LISTING_IMAGE) : DEFAULT_LISTING_IMAGE;
            return (
              <div key={listing.id} className={styles.card}>
                <div className={styles.imageWrapper}>
                  <FavouriteButton listing={listing} variant="card" />
                  <Link to={`/listings/${listing.id}`} className={styles.imageLink} aria-label={`View details for ${listing.title}`}>
                    <img
                      src={cardImageUrl}
                      alt={hasImages ? listing.title : 'Generic demo preview'}
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
                  {hasImages ? (
                    listing.images.length > 1 && (
                      <span className={styles.imageCountBadge} title={`${listing.images.length} photos available`}>
                        <Camera size={13} /> {listing.images.length}
                      </span>
                    )
                  ) : (
                    <span className={styles.demoBadge} title="Demo image - no photos uploaded">
                      <Camera size={12} /> Demo Preview
                    </span>
                  )}
                </div>

                <div className={styles.cardBody}>
                <div className={styles.cardHeader}>
                  <h3 className={styles.cardTitle} title={listing.title}>
                    {listing.title}
                  </h3>
                  <div className={styles.priceContainer}>
                    <span className={styles.priceBadge}>{listing.price} € /mo</span>
                    <PriceDropIndicator listing={listing} avgPricePerM2={avgPricePerM2} />
                  </div>
                </div>

                {listing.subtitle && (
                  <p className={styles.cardSubtitle} title={listing.subtitle}>
                    {listing.subtitle}
                  </p>
                )}

                {listing.description && (!listing.subtitle || listing.description !== listing.subtitle) && (
                  <p className={styles.cardDescription} title={listing.description}>
                    {listing.description}
                  </p>
                )}

                <div className={styles.addressRow}>
                  <MapPin size={15} style={{ flexShrink: 0 }} />
                  <span className={styles.addressText} title={listing.address}>{listing.address}</span>
                </div>

                <div className={styles.specsGrid}>
                  <div className={styles.specItem}>
                    <Bed size={15} className={styles.specIcon} />
                    <span>{listing.bedrooms || 0} Beds</span>
                  </div>
                  <div className={styles.specItem}>
                    <Bath size={15} className={styles.specIcon} />
                    <span>{listing.bathrooms || 0} Baths</span>
                  </div>
                  <div className={styles.specItem}>
                    <Maximize size={15} className={styles.specIcon} />
                    <span>{listing.sizeM2 || 0} m²</span>
                  </div>
                  <div className={styles.specItem}>
                    <ArrowUpToLine size={15} className={styles.specIcon} />
                    <span>Floor {listing.floor || 'G'}</span>
                  </div>
                  {listing.yearBuilt && (
                    <div className={styles.specItem}>
                      <Calendar size={15} className={styles.specIcon} />
                      <span>Built {listing.yearBuilt}</span>
                    </div>
                  )}
                  {listing.propertyType && (
                    <div className={styles.specItem}>
                      <Building2 size={15} className={styles.specIcon} />
                      <span>{listing.propertyType}</span>
                    </div>
                  )}
                </div>

                <div className={styles.cardFooter}>
                  <Link
                    to={`/listings/${listing.id}`}
                    className={styles.viewBtn}
                  >
                    <Search size={16} /> View Details
                  </Link>

                  {isUserOnly && !listing.rented && listing.status !== 'RENTED' && (
                    <Link
                      to={`/tenant/rent/${listing.id}`}
                      className={styles.applyBtn}
                    >
                      Apply for Rental
                    </Link>
                  )}
                </div>
              </div>
            </div>
          );
        })}
        </div>
      )}
    </div>
  );
}
