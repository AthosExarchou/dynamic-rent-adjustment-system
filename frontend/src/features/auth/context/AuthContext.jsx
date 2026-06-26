import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import apiClient from '../../../shared/api/client';

/**
 * Authentication Context for the DRAS frontend.
 *
 * STRATEGY NOTE:
 * The backend currently uses Spring Security session-based form login
 * (see SecurityConfig.java). For the React SPA to work, the backend
 * will need a REST-based auth endpoint (e.g. POST /api/auth/login
 * returning the user as JSON and setting a session cookie).
 *
 * This context provides:
 * - `user`       - The current authenticated user object, or null.
 * - `roles`      - Array of role name strings (e.g. ['USER', 'OWNER']).
 * - `isLoading`  - True while initial auth check is in progress.
 * - `isAuthenticated` - Shorthand for user !== null.
 * - `login(email, password)` - Authenticates and sets user state.
 * - `logout()`   - Ends session and clears user state.
 * - `hasRole(roleName)` - Check if user has a specific role.
 */

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  /* Check if user has an active session on mount */
  useEffect(() => {
    let cancelled = false;

    async function checkSession() {
      try {
        const currentUser = await apiClient('/api/auth/me');
        if (!cancelled) {
          setUser(currentUser);
        }
      } catch {
        /* Not authenticated - expected on first visit */
        if (!cancelled) {
          setUser(null);
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    checkSession();
    return () => { cancelled = true; };
  }, []);

  const login = useCallback(async (email, password) => {
    // TODO: Implement once backend exposes a REST login endpoint.
    // Expected: POST /api/auth/login { email, password } -> User JSON + session cookie
    const loggedInUser = await apiClient('/api/auth/login', {
      method: 'POST',
      body: { email, password },
    });
    setUser(loggedInUser);
    return loggedInUser;
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiClient('/api/auth/logout', { method: 'POST' });
    } catch {
      /* Proceed with client-side cleanup regardless */
    }
    setUser(null);
  }, []);

  const roles = user?.roles?.map((r) => (typeof r === 'string' ? r : r.name)) || [];

  const hasRole = useCallback(
    (roleName) => roles.includes(roleName),
    [roles]
  );

  const value = {
    user,
    roles,
    isLoading,
    isAuthenticated: user !== null,
    login,
    logout,
    hasRole,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * Hook to access authentication state and actions.
 * Must be used within an <AuthProvider>.
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an <AuthProvider>');
  }
  return context;
}

export default AuthContext;
