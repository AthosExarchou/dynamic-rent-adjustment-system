/**
 * Owner domain model.
 *
 * Mirrors: gr.hua.dit.dras.entities.Owner
 *
 * Shape:
 *   id           - Integer
 *   firstName    - String (max 20)
 *   lastName     - String (max 20)
 *   phoneNumber  - String (unique, regex: ^\\+?[0-9. ()-]{7,25}$)
 *   systemOwner  - boolean
 *   listings     - ListingSummary[]
 *   user         - UserSummary
 *
 * OwnerCreateRequest shape (admin creates owner for a user):
 *   userId       - Integer (@NotNull)
 *   firstName    - String (@NotBlank)
 *   lastName     - String (@NotBlank)
 *   phoneNumber  - String (@NotBlank)
 */

export const OWNER_CONSTRAINTS = Object.freeze({
  FIRST_NAME_MAX: 20,
  LAST_NAME_MAX: 20,
  PHONE_REGEX: /^\+?[0-9. ()-]{7,25}$/,
});
