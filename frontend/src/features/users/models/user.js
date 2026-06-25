/**
 * User domain model.
 *
 * Mirrors: gr.hua.dit.dras.entities.User
 *
 * Shape:
 *   id         - Integer
 *   username   - String (max 20, unique)
 *   email      - String (max 50, unique, used as login principal)
 *   roles      - Role[] (each: { id, name })
 *   createdAt  - ISO date string
 *   updatedAt  - ISO date string
 *   lastLogin  - ISO date string | null
 *   owner      - OwnerSummary | null
 *   tenant     - TenantSummary | null
 *
 * UserEditRequest shape (for profile editing):
 *   username   - String (@NotBlank, max 20)
 *   email      - String (@NotBlank, @Email, max 50)
 *
 * AccountDeletionRequest shape:
 *   confirmationPhrase - String (must equal "DELETE MY ACCOUNT")
 *   password           - String (@NotBlank)
 */

export const USER_CONSTRAINTS = Object.freeze({
  USERNAME_MAX: 20,
  EMAIL_MAX: 50,
  PASSWORD_MAX: 100,
  DELETION_CONFIRMATION_PHRASE: 'DELETE MY ACCOUNT',
});
