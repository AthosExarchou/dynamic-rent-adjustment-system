import React, { useState } from 'react';
import apiClient from '../../../shared/api/client';
import styles from './AdminModals.module.css';

export default function AdminProfileCreationModal({ userId, roleType, onClose, onRefresh }) {
  const [formData, setFormData] = useState({ firstName: '', lastName: '', phoneNumber: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!userId || !roleType) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    // BUG-F02 FIX: Use URLSearchParams (form-encoded) instead of JSON to match
    // the Spring MVC backend's expected Content-Type for /owner/new and /tenant/new.
    // The userId is included so the admin can create profiles on behalf of other users.
    const params = new URLSearchParams({
      userId: String(userId),
      firstName: formData.firstName,
      lastName: formData.lastName,
      phoneNumber: formData.phoneNumber,
    });

    try {
      await apiClient(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
      });
      onRefresh();
      onClose();
    } catch (err) {
      setError(`Failed to create ${roleType.toLowerCase()} profile. Data might be invalid.`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modalCard}>
        <div className={styles.modalHeader}>
          <h3>Create {roleType === 'OWNER' ? 'Owner' : 'Tenant'} Profile</h3>
          <button onClick={onClose} className={styles.closeBtn}>&times;</button>
        </div>
        <div className={styles.modalBody}>
          <p className={styles.instruction}>
            This user does not have a {roleType.toLowerCase()} profile yet. You must provide their details to assign this role.
          </p>
          {error && <div className={styles.error}>{error}</div>}
          <form onSubmit={handleSubmit}>
            <div className={styles.formGroup}>
              <label htmlFor="modalFirstName" className={styles.label}>First Name</label>
              <input id="modalFirstName" type="text" required value={formData.firstName}
                     onChange={e => setFormData(
                         {...formData, firstName: e.target.value})} className={styles.input} />
            </div>
            <div className={styles.formGroup}>
              <label htmlFor="modalLastName" className={styles.label}>Last Name</label>
              <input id="modalLastName" type="text" required value={formData.lastName}
                     onChange={e => setFormData(
                         {...formData, lastName: e.target.value})} className={styles.input} />
            </div>
            <div className={styles.formGroup}>
              <label htmlFor="modalPhone" className={styles.label}>Phone Number</label>
              <input id="modalPhone" type="text" required value={formData.phoneNumber}
                     onChange={e => setFormData(
                         {...formData, phoneNumber: e.target.value})} className={styles.input} placeholder="+30 210 1234567" />
            </div>
            <div className={styles.modalFooter}>
              <button type="button" onClick={onClose} className={styles.cancelBtn}>Cancel</button>
              <button type="submit" disabled={loading} className={styles.saveBtn}>
                {loading ? 'Creating...' : `Create ${roleType}`}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
