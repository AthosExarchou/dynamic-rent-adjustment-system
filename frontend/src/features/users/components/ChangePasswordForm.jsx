import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth';
import apiClient from '../../../shared/api/client';
import styles from './ProfileForm.module.css';

export default function ChangePasswordForm() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (formData.newPassword !== formData.confirmPassword) {
      setError('New password confirmations do not match.');
      return;
    }
    setError('');
    try {
      await apiClient(`/user/change-password/${user?.id}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams(formData).toString()
      });
      setSuccess(true);
      setTimeout(() => navigate('/profile'), 2000);
    } catch {
      setError('Failed to change password. Old password may be incorrect.');
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h2 className={styles.title}>Change Password</h2>
        {error && <div className={styles.error}>{error}</div>}
        {success && <div className={styles.success}>Password changed successfully!</div>}

        <form onSubmit={handleSubmit}>
          <div className={styles.formGroup}>
            <label htmlFor="oldPassword" className={styles.label}>Current Password</label>
            <input id="oldPassword" type="password" required value={formData.oldPassword} onChange={e => setFormData({...formData, oldPassword: e.target.value})} className={styles.input} />
          </div>
          <div className={styles.formGroup}>
            <label htmlFor="newPassword" className={styles.label}>New Password</label>
            <input id="newPassword" type="password" required value={formData.newPassword} onChange={e => setFormData({...formData, newPassword: e.target.value})} className={styles.input} />
          </div>
          <div className={styles.formGroupLarge}>
            <label htmlFor="confirmPassword" className={styles.label}>Confirm New Password</label>
            <input id="confirmPassword" type="password" required value={formData.confirmPassword} onChange={e => setFormData({...formData, confirmPassword: e.target.value})} className={styles.input} />
          </div>

          <button type="submit" className={styles.primaryBtn}>Update Password</button>
        </form>
      </div>
    </div>
  );
}
