import React from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import MainLayout from '../../shared/layouts/MainLayout';

// Feature Components
import LandingPage from '../../features/public/components/LandingPage';
import AboutPage from '../../features/public/components/AboutPage';
import ContactPage from '../../features/public/components/ContactPage';
import PrivacyPolicy from '../../features/public/components/PrivacyPolicy';
import TermsOfService from '../../features/public/components/TermsOfService';

import LoginForm from '../../features/auth/components/LoginForm';
import RegisterForm from '../../features/auth/components/RegisterForm';

import ListingList from '../../features/listings/components/ListingList';
import ListingDetail from '../../features/listings/components/ListingDetail';
import ListingForm from '../../features/listings/components/ListingForm';
import MyListings from '../../features/listings/components/MyListings';
import ListingApplications from '../../features/listings/components/ListingApplications';

import Profile from '../../features/users/components/Profile';
import ProfileEditForm from '../../features/users/components/ProfileEditForm';
import ChangePasswordForm from '../../features/users/components/ChangePasswordForm';
import DeleteAccountConfirm from '../../features/users/components/DeleteAccountConfirm';

import UserManagement from '../../features/admin/components/UserManagement';
import PendingApprovals from '../../features/admin/components/PendingApprovals';

import TenantForm from '../../features/tenants/components/TenantForm';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      // Public Routes
      { index: true, element: <LandingPage /> },
      { path: 'about', element: <AboutPage /> },
      { path: 'contact', element: <ContactPage /> },
      { path: 'privacy', element: <PrivacyPolicy /> },
      { path: 'terms', element: <TermsOfService /> },
      { path: 'listings', element: <ListingList /> },
      { path: 'listings/:id', element: <ListingDetail /> },
      { path: 'login', element: <LoginForm /> },
      { path: 'register', element: <RegisterForm /> },

      // Protected User Routes
      { path: 'profile', element: <Profile /> },
      { path: 'profile/edit', element: <ProfileEditForm /> },
      { path: 'profile/password', element: <ChangePasswordForm /> },
      { path: 'profile/delete', element: <DeleteAccountConfirm /> },
      { path: 'listings/new', element: <ListingForm /> },
      { path: 'tenant/rent/:listingId', element: <TenantForm /> },

      // Owner Routes
      { path: 'my-listings', element: <MyListings /> },
      { path: 'my-listings/:listingId/apps', element: <ListingApplications /> },

      // Admin Routes
      { path: 'admin/users', element: <UserManagement /> },
      { path: 'admin/approvals', element: <PendingApprovals /> },

      // Catch-all
      { path: '*', element: <Navigate to="/" replace /> }
    ]
  }
]);

export default router;
