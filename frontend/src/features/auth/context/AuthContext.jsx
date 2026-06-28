import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import apiClient from '../../../shared/api/client';

/**
 * Authentication Context for the DRAS frontend.
 *
 * This context provides:
 * - `user`       - The current authenticated user object, or null.
 * - `roles`      - Array of role name strings (e.g. ['USER', 'OWNER']).
 * - `isLoading`  - True while initial auth check is in progress.
 * - `isAuthenticated` - Shorthand for user !== null.
 * - `login(email, password)` - Authenticates and sets user state.
 * - `logout()`   - Ends session and clears user state.
 * - `hasRole(roleName)` - Check if user has a specific role.
 * - `refreshUser()` - Reload the user state from the server.
 */

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  const checkSession = useCallback(async (cancelled = false) => {
    try {
      const currentUser = await apiClient('/api/auth/me');
      if (!cancelled) {
        setUser(currentUser);
      }
    } catch {
      if (!cancelled) {
        setUser(null);
      }
    } finally {
      if (!cancelled) {
        setIsLoading(false);
      }
    }
  }, []);

  /* Check if user has an active session on mount */
  useEffect(() => {
    let cancelled = false;
    checkSession(cancelled);
    return () => { cancelled = true; };
  }, [checkSession]);

  const refreshUser = useCallback(async () => {
    await checkSession(false);
  }, [checkSession]);

  const login = useCallback(async (username, password) => {
    // Expected: POST /api/auth/login { username, password } -> User JSON + session cookie
    const loggedInUser = await apiClient('/api/auth/login', {
      method: 'POST',
      body: { username, password },
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

  // Memoize `roles` so it has a stable reference between renders
  const roles = useMemo(
    () => user?.roles?.map((r) => (typeof r === 'string' ? r : r.name)) || [],
    [user]
  );

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
    refreshUser,
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
