import React, { useState, useEffect, useCallback } from 'react';
import {
  RefreshCw,
  Users,
  Building2,
  Clock,
  CheckCircle,
  Activity,
  Database
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
          System Status
        </h1>
      </div>

      <hr className={styles.divider} />

      {/* Section 1: Analytics Data */}
      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>
            <Database size={22} className={styles.sectionIcon} />
            Analytics Data
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
              <h3 className={styles.cardTitle}>Macro Forecast</h3>
              <div className={styles.statusIndicator}>
                <span
                  className={`${styles.statusDot} ${status?.macroPredictionsLoaded ? styles.dotGreen : styles.dotRed}`}
                  aria-hidden="true"
                />
                <span className={styles.statusLabel}>
                  {status?.macroPredictionsLoaded ? 'Loaded' : 'Not Loaded'}
                </span>
              </div>
            </div>
            <div className={styles.cardBody}>
              <div className={styles.dataRow}>
                <span className={styles.dataLabel}>File Path:</span>
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
                <span className={styles.dataLabel}>Age:</span>
                <span className={styles.dataValue}>
                  {status?.macroPredictionsAge || 'N/A'}
                </span>
              </div>
            </div>
          </div>

          {/* Price Predictions Card */}
          <div className={styles.analyticsCard}>
            <div className={styles.cardHeader}>
              <h3 className={styles.cardTitle}>Price Predictions</h3>
              <div className={styles.statusIndicator}>
                <span
                  className={`${styles.statusDot} ${status?.pricePredictionsLoaded ? styles.dotGreen : styles.dotRed}`}
                  aria-hidden="true"
                />
                <span className={styles.statusLabel}>
                  {status?.pricePredictionsLoaded ? 'Loaded' : 'Not Loaded'}
                </span>
              </div>
            </div>
            <div className={styles.cardBody}>
              <div className={styles.dataRow}>
                <span className={styles.dataLabel}>File Path:</span>
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
                <span className={styles.dataLabel}>Age:</span>
                <span className={styles.dataValue}>
                  {status?.pricePredictionsAge || 'N/A'}
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Section 2: Platform Statistics */}
      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>
            <Activity size={22} className={styles.sectionIcon} />
            Platform Statistics
          </h2>
        </div>

        <div className={styles.statsGrid}>
          {/* Total Users */}
          <div className={styles.statTile}>
            <div className={styles.statIconWrapper}>
              <Users size={24} className={styles.statIcon} />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statNumber}>{status?.totalUsers ?? 0}</span>
              <span className={styles.statLabel}>Total Users</span>
            </div>
          </div>

          {/* Total Listings */}
          <div className={styles.statTile}>
            <div className={styles.statIconWrapper}>
              <Building2 size={24} className={styles.statIcon} />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statNumber}>{status?.totalListings ?? 0}</span>
              <span className={styles.statLabel}>Total Listings</span>
            </div>
          </div>

          {/* Pending Listings */}
          <div className={styles.statTile}>
            <div className={styles.statIconWrapper}>
              <Clock size={24} className={styles.statIcon} />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statNumber}>{status?.pendingListings ?? 0}</span>
              <span className={styles.statLabel}>Pending Listings</span>
              <span className={styles.statSubtext}>Awaiting approval</span>
            </div>
          </div>

          {/* Active Listings */}
          <div className={styles.statTile}>
            <div className={styles.statIconWrapper}>
              <CheckCircle size={24} className={styles.statIcon} />
            </div>
            <div className={styles.statInfo}>
              <span className={styles.statNumber}>{status?.activeListings ?? 0}</span>
              <span className={styles.statLabel}>Active Listings</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
