/**
 * Tenant domain model.
 *
 * Mirrors: gr.hua.dit.dras.entities.Tenant
 *
 * Shape:
 *   id              - Integer
 *   firstName       - String (max 20)
 *   lastName        - String (max 20)
 *   phoneNumber     - String (unique, regex: ^\\+?[0-9. ()-]{7,25}$)
 *   rentalStatus    - RentalStatus enum (APPLIED | RENTING | CANCELED)
 *   listing         - ListingSummary | null (current rental)
 *   appliedListings - ListingSummary[] (pending applications)
 *   user            - UserSummary
 *
 * TenantCreateRequest shape (admin creates tenant for a user):
 *   userId       - Integer (@NotNull)
 *   firstName    - String (@NotBlank)
 *   lastName     - String (@NotBlank)
 *   phoneNumber  - String (@NotBlank)
 */

export const TENANT_CONSTRAINTS = Object.freeze({
  FIRST_NAME_MAX: 20,
  LAST_NAME_MAX: 20,
  PHONE_REGEX: /^\+?[0-9. ()-]{7,25}$/,
});
