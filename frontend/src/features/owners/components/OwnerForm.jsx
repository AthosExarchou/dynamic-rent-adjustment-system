import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../../../shared/api/client';
import styles from './OwnerForm.module.css';
import { useAuth } from '../../auth';

export default function OwnerForm() {
  const navigate = useNavigate();
  const { fetchUserRoles } = useAuth(); // Assume we have this, or we can just navigate to profile/dashboard
  const [formData, setFormData] = useState({ firstName: '', lastName: '', phoneNumber: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      await apiClient(`/owner/new`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: formData
      });
      alert('Owner profile created successfully!');
      navigate('/profile'); 
    } catch (err) {
      setError('Failed to create owner profile. Please check your details or you might already have one.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h2 className={styles.title}>Register as Owner</h2>
        {error && <div className={styles.error}>{error}</div>}
        
        <form onSubmit={handleSubmit}>
          <div className={styles.formSection}>
            <div>
              <label htmlFor="ownerFirstName" className={styles.label}>First Name</label>
              <input id="ownerFirstName" type="text" required value={formData.firstName}
                     onChange={e => setFormData(
                         {...formData, firstName: e.target.value})} className={styles.input} />
            </div>
            <div>
              <label htmlFor="ownerLastName" className={styles.label}>Last Name</label>
              <input id="ownerLastName" type="text" required value={formData.lastName}
                     onChange={e => setFormData(
                         {...formData, lastName: e.target.value})} className={styles.input} />
            </div>
            <div>
              <label htmlFor="ownerPhone" className={styles.label}>Phone Number</label>
              <input id="ownerPhone" type="text" required value={formData.phoneNumber}
                     onChange={e => setFormData(
                         {...formData, phoneNumber: e.target.value})} className={styles.input} placeholder="+30 210 1234567" />
            </div>
          </div>

          <button type="submit" disabled={loading} className={styles.submitBtn}>
            {loading ? 'Submitting...' : 'Create Owner Profile'}
          </button>
        </form>
      </div>
    </div>
  );
}
