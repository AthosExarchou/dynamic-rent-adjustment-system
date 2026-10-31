/**
 * Owner domain model and DTO shapes.
 *
 * @see {@code backend/src/main/java/gr/hua/dit/dras/entities/Owner.java}
 * @see {@code backend/src/main/java/gr/hua/dit/dras/dto/OwnerCreateRequest.java}
 */

export const OWNER_CONSTRAINTS = Object.freeze({
  FIRST_NAME_MAX: 20,
  LAST_NAME_MAX: 20,
  PHONE_MIN: 7,
  PHONE_MAX: 25,
  PHONE_REGEX: /^\+?[0-9. ()-]{7,25}$/,
});
