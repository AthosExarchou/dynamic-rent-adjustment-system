import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth';
import apiClient from '../../../shared/api/client';
import styles from './ProfileForm.module.css';

export default function ProfileEditForm() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ username: user?.username || '', email: user?.email || '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await apiClient(`/user/edit/${user?.id}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          username: formData.username,
          email: formData.email
        }).toString()
      });
      setSuccess(true);
      setTimeout(() => navigate('/profile'), 2000);
    } catch {
      setError('Failed to update details. Email or Username may be taken.');
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h2 className={styles.title}>Edit Profile</h2>
        {error && <div className={styles.error}>{error}</div>}
        {success && <div className={styles.success}>Profile details updated!</div>}

        <form onSubmit={handleSubmit}>
          <div className={styles.formGroup}>
            <label htmlFor="username" className={styles.label}>Username</label>
            <input id="username" type="text" required value={formData.username} onChange={e => setFormData({...formData, username: e.target.value})} className={styles.input} />
          </div>
          <div className={styles.formGroupLarge}>
            <label htmlFor="email" className={styles.label}>Email Address</label>
            <input id="email" type="email" required value={formData.email} onChange={e => setFormData({...formData, email: e.target.value})} className={styles.input} />
          </div>
          
          <button type="submit" className={styles.submitBtn}>Save Changes</button>
        </form>
      </div>
    </div>
  );
}
