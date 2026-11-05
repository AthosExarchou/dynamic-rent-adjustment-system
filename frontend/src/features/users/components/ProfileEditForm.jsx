import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../auth';
import { useToast } from '../../../shared/components/Toast';
import { Settings, User, Mail, ArrowLeft } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import { USER_CONSTRAINTS } from '../models/user';
import styles from './ProfileForm.module.css';

export default function ProfileEditForm() {
  const { user, refreshUser, hasRole, logout } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [formData, setFormData] = useState({ username: user?.username || '', email: user?.email || '' });
  const [error, setError] = useState('');
  const [errorDetails, setErrorDetails] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (user) {
      setFormData({ username: user.username || '', email: user.email || '' });
    }
  }, [user]);

  if (hasRole('ADMIN') || user?.isSystem) {
    return (
      <div className={styles.container}>
        <div className={styles.topActions}>
          <Link to="/profile" className={styles.backLink}>
            <ArrowLeft size={18} /> Back to Profile
          </Link>
        </div>
        <div className={styles.card}>
          <div className={styles.body} style={{ textAlign: 'center', padding: '3rem' }}>
            <h3 style={{ color: '#dc3545' }}>Action Not Allowed</h3>
            <p style={{ color: '#666' }}>Administrator and System accounts cannot modify their core profile details.</p>
          </div>
        </div>
      </div>
    );
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setErrorDetails([]);
    setLoading(true);
    try {
      const res = await apiClient(`/user/${user?.id}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          username: formData.username,
          email: formData.email
        }).toString()
      });
      if (res?.requiresLogin || !hasRole('ADMIN')) {
        if (logout) await logout();
        toast.success(res?.message || 'Profile details updated, please log in again.');
        navigate('/login');
        return;
      }
      if (refreshUser) await refreshUser();
      toast.success('Profile details updated successfully.');
      navigate('/profile');
    } catch (err) {
      const errorMsg = err.message || 'Failed to update details. Email or Username may be taken.';
      setError(errorMsg);
      toast.error(errorMsg);
      if (Array.isArray(err.details) && err.details.length > 0) {
        setErrorDetails(err.details);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.topActions}>
        <Link to="/profile" className={styles.backLink}>
          <ArrowLeft size={18} /> Back to Profile
        </Link>
      </div>

      <div className={`${styles.card} ${styles.cardHover}`}>
        <div className={styles.header}>
          <h2 className={styles.title}>
            <Settings className={styles.titleIcon} size={24} /> Edit Profile Details
          </h2>
        </div>
        
        <div className={styles.body}>
          {error && (
            <div className={`${styles.alert} ${styles.alertDanger}`}>
              <div>{error}</div>
              {errorDetails && errorDetails.length > 1 && (
                <ul style={{ marginTop: '0.5rem', marginBottom: 0, paddingLeft: '1.25rem' }}>
                  {errorDetails.map((detail, idx) => (
                    <li key={idx}>{detail}</li>
                  ))}
                </ul>
              )}
            </div>
          )}

          <form onSubmit={handleSubmit} className={styles.form}>
            <div className={styles.formGroup}>
              <label htmlFor="username" className={styles.label}>
                <User size={16} /> Username
              </label>
              <input 
                id="username" 
                type="text" 
                required 
                maxLength={USER_CONSTRAINTS.USERNAME_MAX}
                value={formData.username} 
                onChange={e => setFormData({...formData, username: e.target.value})} 
                className={styles.input} 
              />
            </div>
            
            <div className={styles.formGroup}>
              <label htmlFor="email" className={styles.label}>
                <Mail size={16} /> Email Address
              </label>
              <input 
                id="email" 
                type="email" 
                required 
                maxLength={USER_CONSTRAINTS.EMAIL_MAX}
                value={formData.email} 
                onChange={e => setFormData({...formData, email: e.target.value})} 
                className={styles.input} 
              />
            </div>
            
            <hr className={styles.divider} />
            
            <div className={styles.formActions}>
              <button type="submit" disabled={loading || success} className={`${styles.btn} ${styles.btnPrimary}`}>
                {loading ? 'Saving Changes...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
