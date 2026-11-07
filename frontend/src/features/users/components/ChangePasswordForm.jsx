import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../auth';
import { useToast } from '../../../shared/components/Toast';
import { Key, ArrowLeft, Lock, Unlock, AlertCircle } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import styles from './ProfileForm.module.css';

export default function ChangePasswordForm() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [formData, setFormData] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (formData.newPassword !== formData.confirmPassword) {
      const errorMsg = 'New password confirmations do not match.';
      setError(errorMsg);
      return;
    }
    setError('');
    setLoading(true);
    try {
      await apiClient(`/user/change-password/${user?.id}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams(formData).toString()
      });
      toast.success('Password changed successfully.');
      navigate('/profile');
    } catch (err) {
      const errorMsg = err.message || 'Failed to change password. Old password may be incorrect.';
      setError(errorMsg);
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
          {error && (
            <div className={`${styles.alert} ${styles.alertDanger}`}>
              <AlertCircle size={20} className={styles.alertIcon} />
              <div className={styles.alertContent}>{error}</div>
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
                maxLength={100}
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
                maxLength={100}
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
                maxLength={100}
                value={formData.confirmPassword} 
                onChange={e => setFormData({...formData, confirmPassword: e.target.value})} 
                className={styles.input} 
              />
            </div>
            
            <hr className={styles.divider} />
            
            <div className={styles.formActions}>
              <button type="submit" disabled={loading} className={`${styles.btn} ${styles.btnPrimary}`}>
                {loading ? 'Updating Password...' : 'Update Password'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
