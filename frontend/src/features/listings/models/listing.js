/**
 * Listing domain model and DTO shapes.
 *
 * @see {@code backend/src/main/java/gr/hua/dit/dras/entities/Listing.java}
 * @see {@code backend/src/main/java/gr/hua/dit/dras/dto/ListingFilterDTO.java}
 * @see {@code backend/src/main/java/gr/hua/dit/dras/dto/ListingCreateDTO.java}
 */

/**
 * Creates a default empty listing filter.
 * @returns {Object} A ListingFilterDTO-shaped object with all fields null.
 */
export function createEmptyListingFilter() {
  return {
    title: null,
    search: null,
    minPrice: null,
    maxPrice: null,
    type: null,
    municipality: null,
    district: null,
    minBedrooms: null,
    maxBedrooms: null,
    minBathrooms: null,
    maxBathrooms: null,
    minYear: null,
    maxYear: null,
    updatedAfter: null,
    updatedBefore: null,
    externalOnly: null,
  };
}

/**
 * Validation constraints matching the backend entity.
 */
export const LISTING_CONSTRAINTS = Object.freeze({
  TITLE_MAX: 150,
  SUBTITLE_MAX: 250,
  DESCRIPTION_MAX: 5000,
  ADDRESS_MAX: 255,
  PRICE_MIN: 0,
  PRICE_MAX: 20000,
  PRICE_PER_M2_MIN: 0,
  PRICE_PER_M2_MAX: 200,
  SIZE_M2_MIN: 5,
  SIZE_M2_MAX: 1000,
  FLOOR_MIN: -3,
  FLOOR_MAX: 100,
  BEDROOMS_MIN: 0,
  BEDROOMS_MAX: 10,
  BATHROOMS_MIN: 0,
  BATHROOMS_MAX: 5,
  YEAR_BUILT_MIN: 1800,
  YEAR_BUILT_MAX_OFFSET: 5,
  SOURCE_URL_MAX: 500,
  REJECTION_REASON_MAX: 500,
  SEARCH_MAX: 150,
  MUNICIPALITY_MAX: 255,
  DISTRICT_MAX: 250,
  IMAGES_MAX: 10,
});
