import React, { useState } from 'react';
import apiClient from '../../../shared/api/client';
import { USER_CONSTRAINTS } from '../../users/models/user';
import styles from './AdminModals.module.css';

export default function CreateUserModal({ onClose, onRefresh }) {
  const [formData, setFormData] = useState({ username: '', email: '', password: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [errorDetails, setErrorDetails] = useState([]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setErrorDetails([]);

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
      if (Array.isArray(err.details) && err.details.length > 0) {
        setErrorDetails(err.details);
      }
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
          {error && (
            <div className={styles.error}>
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
          <form onSubmit={handleSubmit}>
            <div className={styles.formGroup}>
              <label htmlFor="createUsername" className={styles.label}>Username</label>
              <input id="createUsername" type="text" required maxLength={USER_CONSTRAINTS.USERNAME_MAX} value={formData.username}
                     onChange={e => setFormData({...formData, username: e.target.value})} 
                     className={styles.input} />
            </div>
            <div className={styles.formGroup}>
              <label htmlFor="createEmail" className={styles.label}>Email Address</label>
              <input id="createEmail" type="email" required maxLength={USER_CONSTRAINTS.EMAIL_MAX} value={formData.email}
                     onChange={e => setFormData({...formData, email: e.target.value})} 
                     className={styles.input} />
            </div>
            <div className={styles.formGroup}>
              <label htmlFor="createPassword" className={styles.label}>Password</label>
              <input id="createPassword" type="password" required maxLength={USER_CONSTRAINTS.PASSWORD_MAX} value={formData.password}
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
