import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth';
import apiClient from '../../../shared/api/client';
import styles from './TenantForm.module.css';

export default function TenantForm() {
  const { listingId } = useParams();
  const { roles } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ firstName: '', lastName: '', phoneNumber: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const isTenant = roles.includes('TENANT');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    const params = new URLSearchParams();
    if (!isTenant) {
      params.append('firstName', formData.firstName);
      params.append('lastName', formData.lastName);
      params.append('phoneNumber', formData.phoneNumber);
    }

    try {
      await apiClient(`/tenant/rent/${listingId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
      });
      alert('Application submitted successfully!');
      navigate('/listings');
    } catch {
      setError('Failed to submit application. Profile may already exist.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h2 className={styles.title}>Tenant Rental Application</h2>
        {error && <div className={styles.error}>{error}</div>}
        
        <form onSubmit={handleSubmit}>
          {!isTenant ? (
            <div className={styles.tenantSection}>
              <h4 className={styles.sectionTitle}>Create Tenant Profile</h4>
              <div>
                <label htmlFor="firstName" className={styles.label}>First Name</label>
                <input id="firstName" type="text" required value={formData.firstName} onChange={e => setFormData({...formData, firstName: e.target.value})} className={styles.input} />
              </div>
              <div>
                <label htmlFor="lastName" className={styles.label}>Last Name</label>
                <input id="lastName" type="text" required value={formData.lastName} onChange={e => setFormData({...formData, lastName: e.target.value})} className={styles.input} />
              </div>
              <div>
                <label htmlFor="phoneNumber" className={styles.label}>Phone Number</label>
                <input id="phoneNumber" type="text" required value={formData.phoneNumber} onChange={e => setFormData({...formData, phoneNumber: e.target.value})} className={styles.input} placeholder="+30 690 1234567" />
              </div>
            </div>
          ) : (
            <p className={styles.existingProfileMsg}>Using your existing Tenant Profile. Confirm application below:</p>
          )}

          <button type="submit" disabled={loading} className={styles.submitBtn}>
            {loading ? 'Submitting Application...' : 'Confirm & Submit Application'}
          </button>
        </form>
      </div>
    </div>
  );
}
