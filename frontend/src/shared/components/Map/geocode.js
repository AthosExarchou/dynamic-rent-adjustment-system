/**
 * Minimal Nominatim client.
 *
 * Caches results for the session, de-duplicates concurrent lookups, and keeps
 * requests at >= 1/second as required by the Nominatim usage policy.
 * https://operations.osmfoundation.org/policies/nominatim/
 */

const ENDPOINT = 'https://nominatim.openstreetmap.org/search';
const MIN_INTERVAL_MS = 1100;

const cache = new Map();
const inflight = new Map();
let queue = Promise.resolve();
let lastRequestAt = 0;

function cacheKey(address) {
  return address.trim().toLowerCase().replace(/\s+/g, ' ');
}

function toCoordinates(payload) {
  if (!Array.isArray(payload) || payload.length === 0) return null;
  const lat = Number.parseFloat(payload[0].lat);
  const lon = Number.parseFloat(payload[0].lon);
  return Number.isFinite(lat) && Number.isFinite(lon) ? { lat, lon } : null;
}

async function request(query) {
  const url = `${ENDPOINT}?q=${encodeURIComponent(query)}&format=json&limit=1&accept-language=en`;
  const response = await fetch(url, { headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error(`Nominatim request failed: ${response.status}`);
  return response.json();
}

function schedule(task) {
  const run = queue.then(async () => {
    const wait = MIN_INTERVAL_MS - (Date.now() - lastRequestAt);
    if (wait > 0) await new Promise((resolve) => setTimeout(resolve, wait));
    lastRequestAt = Date.now();
    return task();
  });
  // Keep the queue alive when a request fails
  queue = run.catch(() => undefined);
  return run;
}

async function lookup(address) {
  const query = address.trim();
  const exact = toCoordinates(await request(query));
  if (exact) return exact;

  // Retry with the broader part of the address (drop the street line)
  if (query.includes(',')) {
    const broader = query.split(',').slice(1).join(',').trim();
    if (broader) return toCoordinates(await request(broader));
  }

  return null;
}

/** Resolve an address to `{ lat, lon }`, or `null` if it cannot be found. */
export function geocodeAddress(address) {
  if (!address || !address.trim()) return Promise.resolve(null);

  const key = cacheKey(address);
  if (cache.has(key)) return Promise.resolve(cache.get(key));
  if (inflight.has(key)) return inflight.get(key);

  const promise = schedule(() => lookup(address))
    .then((result) => {
      if (result) cache.set(key, result);
      return result;
    })
    .catch(() => null)
    .finally(() => {
      inflight.delete(key);
    });

  inflight.set(key, promise);
  return promise;
}
