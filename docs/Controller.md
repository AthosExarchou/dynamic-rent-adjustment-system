# Controller Security Audit

This document serves as a comprehensive reference for the endpoint mappings and their associated security constraints across the application. It outlines the access control rules enforced at the controller layer via Spring Security (`SecurityConfig` and method-level annotations).

## Controller Audit

### UserController.java

| Endpoint | Method | Security |
|----------|--------|----------|
| /saveUser | POST | `permitAll` (SecurityConfig) |
| /users | GET | `@Secured("ADMIN")` |
| /user/{user_id} | GET | `@PreAuthorize("hasAuthority('ADMIN') or @userService.getCurrentUserId() == #user_id")` |
| /user/{user_id} | POST | `@Secured("USER")` + internal access checks |
| /user/role/delete/{user_id}/{role_id} | POST | `@Secured("ADMIN")` + assertNotAdmin |
| /user/role/add/{user_id}/{role_id} | POST | `@Secured("ADMIN")` + assertNotAdmin |
| /user/delete/{user_id} | POST | `@Secured("ADMIN")` |
| /user/delete/self | POST | `@Secured("USER")` |

### ProfileController.java

| Endpoint | Method | Security |
|----------|--------|----------|
| /user/change-password/{id} | POST | `@Secured("USER")` + validateProfileOwnership(id) |

### ListingController.java

| Endpoint | Method | Security |
|----------|--------|----------|
| /listings | GET | `permitAll` (SecurityConfig) |
| /listings/local | GET | `permitAll` (SecurityConfig) |
| /listings/{id} | GET | `permitAll` (SecurityConfig) |
| /listings/filter | GET | `permitAll` (SecurityConfig) |
| /listings/mylisting | GET | `@Secured("OWNER")` |
| /listings/new | POST | `@PreAuthorize("hasAuthority('USER')")` |
| /listings/delete/{id} | POST | `@Secured("OWNER")` |
| /listings/forapproval | GET | `@Secured("ADMIN")` |
| /listings/approve/{id} | POST | `@Secured("ADMIN")` |
| /listings/reject/{id} | POST | `@Secured("ADMIN")` |
| /listings/assign/{id} | POST | `@Secured("ADMIN")` |
| /listings/unassign/owner/{id} | POST | `@Secured("ADMIN")` |
| /listings/unassign/tenant/{id} | POST | `@Secured("OWNER")` |
| /listings/{id}/applications | GET | `@Secured("OWNER")` |

### OwnerController.java

| Endpoint | Method | Security |
|----------|--------|----------|
| /owner/new | POST | `@PreAuthorize("hasRole('ADMIN') or hasRole('USER')")` |
| /owner/{id}/listings | GET | `@PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")` + system-owner guard |
| /owner/listings/{listingId}/approveApplicant/{tenantId} | POST | `@Secured("OWNER")` |
| /owner/listings/{listingId}/rejectApplicant/{tenantId} | POST | `@Secured("OWNER")` |

### TenantController.java

| Endpoint | Method | Security |
|----------|--------|----------|
| /tenant/rent/{listingId} | POST | `@Secured("USER")` + validateRentalApplicationRights |
| /tenant/new | POST | `@PreAuthorize("hasRole('ADMIN')")` |

### ExternalImportController.java

| Endpoint | Method | Security |
|----------|--------|----------|
| /external-import/listings | POST | `hasAuthority("ADMIN")` (SecurityConfig) |

### NotificationController.java

| Endpoint | Method | Security |
|----------|--------|----------|
| /notifications | GET | manual auth check |
| /notifications/unread-count | GET | manual auth check |
| /notifications/{id}/read | PUT | manual auth check |
| /notifications/read-all | PUT | manual auth check |

### RestAuthController.java

| Endpoint | Method | Security |
|----------|--------|----------|
| /auth/login | POST | `permitAll` (SecurityConfig) |
| /auth/me | GET | `permitAll` (SecurityConfig) + manual auth check |
| /auth/logout | POST | `permitAll` (SecurityConfig) |

### AuthController.java / HomeController.java / AppErrorController.java

All public endpoints - correctly matched by `SecurityConfig.permitAll()`.

### MvcExceptionHandler.java

- Handles `IllegalState`, `IllegalArgument`, `AccessDenied`, `ResponseStatus`, `DataIntegrityViolation`, `OptimisticLockingFailure`, and generic `Exception`
- Uses safe `redirectToReferer` helper with open-redirect protection (validates same-host)
- Generic fallback hides internal exception details from user - logs server-side only
