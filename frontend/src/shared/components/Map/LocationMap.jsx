import React, { useEffect, useRef, useState, useCallback } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { RotateCcw, ExternalLink, AlertCircle } from 'lucide-react';
import styles from './LocationMap.module.css';

/**
 * Location map component powered by Leaflet and OpenStreetMap.
 *
 * Supports approximate radius circle for listings, point marker for contacts,
 * theme-adaptive CSS filters, and viewport-based lazy loading.
 */
export default function LocationMap({
  address,
  coordinates,
  title = 'Property Location',
  subtitle,
  approximate = true,
  height = 380,
  zoom = 15,
}) {
  const containerRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const [mapVisible, setMapVisible] = useState(false);
  const [coords, setCoords] = useState(coordinates || null);
  const [loading, setLoading] = useState(!coordinates && !!address);
  const [error, setError] = useState(false);
  const [showHint, setShowHint] = useState(false);

  const isMac = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/i.test(navigator.userAgent || navigator.platform);

  // Lazy-loading: only initialize map when the section comes into view
  const setLazyRef = useCallback((node) => {
    if (!node) return;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setMapVisible(true);
          observer.disconnect();
        }
      },
      { rootMargin: '200px' }
    );
    observer.observe(node);
  }, []);

  // Update coords if props change directly
  useEffect(() => {
    if (coordinates?.lat && coordinates?.lon) {
      setCoords({ lat: coordinates.lat, lon: coordinates.lon });
      setLoading(false);
      setError(false);
    }
  }, [coordinates]);

  // Geocode address via Nominatim if coordinates not pre-supplied
  useEffect(() => {
    if (!mapVisible || coords || !address) return;

    let cancelled = false;
    setLoading(true);
    setError(false);

    const query = address.trim();

    async function geocode() {
      try {
        let res = await fetch(
          `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&limit=1&accept-language=en`
        );
        let data = await res.json();

        // If exact address fails and contains commas, try broader part
        if ((!data || data.length === 0) && query.includes(',')) {
          const broader = query.split(',').slice(1).join(',').trim();
          if (broader) {
            res = await fetch(
              `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(broader)}&format=json&limit=1&accept-language=en`
            );
            data = await res.json();
          }
        }

        if (cancelled) return;

        if (data && data.length > 0) {
          setCoords({
            lat: parseFloat(data[0].lat),
            lon: parseFloat(data[0].lon),
          });
        } else {
          setError(true);
        }
      } catch (err) {
        if (!cancelled) setError(true);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    geocode();

    return () => {
      cancelled = true;
    };
  }, [mapVisible, address, coords]);

  // Initialize and mount Leaflet map
  useEffect(() => {
    if (!mapVisible || !coords || !containerRef.current) return;

    // Clean up any existing map instance on re-render
    if (mapInstanceRef.current) {
      mapInstanceRef.current.remove();
      mapInstanceRef.current = null;
    }

    const map = L.map(containerRef.current, {
      center: [coords.lat, coords.lon],
      zoom: zoom,
      scrollWheelZoom: true,
      zoomControl: false,
      attributionControl: true,
    });

    mapInstanceRef.current = map;

    // Base Tile Layer: Official OpenStreetMap
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      className: styles.mapTileLayer,
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors',
    }).addTo(map);

    // Zoom Controls
    L.control.zoom({ position: 'bottomright' }).addTo(map);

    // Approximate area radius circle
    if (approximate) {
      L.circle([coords.lat, coords.lon], {
        radius: 220,
        color: '#3b82f6',
        fillColor: '#3b82f6',
        fillOpacity: 0.12,
        weight: 1.5,
        dashArray: '5, 5',
      }).addTo(map);
    }

    // Modern SVG Pin Marker
    const iconHtml = `
      <div class="${styles.markerPinWrapper}">
        <div class="${styles.markerPulse}"></div>
        <div class="${styles.markerBadge}">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
            <polyline points="9 22 9 12 15 12 15 22"></polyline>
          </svg>
        </div>
      </div>
    `;

    const customMarker = L.divIcon({
      className: styles.markerContainer,
      html: iconHtml,
      iconSize: [44, 44],
      iconAnchor: [22, 22],
      popupAnchor: [0, -20],
    });

    const marker = L.marker([coords.lat, coords.lon], { icon: customMarker }).addTo(map);

    const popupContent = `
      <div class="${styles.popupContent}">
        <strong class="${styles.popupTitle}">${title}</strong>
        ${subtitle ? `<span class="${styles.popupSubtitle}">${subtitle}</span>` : ''}
        ${approximate ? `<span class="${styles.popupNotice}">Approximate location</span>` : ''}
      </div>
    `;

    marker.bindPopup(popupContent, {
      closeButton: false,
      offset: [0, -12],
    });

    // Invalidate size once rendered to ensure tiles fill container
    setTimeout(() => {
      map.invalidateSize();
    }, 150);

    // Ctrl+Scroll Handling: regular scroll does not zoom the map, but Ctrl+Scroll does
    const container = containerRef.current;
    let hintTimer = null;

    const handleWheel = (e) => {
      if (e.ctrlKey || e.metaKey) {
        // Zooming authorized: prevent default full-page browser scaling
        e.preventDefault();
        setShowHint(false);
        if (hintTimer) clearTimeout(hintTimer);
      } else {
        // Regular scroll: stop Leaflet wheel handler to let the page scroll naturally
        e.stopImmediatePropagation();
        setShowHint(true);
        if (hintTimer) clearTimeout(hintTimer);
        hintTimer = setTimeout(() => {
          setShowHint(false);
        }, 1200);
      }
    };

    container.addEventListener('wheel', handleWheel, { capture: true, passive: false });

    return () => {
      if (hintTimer) clearTimeout(hintTimer);
      container.removeEventListener('wheel', handleWheel, { capture: true });
      map.remove();
      mapInstanceRef.current = null;
    };
  }, [mapVisible, coords, approximate, zoom, title, subtitle]);

  const handleResetCenter = () => {
    if (mapInstanceRef.current && coords) {
      mapInstanceRef.current.flyTo([coords.lat, coords.lon], zoom, { duration: 0.8 });
    }
  };

  return (
    <div className={styles.mapWrapper} ref={setLazyRef} style={{ height }}>
      {/* Interactive Leaflet Map Canvas */}
      <div ref={containerRef} className={styles.mapCanvas} />

      {/* Ctrl + Scroll Gesture Hint Overlay */}
      {showHint && (
        <div className={styles.gestureHintOverlay}>
          <span>{isMac ? 'Use ⌘ + scroll to zoom the map' : 'Use Ctrl + scroll to zoom the map'}</span>
        </div>
      )}

      {/* Floating Toolbar Controls */}
      {coords && !loading && !error && (
        <div className={styles.mapToolbar}>
          <button
            type="button"
            onClick={handleResetCenter}
            className={styles.toolBtn}
            title="Reset center"
            aria-label="Reset map view to marker"
          >
            <RotateCcw size={16} />
          </button>
          <a
            href={`https://www.openstreetmap.org/?mlat=${coords.lat}&mlon=${coords.lon}#map=16/${coords.lat}/${coords.lon}`}
            target="_blank"
            rel="noopener noreferrer"
            className={styles.toolBtn}
            title="Open in OpenStreetMap"
            aria-label="Open in OpenStreetMap"
          >
            <ExternalLink size={16} />
          </a>
        </div>
      )}

      {/* Loading State Overlay */}
      {(!mapVisible || loading) && (
        <div className={styles.loadingOverlay}>
          <div className={styles.spinner} />
          <span>Locating on map…</span>
        </div>
      )}

      {/* Error / Fallback State */}
      {error && !loading && (
        <div className={styles.errorOverlay}>
          <AlertCircle size={28} className={styles.errorIcon} />
          <span>Approximate map location unavailable for this address.</span>
          {address && (
            <a
              href={`https://www.openstreetmap.org/search?query=${encodeURIComponent(address)}`}
              target="_blank"
              rel="noopener noreferrer"
              className={styles.fallbackLink}
            >
              Search address on OpenStreetMap ↗
            </a>
          )}
        </div>
      )}
    </div>
  );
}
