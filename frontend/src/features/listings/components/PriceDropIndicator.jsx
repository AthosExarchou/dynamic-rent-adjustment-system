import React from 'react';
import { TrendingDown } from 'lucide-react';
import styles from './PriceDropIndicator.module.css';

/**
 * Computes the average pricePerM2 across all listings (ignoring nulls/zeros).
 * @param {Array} listings
 * @returns {number|null}
 */
export function computeAvgPricePerM2(listings) {
  if (!listings || listings.length === 0)
    return null;

  const valid = listings.filter(l => l.pricePerM2 > 0);
  if (valid.length === 0)
    return null;

  return valid.reduce((sum, l) => sum + l.pricePerM2, 0) / valid.length;
}

/**
 * Returns true if the listing was updated (price lowered) within the last `days` days.
 * @param {string} updatedAt  - ISO date string from the backend
 * @param {number} [days=30]
 * @returns {boolean}
 */
export function isRecentlyUpdated(updatedAt, days = 30) {
  if (!updatedAt) return false;
  const updated = new Date(updatedAt);
  const cutoff = new Date();
  cutoff.setDate(cutoff.getDate() - days);
  return updated >= cutoff;
}

/**
 * Returns the "price signal" for a single listing given the average pricePerM2.
 *  - 'recent_drop':   updated within last 30 days AND price is ≤ avg
 *  - 'below_market':  price is strictly below avg (but no recent change)
 *  - null:            no signal
 *
 * @param {Object} listing
 * @param {number|null} avgPricePerM2
 * @returns {'recent_drop'|'below_market'|null}
 */
export function getPriceSignal(listing, avgPricePerM2) {
  if (!avgPricePerM2 || !listing.pricePerM2) return null;
  const belowAvg = listing.pricePerM2 < avgPricePerM2;
  if (!belowAvg) return null;
  if (isRecentlyUpdated(listing.updatedAt)) return 'recent_drop';
  return 'below_market';
}

/**
 * A small indicator badge that shows a green downward-trending arrow when a
 * listing's price was recently lowered OR is below the current market average.
 *
 * Props:
 *   listing      {Object}       - The listing object
 *   avgPricePerM2 {number|null} - Pre-computed average price/m² across all listings
 */
export default function PriceDropIndicator({ listing, avgPricePerM2 }) {
  const signal = getPriceSignal(listing, avgPricePerM2);
  if (!signal) return null;

  const isRecentDrop = signal === 'recent_drop';
  const label = isRecentDrop ? 'Price recently lowered' : 'Below market average';
  const tooltip = isRecentDrop
    ? `Price recently lowered · ${listing.pricePerM2} €/m² vs avg ${Math.round(avgPricePerM2)} €/m²`
    : `Below market average · ${listing.pricePerM2} €/m² vs avg ${Math.round(avgPricePerM2)} €/m²`;

  return (
    <span
      className={`${styles.badge} ${isRecentDrop ? styles.recentDrop : styles.belowMarket}`}
      title={tooltip}
      aria-label={tooltip}
    >
      <TrendingDown size={13} className={styles.arrow} />
      {label}
    </span>
  );
}
