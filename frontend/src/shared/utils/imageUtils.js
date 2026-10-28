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

/**
 * Validates a web image URL with syntax/hostname checks and a pre-flight image load.
 * Returns a Promise that resolves with image metadata if valid, or rejects with a descriptive error.
 *
 * @param {string} url - Web image URL to validate
 * @param {number} timeoutMs - Timeout in milliseconds (defaults to 6000)
 * @returns {Promise<{ url: string, width: number, height: number }>}
 */
export function validateImageUrl(url, timeoutMs = 6000) {
  return new Promise((resolve, reject) => {
    const trimmed = (url || '').trim();
    if (!trimmed) {
      return reject(new Error('Please enter an image URL.'));
    }

    // Syntax & Domain check
    let parsed;
    try {
      parsed = new URL(trimmed);
    } catch {
      return reject(new Error('Invalid URL format. Please include http:// or https://'));
    }

    if (!['http:', 'https:'].includes(parsed.protocol)) {
      return reject(new Error('URL must start with http:// or https://'));
    }

    if (!parsed.hostname.includes('.') && parsed.hostname !== 'localhost') {
      return reject(new Error('Please enter a valid web domain with a valid extension (e.g. https://example.com/photo.jpg).'));
    }

    // Pre-flight image load check using in-memory Image
    if (typeof window === 'undefined' || typeof window.Image === 'undefined') {
      return resolve({ url: trimmed, width: 0, height: 0 });
    }

    const img = new window.Image();
    let settled = false;

    const timer = setTimeout(() => {
      if (settled) return;
      settled = true;
      img.src = '';
      reject(new Error('Image request timed out. The server took too long to respond.'));
    }, timeoutMs);

    img.onload = () => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      resolve({
        url: trimmed,
        width: img.naturalWidth,
        height: img.naturalHeight
      });
    };

    img.onerror = () => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      reject(new Error('Unable to load image from this URL. Please verify the link points to a direct, publicly accessible photo (JPG, PNG, WebP).'));
    };

    img.src = trimmed;
  });
}
