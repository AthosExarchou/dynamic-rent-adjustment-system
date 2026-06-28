import React, { Suspense, lazy } from 'react';
import { createBrowserRouter, Navigate, useRouteError } from 'react-router-dom';
import MainLayout from '../../shared/layouts/MainLayout';
import styles from './Loading.module.css';

// Lazy load all feature components so the pages load faster
const LandingPage = lazy(() => import('../../features/public/components/LandingPage'));
const AboutPage = lazy(() => import('../../features/public/components/AboutPage'));
const ContactPage = lazy(() => import('../../features/public/components/ContactPage'));
const PrivacyPolicy = lazy(() => import('../../features/public/components/PrivacyPolicy'));
const TermsOfService = lazy(() => import('../../features/public/components/TermsOfService'));
const NotFoundPage = lazy(() => import('../../features/public/components/NotFoundPage'));

const LoginForm = lazy(() => import('../../features/auth/components/LoginForm'));
const RegisterForm = lazy(() => import('../../features/auth/components/RegisterForm'));

const ListingList = lazy(() => import('../../features/listings/components/ListingList'));
const ListingDetail = lazy(() => import('../../features/listings/components/ListingDetail'));
const ListingForm = lazy(() => import('../../features/listings/components/ListingForm'));
const MyListings = lazy(() => import('../../features/listings/components/MyListings'));
const ListingApplications = lazy(() => import('../../features/listings/components/ListingApplications'));

const Profile = lazy(() => import('../../features/users/components/Profile'));
const ProfileEditForm = lazy(() => import('../../features/users/components/ProfileEditForm'));
const ChangePasswordForm = lazy(() => import('../../features/users/components/ChangePasswordForm'));
const DeleteAccountConfirm = lazy(() => import('../../features/users/components/DeleteAccountConfirm'));

const UserManagement = lazy(() => import('../../features/admin/components/UserManagement'));
const PendingApprovals = lazy(() => import('../../features/admin/components/PendingApprovals'));

const TenantForm = lazy(() => import('../../features/tenants/components/TenantForm'));

// Professional loading fallback
const PageLoader = () => (
  <div className={styles.loaderContainer}>
    <div className={styles.spinner}></div>
  </div>
);

// Wrapper to apply Suspense to a route component
const withSuspense = (Component) => (
  <Suspense fallback={<PageLoader />}>
    <Component />
  </Suspense>
);

// Global Error Boundary to catch Chunk Loading errors (dynamically imported module failures)

const GlobalErrorBoundary = () => {
  const error = useRouteError();
  
  if (error && error.message && error.message.includes('Failed to fetch dynamically imported module')) {
    const reloadCount = parseInt(sessionStorage.getItem('chunk_reload_count') || '0', 10);
    if (reloadCount < 2) {
      sessionStorage.setItem('chunk_reload_count', String(reloadCount + 1));
      window.location.reload();
      return <PageLoader />;
    }
  }

  return (
    <div style={{ padding: '4rem 2rem', textAlign: 'center', fontFamily: 'system-ui' }}>
      <h2 style={{ marginBottom: '1rem' }}>Oops! Something went wrong.</h2>
      <p style={{ color: '#666', marginBottom: '2rem' }}>{error?.message || "An unexpected error occurred."}</p>
      <button 
        onClick={() => window.location.href = '/'}
        style={{ padding: '0.5rem 1.5rem', backgroundColor: '#0d6efd', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
      >
        Return to Home
      </button>
    </div>
  );
};

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    errorElement: <GlobalErrorBoundary />,
    children: [
      // Public Routes
      { index: true, element: withSuspense(LandingPage) },
      { path: 'about', element: withSuspense(AboutPage) },
      { path: 'contact', element: withSuspense(ContactPage) },
      { path: 'privacy', element: withSuspense(PrivacyPolicy) },
      { path: 'terms', element: withSuspense(TermsOfService) },
      { path: 'listings', element: withSuspense(ListingList) },
      { path: 'listings/:id', element: withSuspense(ListingDetail) },
      { path: 'login', element: withSuspense(LoginForm) },
      { path: 'register', element: withSuspense(RegisterForm) },

      // Protected User Routes
      { path: 'profile', element: withSuspense(Profile) },
      { path: 'profile/edit', element: withSuspense(ProfileEditForm) },
      { path: 'profile/password', element: withSuspense(ChangePasswordForm) },
      { path: 'profile/delete', element: withSuspense(DeleteAccountConfirm) },
      { path: 'listings/new', element: withSuspense(ListingForm) },
      { path: 'tenant/rent/:listingId', element: withSuspense(TenantForm) },

      // Owner Routes
      { path: 'my-listings', element: withSuspense(MyListings) },
      { path: 'my-listings/:listingId/apps', element: withSuspense(ListingApplications) },

      // Admin Routes
      { path: 'admin/users', element: withSuspense(UserManagement) },
      { path: 'admin/approvals', element: withSuspense(PendingApprovals) },

      // Catch-all (404 Not Found)
      { path: '*', element: withSuspense(NotFoundPage) }
    ]
  }
]);

export default router;
