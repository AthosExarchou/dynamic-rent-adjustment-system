import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../features/auth';

export default function ProtectedRoute({ children, requiredRole }) {
  const { isAuthenticated, isLoading, hasRole } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: '4rem' }}>
        <div style={{
          width: '30px', height: '30px', border: '3px solid #f3f3f3', 
          borderTop: '3px solid #0d6efd', borderRadius: '50%', animation: 'spin 1s linear infinite'
        }} />
        <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (requiredRole && !hasRole(requiredRole)) {
    return (
      <div style={{ padding: '4rem 2rem', textAlign: 'center', fontFamily: 'system-ui' }}>
        <h2 style={{ color: '#dc3545' }}>Access Denied</h2>
        <p style={{ color: '#666' }}>You do not have the required permissions to view this page.</p>
      </div>
    );
  }

  return children;
}
