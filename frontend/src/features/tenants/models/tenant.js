/**
 * Tenant domain model and DTO shapes.
 *
 * @see {@code backend/src/main/java/gr/hua/dit/dras/entities/Tenant.java}
 * @see {@code backend/src/main/java/gr/hua/dit/dras/dto/TenantCreateRequest.java}
 */

export const TENANT_CONSTRAINTS = Object.freeze({
  FIRST_NAME_MAX: 20,
  LAST_NAME_MAX: 20,
  PHONE_MIN: 7,
  PHONE_MAX: 25,
  PHONE_REGEX: /^\+?[0-9. ()-]{7,25}$/,
});
