import React, { useState, useEffect, useRef } from 'react';
import { TrendingUp, Check, ChevronDown, ChevronUp, Database, ExternalLink } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import { InfoTooltip } from '../../../shared/components/InfoTooltip';
import styles from './PriceSuggestionWidget.module.css';

/**
 * PriceSuggestionWidget
 * -------------------
 * Provides property owners with real-time rental price suggestions based on:
 * 1. Time-series Auto-ARIMA micro-forecasts (pmdarima)
 * 2. Scraped residential rental market benchmarks
 * 3. Feature-level micro-adjustments (size tier, floor, construction era, prime area)
 */
export default function PriceSuggestionWidget({ formData, onApplyPrice }) {
  const [suggestion, setSuggestion] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showDetails, setShowDetails] = useState(false);
  const debounceTimerRef = useRef(null);

  const sizeNum = Number(formData.sizeM2);
  const hasValidSize = !isNaN(sizeNum) && sizeNum >= 5 && sizeNum <= 1000;

  useEffect(() => {
    if (!hasValidSize) {
      setSuggestion(null);
      setError(null);
      return;
    }

    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }

    debounceTimerRef.current = setTimeout(async () => {
      setLoading(true);
      setError(null);

      try {
        const query = new URLSearchParams();
        query.append('sizeM2', formData.sizeM2);
        if (formData.propertyType) query.append('propertyType', formData.propertyType);
        if (formData.address) query.append('address', formData.address);
        if (formData.floor !== '') query.append('floor', formData.floor);
        if (formData.yearBuilt !== '') query.append('yearBuilt', formData.yearBuilt);
        if (formData.bedrooms !== '') query.append('bedrooms', formData.bedrooms);
        if (formData.bathrooms !== '') query.append('bathrooms', formData.bathrooms);

        const data = await apiClient(`/listings/price-suggestion?${query.toString()}`);
        setSuggestion(data);
      } catch {
        setError('Unable to calculate price suggestion at this time.');
      } finally {
        setLoading(false);
      }
    }, 350);

    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, [
    formData.sizeM2,
    formData.propertyType,
    formData.address,
    formData.floor,
    formData.yearBuilt,
    formData.bedrooms,
    formData.bathrooms,
    hasValidSize
  ]);

  if (!hasValidSize) {
    return null;
  }

  const currentPriceNum = Number(formData.price);
  const hasCurrentPrice = formData.price !== '' && !isNaN(currentPriceNum) && currentPriceNum > 0;
  const isApplied = suggestion && hasCurrentPrice && currentPriceNum === suggestion.suggestedPrice;

  // Determine market position of user's current price
  let statusBadge = null;
  if (suggestion && hasCurrentPrice) {
    if (currentPriceNum < suggestion.minPrice) {
      statusBadge = (
        <span className={`${styles.statusBadge} ${styles.statusBelow}`}>
          Below Market Range: Attractive rate for quick rental
        </span>
      );
    } else if (currentPriceNum > suggestion.maxPrice) {
      statusBadge = (
        <span className={`${styles.statusBadge} ${styles.statusAbove}`}>
          Above Market Range: Premium listing; may take longer to lease
        </span>
      );
    } else {
      statusBadge = (
        <span className={`${styles.statusBadge} ${styles.statusCompetitive}`}>
          Competitive Market Rate: Aligned with current forecasts & market data
        </span>
      );
    }
  }

  return (
    <div className={styles.widgetContainer}>
      <div className={styles.widgetHeader}>
        <div className={styles.widgetTitleGroup}>
          <h4 className={styles.widgetTitle}>
            Market Price Suggestion
            <InfoTooltip text="Automated rental price suggestion calculated by combining weekly Athens time-series projections with regional market benchmarks." />
          </h4>
        </div>
        <div className={styles.widgetActions}>
          {loading && <span className={styles.loadingSpinner} title="Calculating..." />}
          <a
            href="/market-trends"
            target="_blank"
            rel="noopener noreferrer"
            className={styles.macroTrendsLink}
            title="Open Market Intelligence & Macro Trends in a new tab"
          >
            <span>Macro Trends</span>
            <ExternalLink size={13} className={styles.macroTrendsIcon} />
          </a>
        </div>
      </div>

      {error && <div style={{ color: '#ef4444', fontSize: '0.85rem' }}>{error}</div>}

      {suggestion && (
        <>
          <div className={styles.priceHighlightBox}>
            <div>
              <div className={styles.suggestedPriceBig}>
                €{suggestion.suggestedPrice}
                <span className={styles.perM2Tag}>
                  / month (€{suggestion.suggestedPricePerM2}/m²)
                </span>
              </div>
              <div className={styles.rangeText}>
                Estimated Market Range {suggestion.marginPercentage ? `(±${suggestion.marginPercentage}%)` : ''}
                <InfoTooltip text="Estimated rental price range based on the suggested price and property-specific adjustments." />
                {': '}
                <span className={styles.rangeValues}>
                  €{suggestion.minPrice} - €{suggestion.maxPrice} / mo
                </span>
              </div>
            </div>

            <button
              type="button"
              onClick={() => onApplyPrice(suggestion.suggestedPrice)}
              className={`${styles.applyBtn} ${isApplied ? styles.applyBtnApplied : ''}`}
            >
              {isApplied ? (
                <>
                  <Check size={16} /> Applied
                </>
              ) : (
                <>Apply Suggested Price</>
              )}
            </button>
          </div>

          {statusBadge}

          {suggestion.appliedAdjustments && suggestion.appliedAdjustments.length > 0 && (
            <div className={styles.adjustmentsList}>
              {suggestion.appliedAdjustments.map((adj, i) => (
                <span key={i} className={styles.adjustmentTag}>
                  {adj}
                </span>
              ))}
            </div>
          )}

          <button
            type="button"
            className={styles.toggleDetailsBtn}
            onClick={() => setShowDetails(!showDetails)}
          >
            {showDetails ? (
              <>
                Hide Model Insights <ChevronUp size={14} />
              </>
            ) : (
              <>
                View Model & Scraped Data Insights <ChevronDown size={14} />
              </>
            )}
          </button>

          {showDetails && (
            <div className={styles.detailsContent}>
              <div className={styles.insightCard}>
                <div className={styles.insightTitle}>
                  <TrendingUp size={14} className={styles.forecastIcon} />
                  <span>
                    {suggestion.arimaForecast?.modelStatus === 'STATISTICAL_BASELINE'
                      ? 'Statistical Baseline (Historical Trend)'
                      : `Auto-ARIMA Micro-Forecast (${suggestion.arimaForecast?.horizonWeeks || 5}-Week Projection)`}
                  </span>
                  <InfoTooltip text={
                    suggestion.arimaForecast?.modelStatus === 'STATISTICAL_BASELINE'
                      ? "Historical baseline average computed from recent Athens listings when insufficient time-series observations exist for full ARIMA estimation."
                      : "Short-term statistical projection derived from weekly Athens rental data to capture recent pricing momentum."
                  } />
                </div>
                <div>
                  {suggestion.arimaForecast?.modelStatus === 'STATISTICAL_BASELINE' ? (
                    <>
                      Historical Baseline Average:{' '}
                      <strong>€{suggestion.arimaForecast?.predictedPricePerM2}/m²</strong> (Recent variability:{' '}
                      €{suggestion.arimaForecast?.lowerBoundPerM2} - €{suggestion.arimaForecast?.upperBoundPerM2}/m²).{' '}
                      <InfoTooltip text="Directional indicator derived from recent weekly scraped rental listings across Athens.">
                        Trend signal
                      </InfoTooltip>:{' '}
                      <strong>{suggestion.arimaForecast?.trend}</strong> (
                      {suggestion.arimaForecast?.trendPercentage > 0 ? '+' : ''}
                      {suggestion.arimaForecast?.trendPercentage}%).
                    </>
                  ) : (
                    <>
                      Forecasted Price:{' '}
                      <strong>€{suggestion.arimaForecast?.predictedPricePerM2}/m²</strong> ({' '}
                      <InfoTooltip text="95% Prediction Interval: a model-based range representing the uncertainty around the projected weekly rental price per square meter, based on recent observations.">
                        95% PI
                      </InfoTooltip>:{' '}
                      €{suggestion.arimaForecast?.lowerBoundPerM2} - €{suggestion.arimaForecast?.upperBoundPerM2}/m²).{' '}
                      <InfoTooltip text="Directional indicator derived from recent weekly scraped rental listings across Athens.">
                        Trend signal
                      </InfoTooltip>:{' '}
                      <strong>{suggestion.arimaForecast?.trend}</strong> (
                      {suggestion.arimaForecast?.trendPercentage > 0 ? '+' : ''}
                      {suggestion.arimaForecast?.trendPercentage}%).
                    </>
                  )}
                </div>
              </div>

              <div className={`${styles.insightCard} ${styles.insightCardScraped}`}>
                <div className={styles.insightTitle}>
                  <Database size={14} className={styles.benchmarkIcon} />
                  <span>Scraped Real Estate Market Benchmark</span>
                  <InfoTooltip text="Market benchmark calculated from active scraped Athens listings for the selected property category." />
                  {suggestion.marketBenchmark?.propertyTypeStatus === 'MEASURED' ? (
                    <span style={{ fontSize: '0.75rem', fontWeight: 'normal', color: '#10b981', marginLeft: '6px' }}>
                      (Measured: {suggestion.marketBenchmark?.propertyTypeSampleCount} listings)
                      <InfoTooltip size={12} text="Benchmark computed directly from active scraped listings in the dataset." />
                    </span>
                  ) : (
                    <span style={{ fontSize: '0.75rem', fontWeight: 'normal', color: '#f59e0b', marginLeft: '6px' }}>
                      (Domain Prior Estimate)
                      <InfoTooltip size={12} text="Fewer than 5 comparable properties of this type are available in the active dataset, so the system uses a reference baseline instead of a measured market benchmark." />
                    </span>
                  )}
                </div>
                <div>
                  {suggestion.marketBenchmark?.propertyTypeStatus === 'MEASURED' ? (
                    <>
                      Derived from{' '}
                      <strong>{suggestion.marketBenchmark?.propertyTypeSampleCount}+ measured listings</strong> for{' '}
                      {formData.propertyType || 'APARTMENT'} in Athens.{' '}
                      Category benchmark:{' '}
                      <strong>€{suggestion.marketBenchmark?.propertyTypeAvgPerM2}/m²</strong> (Overall population avg:{' '}
                      €{suggestion.marketBenchmark?.avgPricePerM2}/m²).
                    </>
                  ) : (
                    <>
                      Category benchmark:{' '}
                      <strong>€{suggestion.marketBenchmark?.propertyTypeAvgPerM2}/m²</strong> (derived from domain baseline prior ratio; {suggestion.marketBenchmark?.propertyTypeSampleCount || 0} listings in dataset). Overall population avg:{' '}
                      €{suggestion.marketBenchmark?.avgPricePerM2}/m².
                    </>
                  )}
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
