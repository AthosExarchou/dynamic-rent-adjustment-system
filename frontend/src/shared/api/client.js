/**
 * Centralized API client for communicating with the DRAS backend.
 *
 * All feature-level `api/` modules should import this client
 * rather than calling fetch() directly, to ensure consistent
 * base URL, credentials, headers, and error handling.
 */

import config from '../../config';

const API_BASE_URL = config.apiBaseUrl;

/**
 * Wrapper around fetch() pre-configured for the DRAS backend.
 *
 * - Automatically prepends the API base URL.
 * - Sends credentials (cookies/session) with every request.
 * - Sets JSON content-type for non-GET requests.
 * - Throws on non-2xx responses with a parsed error message.
 *
 * @param {string} endpoint - Path relative to the API base (e.g. '/listings')
 * @param {RequestInit} [options={}] - Standard fetch options (method, body, headers, etc.)
 * @returns {Promise<any>} Parsed JSON response body
 */
async function apiClient(endpoint, options = {}) {
  const { headers: customHeaders, body, ...restOptions } = options;

  const headers = {
    ...customHeaders,
  };

  /* Set JSON content-type for requests with a body */
  if (body && !(body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  // BUG-F21: Extract CSRF token and append it
  const method = (restOptions.method || 'GET').toUpperCase();
  if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
    const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
    if (match && match[1]) {
      headers['X-XSRF-TOKEN'] = decodeURIComponent(match[1]);
    }
  }

  const fetchConfig = {
    ...restOptions,
    headers,
    credentials: 'include', // send session cookies cross-origin
  };

  if (body) {
    fetchConfig.body = typeof body === 'string' ? body : JSON.stringify(body);
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, fetchConfig);

  if (!response.ok) {
    let errorMessage = `API Error: ${response.status} ${response.statusText}`;

    try {
      const errorBody = await response.json();
      errorMessage = errorBody.message || errorBody.error || errorMessage;
    } catch {
      /* Response body is not JSON - use default message */
    }

    const error = new Error(errorMessage);
    error.status = response.status;
    throw error;
  }

  /* Handle 204 No Content */
  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return response.json();
  }

  return response.text();
}

export default apiClient;
