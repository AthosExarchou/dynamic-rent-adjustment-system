import React from 'react';
import { TrendingDown } from 'lucide-react';
import styles from './PriceDropIndicator.module.css';

/**
 * Computes the area-weighted average pricePerM2 across all listings (ignoring nulls/zeros).
 * Falls back to simple mean if size/area data is unavailable.
 * @param {Array} listings
 * @returns {number|null}
 */
export function computeAvgPricePerM2(listings) {
  if (!listings || listings.length === 0)
    return null;

  const valid = listings.filter(l => l && l.pricePerM2 > 0);
  if (valid.length === 0)
    return null;

  const totalArea = valid.reduce((sum, l) => sum + (Number(l.sizeM2) > 0 ? Number(l.sizeM2) : 0), 0);
  if (totalArea > 0) {
    const totalWeightedSum = valid.reduce((sum, l) => {
      const area = Number(l.sizeM2) > 0 ? Number(l.sizeM2) : 0;
      return sum + (l.pricePerM2 * area);
    }, 0);
    return totalWeightedSum / totalArea;
  }

  return valid.reduce((sum, l) => sum + l.pricePerM2, 0) / valid.length;
}

/**
 * Returns the price signal for a single listing given the average pricePerM2:
 *  - 'below_market': price per square meter is strictly below market average
 *  - null:           no signal
 *
 * @param {Object} listing
 * @param {number|null} avgPricePerM2
 * @returns {'below_market'|null}
 */
export function getPriceSignal(listing, avgPricePerM2) {
  if (!avgPricePerM2 || !listing?.pricePerM2) return null;
  return listing.pricePerM2 < avgPricePerM2 ? 'below_market' : null;
}

/**
 * A small indicator badge that shows a green downward-trending arrow when a
 * listing's price per square meter is below the current market average.
 *
 * Props:
 *   listing       {Object}      - The listing object
 *   avgPricePerM2 {number|null} - Pre-computed average price/m² across all listings
 */
export default function PriceDropIndicator({ listing, avgPricePerM2 }) {
  const signal = getPriceSignal(listing, avgPricePerM2);
  if (!signal) return null;

  const tooltip = `Below market average · ${listing.pricePerM2} €/m² vs avg ${Math.round(avgPricePerM2)} €/m²`;

  return (
    <span
      className={`${styles.badge} ${styles.belowMarket}`}
      title={tooltip}
      aria-label={tooltip}
    >
      <TrendingDown size={13} className={styles.arrow} />
      Below market average
    </span>
  );
}
