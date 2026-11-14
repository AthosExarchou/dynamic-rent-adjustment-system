import React, { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { RotateCcw, ExternalLink, AlertCircle } from 'lucide-react';
import styles from './LocationMap.module.css';
import { geocodeAddress } from './geocode';

/**
 * Location map component powered by Leaflet and OpenStreetMap.
 *
 * The map is created imperatively by Leaflet exactly once per mount; prop
 * changes are applied with `setView`/`setLatLng` instead of rebuilding the map,
 * so tiles already in the browser cache are reused.
 *
 * Tiles load only after a pan/zoom gesture settles (`updateWhenZooming: false`)
 * and the filter is applied to the tile pane rather than to each tile image.
 */

const OSM_TILE_URL = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
const OSM_ATTRIBUTION =
  '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors';
const APPROXIMATE_RADIUS_M = 220;

function normalizeCoordinates(value) {
  const lat = Number(value?.lat);
  const lon = Number(value?.lon);
  if (!Number.isFinite(lat) || !Number.isFinite(lon)) return null;
  return { lat, lon };
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function buildPopupHtml({ title, subtitle, approximate }) {
  return `
    <div class="${styles.popupContent}">
      <strong class="${styles.popupTitle}">${escapeHtml(title)}</strong>
      ${subtitle ? `<span class="${styles.popupSubtitle}">${escapeHtml(subtitle)}</span>` : ''}
      ${approximate ? `<span class="${styles.popupNotice}">Approximate location</span>` : ''}
    </div>
  `;
}

const MARKER_HTML = `
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

export default function LocationMap({
  address,
  coordinates,
  title = 'Property Location',
  subtitle,
  approximate = true,
  height = 380,
  zoom = 15,
}) {
  const wrapperRef = useRef(null);
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const markerRef = useRef(null);
  const circleRef = useRef(null);
  const appliedCoordsRef = useRef(null);
  const coordsRef = useRef(null);
  const geocodedAddressRef = useRef(null);
  const hintTimerRef = useRef(null);
  const showHintRef = useRef(false);

  const [mapVisible, setMapVisible] = useState(false);
  const [coords, setCoords] = useState(() => normalizeCoordinates(coordinates));
  const [loading, setLoading] = useState(() => !normalizeCoordinates(coordinates) && Boolean(address));
  const [error, setError] = useState(false);
  const [showHint, setShowHint] = useState(false);

  const isMac =
    typeof navigator !== 'undefined' &&
    /Mac|iPhone|iPad/i.test(navigator.userAgent || navigator.platform);

  coordsRef.current = coords;
  const hasCoords = Boolean(coords);

  // Lazy-loading: only initialize the map when the section comes into view
  useEffect(() => {
    if (mapVisible) return undefined;
    const node = wrapperRef.current;
    if (!node) return undefined;

    if (typeof IntersectionObserver === 'undefined') {
      setMapVisible(true);
      return undefined;
    }

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
    return () => observer.disconnect();
  }, [mapVisible]);

  // Sync externally supplied coordinates
  const propLat = coordinates?.lat;
  const propLon = coordinates?.lon;
  useEffect(() => {
    const normalized = normalizeCoordinates({ lat: propLat, lon: propLon });
    if (!normalized) return;
    setCoords((prev) =>
      prev && prev.lat === normalized.lat && prev.lon === normalized.lon ? prev : normalized
    );
    setLoading(false);
    setError(false);
  }, [propLat, propLon]);

  // Geocode the address when no coordinates were supplied
  useEffect(() => {
    const hasPropCoordinates = Boolean(normalizeCoordinates({ lat: propLat, lon: propLon }));
    if (!mapVisible || !address || hasPropCoordinates) return undefined;
    if (coords && geocodedAddressRef.current === address) return undefined;

    let cancelled = false;
    setLoading(true);
    setError(false);

    geocodeAddress(address)
      .then((result) => {
        if (cancelled) return;
        geocodedAddressRef.current = address;
        if (result) {
          setCoords(result);
        } else {
          setError(true);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [mapVisible, address, coords, propLat, propLon]);

  // Initialize the map exactly once, when it is visible and has a location
  useEffect(() => {
    if (!mapVisible || !coordsRef.current || mapRef.current) return undefined;
    const container = containerRef.current;
    if (!container) return undefined;

    const start = coordsRef.current;

    const map = L.map(container, {
      center: [start.lat, start.lon],
      zoom,
      scrollWheelZoom: true,
      zoomControl: false,
      attributionControl: true,
      // Coalesce wheel input so one gesture does not traverse many zoom levels
      wheelDebounceTime: 100,
      wheelPxPerZoomLevel: 120,
    });
    mapRef.current = map;
    appliedCoordsRef.current = start;

    L.tileLayer(OSM_TILE_URL, {
      maxZoom: 19,
      attribution: OSM_ATTRIBUTION,
      // Fetch tiles once the gesture settles instead of during every animation
      updateWhenZooming: false,
      updateWhenIdle: true,
      keepBuffer: 1,
    }).addTo(map);

    L.control.zoom({ position: 'bottomright' }).addTo(map);

    const invalidateTimer = setTimeout(() => {
      map.invalidateSize();
    }, 150);

    return () => {
      clearTimeout(invalidateTimer);
      map.remove();
      mapRef.current = null;
      markerRef.current = null;
      circleRef.current = null;
      appliedCoordsRef.current = null;
    };
  }, [mapVisible, hasCoords]);

  // Marker, approximate circle, popup and recentering
  useEffect(() => {
    const map = mapRef.current;
    const point = normalizeCoordinates(coords);
    if (!map || !point) return;

    const latlng = [point.lat, point.lon];
    const popupHtml = buildPopupHtml({ title, subtitle, approximate });

    if (!markerRef.current) {
      const icon = L.divIcon({
        className: styles.markerContainer,
        html: MARKER_HTML,
        iconSize: [44, 44],
        iconAnchor: [22, 22],
        popupAnchor: [0, -20],
      });
      markerRef.current = L.marker(latlng, { icon }).addTo(map);
      markerRef.current.bindPopup(popupHtml, { closeButton: false, offset: [0, -12] });
    } else {
      markerRef.current.setLatLng(latlng);
      markerRef.current.setPopupContent(popupHtml);
    }

    if (approximate) {
      if (circleRef.current) {
        circleRef.current.setLatLng(latlng);
      } else {
        circleRef.current = L.circle(latlng, {
          radius: APPROXIMATE_RADIUS_M,
          color: '#3b82f6',
          fillColor: '#3b82f6',
          fillOpacity: 0.12,
          weight: 1.5,
          dashArray: '5, 5',
        }).addTo(map);
      }
    } else if (circleRef.current) {
      map.removeLayer(circleRef.current);
      circleRef.current = null;
    }

    const previous = appliedCoordsRef.current;
    if (previous && (previous.lat !== point.lat || previous.lon !== point.lon)) {
      map.flyTo(latlng, map.getZoom(), { duration: 0.6 });
    }
    appliedCoordsRef.current = point;
  }, [mapVisible, coords, approximate, title, subtitle]);

  // Keep the view in sync when the requested zoom level changes
  useEffect(() => {
    const map = mapRef.current;
    if (!map || map.getZoom() === zoom) return;
    map.setZoom(zoom);
  }, [zoom]);

  // Listening on the wrapper in the capture phase runs before Leaflet's own
  // handler on the map container.
  useEffect(() => {
    const wrapper = wrapperRef.current;
    if (!wrapper) return undefined;

    const hideHint = () => {
      if (hintTimerRef.current) {
        clearTimeout(hintTimerRef.current);
        hintTimerRef.current = null;
      }
      if (showHintRef.current) {
        showHintRef.current = false;
        setShowHint(false);
      }
    };

    const handleWheel = (event) => {
      if (event.ctrlKey || event.metaKey) {
        // Pinch-zoom / Ctrl+wheel: let Leaflet handle it, block browser zoom
        event.preventDefault();
        hideHint();
        return;
      }

      // Plain wheel: stop the event before it reaches Leaflet so the page scrolls
      event.stopPropagation();
      if (!showHintRef.current) {
        showHintRef.current = true;
        setShowHint(true);
      }
      if (hintTimerRef.current) clearTimeout(hintTimerRef.current);
      hintTimerRef.current = setTimeout(hideHint, 1200);
    };

    wrapper.addEventListener('wheel', handleWheel, { capture: true, passive: false });

    return () => {
      wrapper.removeEventListener('wheel', handleWheel, { capture: true });
      if (hintTimerRef.current) {
        clearTimeout(hintTimerRef.current);
        hintTimerRef.current = null;
      }
    };
  }, []);

  const handleResetCenter = () => {
    const map = mapRef.current;
    if (!map || !coords) return;
    map.flyTo([coords.lat, coords.lon], zoom, { duration: 0.8 });
  };

  return (
    <div className={styles.mapWrapper} ref={wrapperRef} style={{ height }}>
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
          <span>Locating on map...</span>
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
