/**
 * Listing domain model.
 *
 * Mirrors: gr.hua.dit.dras.entities.Listing
 *
 * Shape:
 *   id              - Integer
 *   title           - String (max 150)
 *   subtitle        - String | null (max 250)
 *   description     - String (max 5000)
 *   price           - Integer (0-20000)
 *   pricePerM2      - Integer (0-200)
 *   address         - String (max 255)
 *   sizeM2          - Integer (5-1000)
 *   propertyType    - PropertyType enum
 *   rentalDuration  - RentalDuration enum
 *   floor           - Integer | null (-3-100)
 *   yearBuilt       - Integer | null
 *   bedrooms        - Integer | null (0-10)
 *   bathrooms       - Integer | null (0-5)
 *   sourceUrl       - String | null (max 500)
 *   status          - ListingStatus enum
 *   images          - String[]
 *   external        - boolean
 *   createdAt       - ISO date string
 *   updatedAt       - ISO date string
 *   owner           - OwnerSummary | null
 *   tenant          - TenantSummary | null
 *   applicants      - TenantSummary[]
 *
 * ListingFilterDTO shape:
 *   title           - String | null
 *   minPrice        - Integer | null
 *   maxPrice        - Integer | null
 *   type            - PropertyType | null
 *   municipality    - String | null
 *   district        - String | null
 *   minBedrooms     - Integer | null
 *   maxBedrooms     - Integer | null
 *   minBathrooms    - Integer | null
 *   maxBathrooms    - Integer | null
 *   minYear         - Integer | null
 *   maxYear         - Integer | null
 *   updatedAfter    - date string | null
 *   updatedBefore   - date string | null
 *   externalOnly    - boolean | null
 */

/**
 * Creates a default empty listing filter.
 * @returns {Object} A ListingFilterDTO-shaped object with all fields null.
 */
export function createEmptyListingFilter() {
  return {
    title: null,
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
  SOURCE_URL_MAX: 500,
});
