/**
 * User domain model and DTO shapes.
 *
 * @see {@code backend/src/main/java/gr/hua/dit/dras/entities/User.java}
 * @see {@code backend/src/main/java/gr/hua/dit/dras/dto/UserDTO.java}
 * @see {@code backend/src/main/java/gr/hua/dit/dras/dto/UserEditRequest.java}
 * @see {@code backend/src/main/java/gr/hua/dit/dras/dto/AccountDeletionRequest.java}
 */

export const USER_CONSTRAINTS = Object.freeze({
  USERNAME_MAX: 20,
  EMAIL_MAX: 50,
  PASSWORD_MAX: 100,
  DELETION_CONFIRMATION_PHRASE: 'DELETE MY ACCOUNT',
});
