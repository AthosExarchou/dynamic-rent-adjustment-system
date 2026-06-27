/**
 * Global application configuration.
 * 
 * Centralizes environment variables and application-wide constants.
 * This prevents `import.meta.env` from being scattered throughout the codebase
 * and provides a single place to set defaults or validate variables.
 */

export const config = {
  apiBaseUrl: import.meta.env.VITE_API_URL || import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  isDev: import.meta.env.DEV,
  isProd: import.meta.env.PROD,
};

export default config;
