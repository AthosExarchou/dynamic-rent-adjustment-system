import React, { useState } from 'react';
import apiClient from '../../../shared/api/client';
import styles from './AdminModals.module.css';

export default function AdminProfileCreationModal({ userId, roleType, onClose, onRefresh }) {
  const [formData, setFormData] = useState({ firstName: '', lastName: '', phoneNumber: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [errorDetails, setErrorDetails] = useState([]);

  if (!userId || !roleType) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setErrorDetails([]);

    const endpoint = roleType === 'OWNER' ? '/owner/new' : '/tenant/new';
    try {
      await apiClient(endpoint, {
        method: 'POST',
        body: {
          userId: Number(userId),
          firstName: formData.firstName,
          lastName: formData.lastName,
          phoneNumber: formData.phoneNumber
        }
      });
      onRefresh();
      onClose();
    } catch (err) {
      setError(err.message || `Failed to create ${roleType.toLowerCase()} profile. Data might be invalid.`);
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
          <h3>Create {roleType === 'OWNER' ? 'Owner' : 'Tenant'} Profile</h3>
          <button onClick={onClose} className={styles.closeBtn}>&times;</button>
        </div>
        <div className={styles.modalBody}>
          <p className={styles.instruction}>
            This user does not have a {roleType.toLowerCase()} profile yet. You must provide their details to assign this role.
          </p>
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
              <label htmlFor="modalFirstName" className={styles.label}>First Name</label>
              <input id="modalFirstName" type="text" required maxLength={20} value={formData.firstName}
                     onChange={e => setFormData(
                         {...formData, firstName: e.target.value})} className={styles.input} />
            </div>
            <div className={styles.formGroup}>
              <label htmlFor="modalLastName" className={styles.label}>Last Name</label>
              <input id="modalLastName" type="text" required maxLength={20} value={formData.lastName}
                     onChange={e => setFormData(
                         {...formData, lastName: e.target.value})} className={styles.input} />
            </div>
            <div className={styles.formGroup}>
              <label htmlFor="modalPhone" className={styles.label}>Phone Number</label>
              <input id="modalPhone" type="tel" required
                     minLength={7}
                     maxLength={25}
                     pattern="^\+?[0-9. ()-]{7,25}$"
                     title="Enter a valid phone number with 7-25 digits."
                     value={formData.phoneNumber}
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
