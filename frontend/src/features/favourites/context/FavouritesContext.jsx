import React, { createContext, useContext, useState, useEffect, useCallback, useMemo, useRef } from 'react';
import apiClient from '../../../shared/api/client';
import { useAuth } from '../../auth';

const FavouritesContext = createContext(null);

const GUEST_STORAGE_KEY = 'dras_guest_favourites';
const USER_STORAGE_PREFIX = 'dras_user_favourites_';
const LEGACY_STORAGE_KEY = 'dras_favourites';

// Clean up legacy un-scoped storage key if present
try {
  localStorage.removeItem(LEGACY_STORAGE_KEY);
} catch {
  // ignore
}

export const getGuestFavouriteIds = () => {
  try {
    const raw = localStorage.getItem(GUEST_STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (err) {
    console.error('Failed to parse guest favourites from localStorage', err);
    return [];
  }
};

export const setGuestFavouriteIds = (ids) => {
  try {
    if (!ids || ids.length === 0) {
      localStorage.removeItem(GUEST_STORAGE_KEY);
    } else {
      localStorage.setItem(GUEST_STORAGE_KEY, JSON.stringify(ids));
    }
  } catch (err) {
    console.error('Failed to store guest favourites in localStorage', err);
  }
};

export const clearGuestFavourites = () => {
  try {
    localStorage.removeItem(GUEST_STORAGE_KEY);
    localStorage.removeItem(LEGACY_STORAGE_KEY);
  } catch {
    // ignore
  }
};

export const getUserFavouriteIds = (userId) => {
  if (!userId) return [];
  try {
    const raw = localStorage.getItem(USER_STORAGE_PREFIX + userId);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
};

export const setUserFavouriteIds = (userId, ids) => {
  if (!userId) return;
  try {
    localStorage.setItem(USER_STORAGE_PREFIX + userId, JSON.stringify(ids));
  } catch (err) {
    console.error('Failed to store user favourites in localStorage', err);
  }
};

export const clearUserFavourites = (userId) => {
  if (!userId) return;
  try {
    localStorage.removeItem(USER_STORAGE_PREFIX + userId);
  } catch {
    // ignore
  }
};

export function FavouritesProvider({ children }) {
  const { user, isAuthenticated, isLoading: authLoading } = useAuth();

  const prevAuthRef = useRef(isAuthenticated);
  const prevUserIdRef = useRef(user?.id);

  const [favouriteIds, setFavouriteIds] = useState(() => {
    return isAuthenticated && user?.id ? getUserFavouriteIds(user.id) : getGuestFavouriteIds();
  });
  const [favourites, setFavourites] = useState([]);
  const [loading, setLoading] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [guestFavouritesPending, setGuestFavouritesPending] = useState(() => {
    return isAuthenticated && user?.id ? getGuestFavouriteIds().length : 0;
  });

  // Sync / fetch favourite IDs whenever authentication state changes
  useEffect(() => {
    if (authLoading) return;

    let active = true;

    async function handleAuthChange() {
      if (isAuthenticated && user?.id) {
        // Authenticated user:
        // Check if there are guest favourites waiting to be imported
        const guestIds = getGuestFavouriteIds();
        if (guestIds.length > 0) {
          setGuestFavouritesPending(guestIds.length);
        } else {
          setGuestFavouritesPending(0);
        }

        // Fetch the user's authentic account favourites from server
        try {
          const ids = await apiClient('/favourites/ids');
          if (active && Array.isArray(ids)) {
            setFavouriteIds(ids);
            setUserFavouriteIds(user.id, ids);
          }
        } catch (err) {
          console.error('Failed to fetch user favourite IDs', err);
          if (active) {
            setFavouriteIds(getUserFavouriteIds(user.id));
          }
        }
      } else {
        // Logged out / Guest mode:
        setGuestFavouritesPending(0);

        if (prevAuthRef.current && !isAuthenticated) {
          // User just logged out: clean wipe
          clearGuestFavourites();
          if (prevUserIdRef.current) {
            clearUserFavourites(prevUserIdRef.current);
          }
          setFavouriteIds([]);
          setFavourites([]);
        } else {
          // Regular guest visit: restore guest favourites
          setFavouriteIds(getGuestFavouriteIds());
        }
      }

      prevAuthRef.current = isAuthenticated;
      prevUserIdRef.current = user?.id;
    }

    handleAuthChange();

    return () => {
      active = false;
    };
  }, [isAuthenticated, user?.id, authLoading]);

  // Explicitly import guest favourites into the current user's account
  const importGuestFavourites = useCallback(async () => {
    if (!isAuthenticated || !user?.id) return;
    const guestIds = getGuestFavouriteIds();
    if (guestIds.length === 0) {
      setGuestFavouritesPending(0);
      return;
    }

    setSyncing(true);
    try {
      const res = await apiClient('/favourites/sync', {
        method: 'POST',
        body: { listingIds: guestIds }
      });
      if (res?.favouriteIds) {
        const merged = Array.from(res.favouriteIds);
        setFavouriteIds(merged);
        setUserFavouriteIds(user.id, merged);
        clearGuestFavourites();
        setGuestFavouritesPending(0);

        // Refresh favourites list if on Favourites page
        try {
          const updatedListings = await apiClient('/favourites');
          if (Array.isArray(updatedListings)) {
            setFavourites(updatedListings);
          }
        } catch {
          // ignore
        }
      }
    } catch (err) {
      console.error('Failed to import guest favourites into account', err);
    } finally {
      setSyncing(false);
    }
  }, [isAuthenticated, user?.id]);

  // Explicitly discard guest favourites without modifying account
  const discardGuestFavourites = useCallback(() => {
    clearGuestFavourites();
    setGuestFavouritesPending(0);
  }, []);

  // Check if a specific listing is in favourites
  const isFavourite = useCallback((listingId) => {
    if (!listingId) return false;
    const numId = Number(listingId);
    return favouriteIds.some(id => Number(id) === numId);
  }, [favouriteIds]);

  // Toggle favourite status
  const toggleFavourite = useCallback(async (listing) => {
    if (!listing || !listing.id) return;
    const listingId = Number(listing.id);
    const currentlyFav = isFavourite(listingId);

    // Optimistic state update
    const nextIds = currentlyFav
      ? favouriteIds.filter(id => Number(id) !== listingId)
      : [...favouriteIds, listingId];

    setFavouriteIds(nextIds);

    if (isAuthenticated && user?.id) {
      setUserFavouriteIds(user.id, nextIds);
    } else {
      setGuestFavouriteIds(nextIds);
    }

    // Also update full listings list if loaded
    if (currentlyFav) {
      setFavourites(prev => prev.filter(item => Number(item.id) !== listingId));
    }

    if (isAuthenticated) {
      try {
        if (currentlyFav) {
          await apiClient(`/favourites/${listingId}`, { method: 'DELETE' });
        } else {
          await apiClient(`/favourites/${listingId}`, { method: 'POST' });
        }
      } catch (err) {
        console.error('Failed to update favourite on server', err);
        // Rollback on error
        setFavouriteIds(favouriteIds);
        if (user?.id) {
          setUserFavouriteIds(user.id, favouriteIds);
        }
      }
    }
  }, [favouriteIds, isFavourite, isAuthenticated, user?.id]);

  // Remove favourite explicitly
  const removeFavourite = useCallback(async (listingId) => {
    if (!listingId) return;
    const numId = Number(listingId);
    const nextIds = favouriteIds.filter(id => Number(id) !== numId);
    setFavouriteIds(nextIds);

    if (isAuthenticated && user?.id) {
      setUserFavouriteIds(user.id, nextIds);
    } else {
      setGuestFavouriteIds(nextIds);
    }

    setFavourites(prev => prev.filter(item => Number(item.id) !== numId));

    if (isAuthenticated) {
      try {
        await apiClient(`/favourites/${numId}`, { method: 'DELETE' });
      } catch (err) {
        console.error('Failed to remove favourite on server', err);
      }
    }
  }, [favouriteIds, isAuthenticated, user?.id]);

  // Fetch full listings for Favourites page
  const fetchFavourites = useCallback(async (signal) => {
    setLoading(true);
    try {
      if (isAuthenticated) {
        const data = await apiClient('/favourites', signal ? { signal } : {});
        if (Array.isArray(data)) {
          setFavourites(data);
          const ids = data.map(l => l.id);
          setFavouriteIds(ids);
          if (user?.id) {
            setUserFavouriteIds(user.id, ids);
          }
        }
      } else {
        // Guest mode: fetch all listings and filter by stored guest favourite IDs
        const localIds = getGuestFavouriteIds();
        if (localIds.length === 0) {
          setFavourites([]);
        } else {
          const allListings = await apiClient('/listings', signal ? { signal } : {});
          if (Array.isArray(allListings)) {
            const idSet = new Set(localIds.map(Number));
            const filtered = allListings.filter(l => idSet.has(Number(l.id)));
            setFavourites(filtered);
          }
        }
      }
    } catch (err) {
      if (err.name === 'AbortError') return;
      console.error('Failed to fetch full favourites list', err);
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated, user?.id]);

  const value = useMemo(() => ({
    favouriteIds,
    favourites,
    isFavourite,
    toggleFavourite,
    removeFavourite,
    fetchFavourites,
    favouriteCount: favouriteIds.length,
    loading,
    syncing,
    isAuthenticated,
    guestFavouritesPending,
    importGuestFavourites,
    discardGuestFavourites,
  }), [
    favouriteIds,
    favourites,
    isFavourite,
    toggleFavourite,
    removeFavourite,
    fetchFavourites,
    loading,
    syncing,
    isAuthenticated,
    guestFavouritesPending,
    importGuestFavourites,
    discardGuestFavourites,
  ]);

  return (
    <FavouritesContext.Provider value={value}>
      {children}
    </FavouritesContext.Provider>
  );
}

export function useFavourites() {
  const context = useContext(FavouritesContext);
  if (!context) {
    throw new Error('useFavourites must be used within a FavouritesProvider');
  }
  return context;
}

export default FavouritesContext;
