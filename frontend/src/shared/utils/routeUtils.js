import { matchPath } from 'react-router-dom';

/**
 * List of registered navigable route patterns in the application.
 * Excludes intermediate/grouping segments that are not standalone pages and the catch-all wildcard.
 */
export const NAVIGABLE_ROUTES = [
  '/',
  '/about',
  '/contact',
  '/privacy',
  '/terms',
  '/listings',
  '/listings/:id',
  '/favourites',
  '/listings/new',
  '/login',
  '/register',
  '/profile',
  '/profile/edit',
  '/profile/password',
  '/profile/delete',
  '/owner/new',
  '/tenant/rent/:listingId',
  '/my-listings',
  '/my-listings/:listingId/apps',
  '/admin/users',
  '/admin/approvals',
];

/**
 * Checks whether a given pathname corresponds to a registered navigable route.
 * @param {string} pathname - Path to validate (e.g., '/my-listings/6')
 * @returns {boolean} - True if navigable, false if it would hit 404 or an unroutable state.
 */
export const isNavigablePath = (pathname) => {
  if (!pathname) return false;
  return NAVIGABLE_ROUTES.some((pattern) => matchPath({ path: pattern, end: true }, pathname) !== null);
};
