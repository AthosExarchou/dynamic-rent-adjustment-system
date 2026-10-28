/**
 * Generic demonstration placeholder used when a listing has no custom photos.
 * Note: Local Pexels demo asset (`/images/demo/image1.jpg`) enables full offline
 * usage. Sourced from Pexels as a generic architectural graphic, not a photograph
 * of a specific real property in Athens. See IMAGE_CREDITS.md for details.
 */
export const DEFAULT_LISTING_IMAGE = "/images/demo/image1.jpg";
export const REMOTE_FALLBACK_IMAGE = "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?auto=format&fit=crop&w=1200&q=80";

/**
 * Resolves an image path to a usable image URL.
 *
 * Supported formats:
 * - Remote URLs: 'http://...', 'https://...' -> unchanged
 * - In-memory previews: 'blob:...', 'data:image/...' -> unchanged
 * - Scraped local paths: 'data/images/2120914817/abc.jpg' -> '/api/images/2120914817/abc.jpg'
 * - Local server paths: '/images/uploads/abc.jpg' or 'images/abc.jpg' -> '/api/images/uploads/abc.jpg'
 * - Raw filenames/subpaths: '2120914817/abc.jpg' -> '/api/images/2120914817/abc.jpg'
 */
export function getImageUrl(path) {
  if (!path || typeof path !== 'string') return null;

  const trimmed = path.trim();
  if (!trimmed) return null;

  // Remote URLs or data/blob URIs are passed through as-is
  if (/^(https?:\/\/|data:|blob:)/i.test(trimmed)) {
    return trimmed;
  }

  // Normalize path by stripping leading slash
  const clean = trimmed.startsWith('/') ? trimmed.slice(1) : trimmed;

  // If already routed to api/
  if (clean.startsWith('api/')) {
    return `/${clean}`;
  }

  // If path starts with 'data/images/', map to '/api/images/...'
  if (clean.startsWith('data/images/')) {
    return `/api/images/${clean.slice('data/images/'.length)}`;
  }

  // If path starts with 'data/', map to '/api/images/...'
  if (clean.startsWith('data/')) {
    return `/api/images/${clean.slice('data/'.length)}`;
  }

  // If path starts with 'images/', map to '/api/images/...'
  if (clean.startsWith('images/')) {
    return `/api/${clean}`;
  }

  // Default: prepend '/api/images/'
  return `/api/images/${clean}`;
}
