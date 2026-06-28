import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../auth';
import { Key, CheckCircle, ArrowLeft, Lock, Unlock } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import styles from './ProfileForm.module.css';

export default function ChangePasswordForm() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let timeoutId;
    if (success) {
      timeoutId = setTimeout(() => navigate('/profile'), 2000);
    }
    return () => {
      if (timeoutId) clearTimeout(timeoutId);
    };
  }, [success, navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (formData.newPassword !== formData.confirmPassword) {
      setError('New password confirmations do not match.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const { confirmPassword: _ignored, ...payloadData } = formData;
      await apiClient(`/user/change-password/${user?.id}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams(payloadData).toString()
      });
      setSuccess(true);
    } catch {
      setError('Failed to change password. Old password may be incorrect.');
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
            <Key className={styles.titleIcon} size={24} /> Change Password
          </h2>
        </div>
        
        <div className={styles.body}>
          {error && <div className={`${styles.alert} ${styles.alertDanger}`}>{error}</div>}
          {success && (
            <div className={`${styles.alert} ${styles.alertSuccess}`}>
              <CheckCircle size={18} /> Password changed successfully! Redirecting...
            </div>
          )}

          <form onSubmit={handleSubmit} className={styles.form}>
            <div className={styles.formGroup}>
              <label htmlFor="oldPassword" className={styles.label}>
                <Unlock size={16} /> Current Password
              </label>
              <input 
                id="oldPassword" 
                type="password" 
                required 
                value={formData.oldPassword} 
                onChange={e => setFormData({...formData, oldPassword: e.target.value})} 
                className={styles.input} 
              />
            </div>
            
            <div className={styles.formGroup}>
              <label htmlFor="newPassword" className={styles.label}>
                <Lock size={16} /> New Password
              </label>
              <input 
                id="newPassword" 
                type="password" 
                required 
                value={formData.newPassword} 
                onChange={e => setFormData({...formData, newPassword: e.target.value})} 
                className={styles.input} 
              />
            </div>
            
            <div className={styles.formGroup}>
              <label htmlFor="confirmPassword" className={styles.label}>
                <Lock size={16} /> Confirm New Password
              </label>
              <input 
                id="confirmPassword" 
                type="password" 
                required 
                value={formData.confirmPassword} 
                onChange={e => setFormData({...formData, confirmPassword: e.target.value})} 
                className={styles.input} 
              />
            </div>
            
            <hr className={styles.divider} />
            
            <div className={styles.formActions}>
              <button type="submit" disabled={loading || success} className={`${styles.btn} ${styles.btnPrimary}`}>
                {loading ? 'Updating Password...' : 'Update Password'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
