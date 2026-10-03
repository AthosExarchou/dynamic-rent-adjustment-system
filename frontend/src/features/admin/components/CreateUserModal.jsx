import React, { useState } from 'react';
import apiClient from '../../../shared/api/client';
import styles from './AdminModals.module.css';

export default function CreateUserModal({ onClose, onRefresh }) {
  const [formData, setFormData] = useState({ username: '', email: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    const params = new URLSearchParams(formData);

    try {
      await apiClient('/saveUser', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
      });
      onRefresh();
      onClose();
    } catch (err) {
      setError(err.message || 'Failed to create user. Username or email may be taken.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modalCard}>
        <div className={styles.modalHeader}>
          <h3>Create New User</h3>
          <button onClick={onClose} className={styles.closeBtn}>&times;</button>
        </div>
        <div className={styles.modalBody}>
          {error && <div className={styles.error}>{error}</div>}
          <form onSubmit={handleSubmit}>
            <div className={styles.formGroup}>
              <label htmlFor="createUsername" className={styles.label}>Username</label>
              <input id="createUsername" type="text" required value={formData.username}
                     onChange={e => setFormData({...formData, username: e.target.value})} 
                     className={styles.input} />
            </div>
            <div className={styles.formGroup}>
              <label htmlFor="createEmail" className={styles.label}>Email Address</label>
              <input id="createEmail" type="email" required value={formData.email}
                     onChange={e => setFormData({...formData, email: e.target.value})} 
                     className={styles.input} />
            </div>
            <div className={styles.formGroup}>
              <label htmlFor="createPassword" className={styles.label}>Password</label>
              <input id="createPassword" type="password" required value={formData.password}
                     onChange={e => setFormData({...formData, password: e.target.value})} 
                     className={styles.input} />
            </div>
            <div className={styles.modalFooter}>
              <button type="button" onClick={onClose} className={styles.cancelBtn}>Cancel</button>
              <button type="submit" disabled={loading} className={styles.saveBtn}>
                {loading ? 'Creating...' : 'Create User'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
