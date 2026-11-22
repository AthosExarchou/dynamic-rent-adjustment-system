/**
 * Generic demonstration placeholder used when a listing has no custom photos.
 * Note: Local Pexels demo asset (`/images/demo/image1.jpg`) enables full offline
 * usage. Sourced from Pexels as a generic architectural graphic, not a photograph
 * of a specific real property in Athens. See IMAGE_CREDITS.md for details.
 */
export const DEFAULT_LISTING_IMAGE = '/images/demo/image1.jpg';
export const SECONDARY_LISTING_IMAGE = '/images/demo/image2.jpg';

/**
 * Returns the next local fallback for a failed listing image.
 * The chain always stops after two attempts, even when the real image is one of
 * the demo assets.
 */
export function getListingImageFallback(originalSrc, attempt) {
  if (originalSrc.endsWith(DEFAULT_LISTING_IMAGE)) {
    return attempt === 0 ? SECONDARY_LISTING_IMAGE : null;
  }
  if (originalSrc.endsWith(SECONDARY_LISTING_IMAGE)) {
    return attempt === 0 ? DEFAULT_LISTING_IMAGE : null;
  }
  if (attempt >= 2) return null;
  return attempt === 0 ? DEFAULT_LISTING_IMAGE : SECONDARY_LISTING_IMAGE;
}

/**
 * Fallback chain for broken listing images. Both assets are local Pexels files,
 * so the UI stays offline-capable and never requests a third-party CDN.
 */
export function handleListingImageError(event) {
  const img = event?.target;
  if (!img) return;

  const currentSrc = img.getAttribute('src') || img.src || '';
  if (img.dataset.lastSrc !== currentSrc) {
    img.dataset.lastSrc = currentSrc;
    img.dataset.fallbackOrigin = currentSrc;
    img.dataset.fallbackAttempt = '0';
  }

  const attempt = Number(img.dataset.fallbackAttempt || 0);
  const next = getListingImageFallback(img.dataset.fallbackOrigin || currentSrc, attempt);
  if (!next) return;

  img.dataset.fallbackAttempt = String(attempt + 1);
  img.dataset.lastSrc = next;
  img.src = next;
}

/**
 * Resolves a stored image path to a usable local URL.
 *
 * Supported formats:
 * - In-memory previews: 'blob:...', 'data:image/...' -> unchanged
 * - Scraped local paths: 'data/images/2120914817/abc.jpg' -> '/api/images/2120914817/abc.jpg'
 * - Demo assets: 'images/demo/image1.jpg' -> '/images/demo/image1.jpg'
 * - Local server paths: '/images/uploads/abc.jpg' or 'images/abc.jpg' -> '/api/images/uploads/abc.jpg'
 * - Raw filenames/subpaths: '2120914817/abc.jpg' -> '/api/images/2120914817/abc.jpg'
 *
 * Remote http(s) URLs are replaced by the local placeholder so the browser never
 * contacts an external image host.
 */
export function getImageUrl(path) {
  if (!path || typeof path !== 'string') return null;

  const trimmed = path.trim();
  if (!trimmed) return null;

  if (/^(https?:)?\/\//i.test(trimmed)) {
    return DEFAULT_LISTING_IMAGE;
  }

  if (/^(data:|blob:)/i.test(trimmed)) {
    return trimmed;
  }

  const clean = trimmed.startsWith('/') ? trimmed.slice(1) : trimmed;

  if (clean.startsWith('api/')) {
    return `/${clean}`;
  }

  if (clean.startsWith('data/images/')) {
    return `/api/images/${clean.slice('data/images/'.length)}`;
  }

  if (clean.startsWith('data/')) {
    return `/api/images/${clean.slice('data/'.length)}`;
  }

  if (clean.startsWith('images/demo/')) {
    return `/${clean}`;
  }

  if (clean.startsWith('images/')) {
    return `/api/${clean}`;
  }

  return `/api/images/${clean}`;
}
