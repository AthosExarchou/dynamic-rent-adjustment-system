import React, { useState, useEffect } from 'react';
import apiClient from '../../../shared/api/client';
import styles from './AdminModals.module.css';

export default function EditUserModal({ user, onClose, onRefresh }) {
  const [formData, setFormData] = useState({ username: '', email: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (user) {
      setFormData({ username: user.username, email: user.email });
    }
  }, [user]);

  if (!user) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      await apiClient(`/user/${user.id}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams(formData).toString()
      });
      onRefresh();
      onClose();
    } catch {
      setError('Failed to update user. Username or email may be taken.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modalCard}>
        <div className={styles.modalHeader}>
          <h3>Edit User Details</h3>
          <button onClick={onClose} className={styles.closeBtn}>&times;</button>
        </div>
        <div className={styles.modalBody}>
          {error && <div className={styles.error}>{error}</div>}
          <form onSubmit={handleSubmit}>
            <div className={styles.formGroup}>
              <label htmlFor="editUsername" className={styles.label}>Username</label>
              <input id="editUsername" type="text" required value={formData.username}
                     onChange={e => setFormData(
                         {...formData, username: e.target.value})} className={styles.input} />
            </div>
            <div className={styles.formGroup}>
              <label htmlFor="editEmail" className={styles.label}>Email Address</label>
              <input id="editEmail" type="email" required value={formData.email}
                     onChange={e => setFormData(
                         {...formData, email: e.target.value})} className={styles.input} />
            </div>
            <div className={styles.modalFooter}>
              <button type="button" onClick={onClose} className={styles.cancelBtn}>Cancel</button>
              <button type="submit" disabled={loading} className={styles.saveBtn}>
                {loading ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
