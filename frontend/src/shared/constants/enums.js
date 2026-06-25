/**
 * Listing Status - mirrors backend ListingStatus enum.
 * @see backend: gr.hua.dit.dras.model.enums.ListingStatus
 */
export const ListingStatus = Object.freeze({
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  DISABLED: 'DISABLED',
  RENTED: 'RENTED',
  DELETED: 'DELETED',
});

/**
 * Property Type - mirrors backend PropertyType enum.
 * @see backend: gr.hua.dit.dras.model.enums.PropertyType
 */
export const PropertyType = Object.freeze({
  APARTMENT: 'APARTMENT',
  HOUSE: 'HOUSE',
  STUDIO: 'STUDIO',
  MAISONETTE: 'MAISONETTE',
  LOFT: 'LOFT',
  VILLA: 'VILLA',
  OTHER: 'OTHER',
});

/**
 * Rental Duration - mirrors backend RentalDuration enum.
 * @see backend: gr.hua.dit.dras.model.enums.RentalDuration
 */
export const RentalDuration = Object.freeze({
  INDEFINITE: 'INDEFINITE',
  FIXED_TERM: 'FIXED_TERM',
  SHORT_TERM: 'SHORT_TERM',
  LONG_TERM: 'LONG_TERM',
  OTHER: 'OTHER',
});

/**
 * Rental Status - mirrors backend RentalStatus enum.
 * @see backend: gr.hua.dit.dras.model.enums.RentalStatus
 */
export const RentalStatus = Object.freeze({
  APPLIED: 'APPLIED',
  RENTING: 'RENTING',
  CANCELED: 'CANCELED',
});

/**
 * User Roles - mirrors backend Role entity seeded values.
 * @see backend: gr.hua.dit.dras.entities.Role
 */
export const UserRole = Object.freeze({
  USER: 'USER',
  ADMIN: 'ADMIN',
  OWNER: 'OWNER',
  TENANT: 'TENANT',
});
