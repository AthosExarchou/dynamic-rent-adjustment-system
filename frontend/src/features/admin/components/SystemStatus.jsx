import React, { useState, useEffect, useCallback } from 'react';
import {
  RefreshCw,
  Users,
  Building2,
  Clock,
  CheckCircle,
  Activity,
  Database,
  Globe,
  Home,
  UserCheck,
  Key,
  XCircle
} from 'lucide-react';
import apiClient from '../../../shared/api/client';
import { useToast } from '../../../shared/components/Toast';
import styles from './SystemStatus.module.css';

export default function SystemStatus() {
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloading, setReloading] = useState(false);
  const toast = useToast();

  const fetchStatus = useCallback(async (signal) => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiClient('/admin/system/status', signal ? { signal } : {});
      setStatus(data);
    } catch (err) {
      if (err?.name === 'AbortError') return;
      setError(err?.message || 'Failed to fetch system status.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    fetchStatus(controller.signal);
    return () => controller.abort();
  }, [fetchStatus]);

  const handleReloadForecast = async () => {
    if (reloading) return;
    try {
      setReloading(true);
      const updatedStatus = await apiClient('/admin/system/reload-forecast', {
        method: 'POST'
      });
      if (updatedStatus) {
        setStatus(updatedStatus);
      } else {
        await fetchStatus();
      }
      toast.success('Forecast data reloaded');
    } catch (err) {
      toast.error(err?.message || 'Failed to reload forecast data');
    } finally {
      setReloading(false);
    }
  };

  const totalListings = status?.totalListings || 0;
  const localListings = status?.localListings || 0;
  const externalListings = status?.externalListings || 0;

  const localPct = totalListings > 0 ? Math.round((localListings / totalListings) * 100) : 0;
  const externalPct = totalListings > 0 ? (100 - localPct) : 0;

  if (loading) {
    return (
      <div className={styles.container}>
        <div className={styles.loadingContainer}>
          <div className={styles.spinner}></div>
          <p>Loading system status...</p>
        </div>
      </div>
    );
  }

  if (error && !status) {
    return (
      <div className={styles.container}>
        <div className={styles.errorContainer}>
          <h3 className={styles.errorTitle}>Error Loading System Status</h3>
          <p className={styles.errorMessage}>{error}</p>
          <button onClick={() => fetchStatus()} className={styles.retryBtn}>
            <RefreshCw size={16} />
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1 className={styles.title}>
          <Activity className={styles.titleIcon} size={28} />
          System Status & Observability
        </h1>
      </div>

      <hr className={styles.divider} />

      {/* Section 1: Analytics Data */}
      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>
            <Database size={22} className={styles.sectionIcon} />
            Analytics & Forecasting Engine
          </h2>
          <button
            onClick={handleReloadForecast}
            disabled={reloading}
            className={styles.reloadBtn}
          >
            <RefreshCw size={16} className={reloading ? styles.spinning : ''} />
            {reloading ? 'Reloading...' : 'Force Reload Forecast Data'}
          </button>
        </div>

        <div className={styles.analyticsGrid}>
          {/* Macro Forecast Card */}
          <div className={styles.analyticsCard}>
            <div className={styles.cardHeader}>
              <h3 className={styles.cardTitle}>Macroeconomic Forecast (ARIMA)</h3>
              <div className={styles.statusIndicator}>
                <span
                  className={`${styles.statusDot} ${status?.macroPredictionsLoaded ? styles.dotGreen : styles.dotRed}`}
                  aria-hidden="true"
                />
                <span className={styles.statusLabel}>
                  {status?.macroPredictionsLoaded ? 'In Memory (Active)' : 'Not Loaded'}
                </span>
              </div>
            </div>
            <div className={styles.cardBody}>
              <div className={styles.dataRow}>
                <span className={styles.dataLabel}>File Source:</span>
                <span
                  className={styles.dataValueTruncated}
                  title={status?.macroPredictionsFile || 'Not found'}
                >
                  {status?.macroPredictionsFile || 'Not found'}
                </span>
              </div>
              <div className={styles.dataRow}>
                <span className={styles.dataLabel}>Last Modified:</span>
                <span className={styles.dataValue}>
                  {status?.macroPredictionsLastModified || 'Not found'}
                </span>
              </div>
              <div className={styles.dataRow}>
                <span className={styles.dataLabel}>Freshness:</span>
                <span className={styles.dataValue}>
                  {status?.macroPredictionsAge || 'N/A'}
                </span>
              </div>
            </div>
          </div>

          {/* Price Predictions Card */}
          <div className={styles.analyticsCard}>
            <div className={styles.cardHeader}>
              <h3 className={styles.cardTitle}>Price Suggestion Model</h3>
              <div className={styles.statusIndicator}>
                <span
                  className={`${styles.statusDot} ${status?.pricePredictionsLoaded ? styles.dotGreen : styles.dotRed}`}
                  aria-hidden="true"
                />
                <span className={styles.statusLabel}>
                  {status?.pricePredictionsLoaded ? 'In Memory (Active)' : 'Not Loaded'}
                </span>
              </div>
            </div>
            <div className={styles.cardBody}>
              <div className={styles.dataRow}>
                <span className={styles.dataLabel}>File Source:</span>
                <span
                  className={styles.dataValueTruncated}
                  title={status?.pricePredictionsFile || 'Not found'}
                >
                  {status?.pricePredictionsFile || 'Not found'}
                </span>
              </div>
              <div className={styles.dataRow}>
                <span className={styles.dataLabel}>Last Modified:</span>
                <span className={styles.dataValue}>
                  {status?.pricePredictionsLastModified || 'Not found'}
                </span>
              </div>
              <div className={styles.dataRow}>
                <span className={styles.dataLabel}>Freshness:</span>
                <span className={styles.dataValue}>
                  {status?.pricePredictionsAge || 'N/A'}
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Section 2: Listings Breakdown & Origin Comparison */}
      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>
            <Building2 size={22} className={styles.sectionIcon} />
            Listing Inventory & Origin Breakdown
          </h2>
        </div>

        {/* Origin Comparison Panel */}
        <div className={styles.originComparisonCard}>
          <div className={styles.originHeader}>
            <div className={styles.totalListingsBadge}>
              <span className={styles.totalCount}>{totalListings}</span>
              <span className={styles.totalLabel}>Total Properties in Database</span>
            </div>

            <div className={styles.originPills}>
              <div className={styles.originPillLocal}>
                <Home size={16} />
                <span>Local Platform: <strong>{localListings}</strong> ({localPct}%)</span>
              </div>
              <div className={styles.originPillExternal}>
                <Globe size={16} />
                <span>External Scraped: <strong>{externalListings}</strong> ({externalPct}%)</span>
              </div>
            </div>
          </div>

          {/* Ratio bar */}
          <div className={styles.splitBarContainer} title={`Local: ${localListings} (${localPct}%) | External: ${externalListings} (${externalPct}%)`}>
            <div
              className={styles.splitBarLocal}
              style={{ width: `${totalListings > 0 ? localPct : 50}%` }}
            />
            <div
              className={styles.splitBarExternal}
              style={{ width: `${totalListings > 0 ? externalPct : 50}%` }}
            />
          </div>
          <div className={styles.splitBarLabels}>
            <span className={styles.labelLocalText}>● Local Platform Listings ({localPct}%)</span>
            <span className={styles.labelExternalText}>● External Market Listings ({externalPct}%)</span>
          </div>
        </div>

        {/* Listing Status Grid */}
        <div className={styles.statsGrid}>
          {/* Active Listings */}
          <div className={styles.statTile}>
            <div className={`${styles.statIconWrapper} ${styles.iconApproved}`}>
              <CheckCircle size={22} />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statNumber}>{status?.activeListings ?? 0}</span>
              <span className={styles.statLabel}>Active Listings</span>
              <span className={styles.statSubtext}>Approved & visible to public</span>
            </div>
          </div>

          {/* Pending Listings */}
          <div className={styles.statTile}>
            <div className={`${styles.statIconWrapper} ${styles.iconPending}`}>
              <Clock size={22} />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statNumber}>{status?.pendingListings ?? 0}</span>
              <span className={styles.statLabel}>Pending Approval</span>
              <span className={styles.statSubtext}>Awaiting administrator review</span>
            </div>
          </div>

          {/* Rented Listings */}
          <div className={styles.statTile}>
            <div className={`${styles.statIconWrapper} ${styles.iconRented}`}>
              <Key size={22} />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statNumber}>{status?.rentedListings ?? 0}</span>
              <span className={styles.statLabel}>Rented / Leased</span>
              <span className={styles.statSubtext}>Active lease agreements</span>
            </div>
          </div>

          {/* Rejected Listings */}
          <div className={styles.statTile}>
            <div className={`${styles.statIconWrapper} ${styles.iconRejected}`}>
              <XCircle size={22} />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statNumber}>{status?.rejectedListings ?? 0}</span>
              <span className={styles.statLabel}>Rejected / Archived</span>
              <span className={styles.statSubtext}>Did not meet guidelines</span>
            </div>
          </div>
        </div>
      </section>

      {/* Section 3: User Accounts & Roles */}
      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>
            <Users size={22} className={styles.sectionIcon} />
            User Accounts & Community
          </h2>
        </div>

        <div className={styles.statsGrid}>
          {/* Total Users */}
          <div className={styles.statTile}>
            <div className={styles.statIconWrapper}>
              <Users size={22} />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statNumber}>{status?.totalUsers ?? 0}</span>
              <span className={styles.statLabel}>Total Accounts</span>
              <span className={styles.statSubtext}>Registered platform users</span>
            </div>
          </div>

          {/* Verified Owners */}
          <div className={styles.statTile}>
            <div className={styles.statIconWrapper}>
              <Home size={22} />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statNumber}>{status?.totalOwners ?? 0}</span>
              <span className={styles.statLabel}>Property Owners</span>
              <span className={styles.statSubtext}>With active owner profile</span>
            </div>
          </div>

          {/* Verified Tenants */}
          <div className={styles.statTile}>
            <div className={styles.statIconWrapper}>
              <UserCheck size={22} />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statNumber}>{status?.totalTenants ?? 0}</span>
              <span className={styles.statLabel}>Verified Tenants</span>
              <span className={styles.statSubtext}>With active tenant profile</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
