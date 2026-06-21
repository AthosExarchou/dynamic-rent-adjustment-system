# Controller Security Audit

This document serves as a comprehensive reference for the endpoint mappings and their associated security constraints across the application. It outlines the access control rules enforced at the controller layer via Spring Security (`SecurityConfig` and method-level annotations).

## Controller Audit
### UserController.java

| Endpoint | Method | Security |
|----------|--------|----------|
| /register | GET | `permitAll` (SecurityConfig) |
| /saveUser | POST | `permitAll` |
| /users | GET | `@Secured("ADMIN")` |
| /user/{user_id} | GET | `@PreAuthorize("hasRole('ADMIN') or ... == #user_id")` |
| /user/{user_id} | POST | `@Secured("USER")` + UserApplicationService.editUser() does IDOR check |
| /user/role/delete/{uid}/{rid} | POST | `@Secured("ADMIN")` + assertNotAdmin |
| /user/role/add/{uid}/{rid} | POST | `@Secured("ADMIN")` + assertNotAdmin |
| /user/delete/{user_id} | POST | `@Secured("ADMIN")` |
| /user/delete/self | GET/POST | `@Secured("USER")` + confirmation phrase + password check |

### ProfileController.java

| Endpoint | Method | Security |
|----------|--------|----------|
| /profile | GET | `authenticated` (catch-all) + explicit auth check |
| /user/change-password/{id} | GET/POST | `@Secured("USER")` + validateProfileOwnership(id) |
| /user/edit/{id} | GET/POST | `@Secured("USER")` + validateProfileOwnership(id) |

### ListingController.java

| Endpoint | Method | Security |
|----------|--------|----------|
| /listings | GET | `permitAll` |
| /listings/local | GET | `permitAll` |
| /listings/{id} | GET | `permitAll` |
| /listings/filter | GET | `permitAll` |
| /listings/mylisting | GET | `@Secured("OWNER")` |
| /listings/new | GET/POST | `@PreAuthorize("hasRole('USER') and !hasRole('ADMIN')")` |
| /listings/delete/{id} | POST | `@Secured("OWNER")` + validateModificationRights |
| /listings/forapproval | GET | `@Secured("ADMIN")` |
| /listings/approve/{id} | POST | `@Secured("ADMIN")` |
| /listings/reject/{id} | POST | `@Secured("ADMIN")` |
| /listings/assign/{id} | GET/POST | `@Secured("ADMIN")` + validateModificationRights |
| /listings/unassign/owner/{id} | GET | `@Secured("ADMIN")` + validateModificationRights |
| /listings/tenantassign/{id} | GET | `@Secured("USER")` |
| /listings/unassign/tenant/{id} | POST | `@Secured("OWNER")` + validateModificationRights |
| /listings/{id}/applications | GET | `@Secured("OWNER")` + validateModificationRights |

### OwnerController.java

| Endpoint | Method | Security |
|----------|--------|----------|
| /owner/auth/users | GET | `@PreAuthorize("hasRole('ADMIN')")` |
| /owner/new | POST | `@PreAuthorize("hasRole('ADMIN')")` |
| /owner/{id}/listings | GET | `@PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")` + explicit user-ID ownership check + system-owner guard |
| /owner/listings/{lid}/approveApplicant/{tid} | POST | `@Secured("OWNER")` -> delegates to ListingApplicationService.approveTenantApplication which calls validateModificationRights |
| /owner/listings/{lid}/rejectApplicant/{tid} | POST | `@Secured("OWNER")` -> same delegation pattern |

### TenantController.java

| Endpoint | Method | Security |
|----------|--------|----------|
| /tenant/rent/{id} | GET | `@Secured("USER")` + validateRentalApplicationRights + tenant-already-renting check |
| /tenant/rent/{listingId} | POST | `@Secured("USER")` + validateRentalApplicationRights + duplicate application check + already-renting check |
| /tenant/new | POST | `@PreAuthorize("hasRole('ADMIN')")` |

### ExternalImportController.java

| Endpoint | Method | Security |
|----------|--------|----------|
| /api/external-import/listings | POST | `hasAuthority("ADMIN")` (SecurityConfig) + CSRF disabled for API path |

### AuthController.java / HomeController.java / AppErrorController.java

All public endpoints - correctly matched by `SecurityConfig.permitAll()`.

### MvcExceptionHandler.java

- Handles `IllegalState`, `IllegalArgument`, `AccessDenied`, `ResponseStatus`, `DataIntegrityViolation`, `OptimisticLockingFailure`, and generic `Exception`
- Uses safe `redirectToReferer` helper with open-redirect protection (validates same-host)
- Generic fallback hides internal exception details from user - logs server-side only
