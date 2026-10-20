import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Phone, CheckCircle, ChevronRight, Briefcase } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import { useAuth } from '../../auth';
import { OWNER_CONSTRAINTS } from '../models/owner';
import styles from './OwnerForm.module.css';

export default function OwnerForm() {
  const { user, refreshUser } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ firstName: '', lastName: '', phoneNumber: '' });
  const [error, setError] = useState('');
  const [errorDetails, setErrorDetails] = useState([]);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setErrorDetails([]);

    try {
      await apiClient(`/owner/new`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: { ...formData, userId: user?.id }
      });
      await refreshUser();
      alert('Owner profile created successfully!');
      navigate('/profile'); 
    } catch (err) {
      setError(err.message || 'Failed to create owner profile. Please check your details or you might already have one.');
      if (Array.isArray(err.details) && err.details.length > 0) {
        setErrorDetails(err.details);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={`${styles.card} ${styles.cardHover}`}>
        <div className={styles.header}>
          <h3 className={styles.title}>
            <Briefcase className={styles.titleIcon} size={28} />
            Register as Owner
          </h3>
        </div>
        
        <div className={styles.body}>
          {error && (
            <div className={`${styles.alert} ${styles.alertDanger}`}>
              <div>{error}</div>
              {errorDetails.length > 1 && (
                <ul style={{ marginTop: '0.5rem', marginBottom: 0, paddingLeft: '1.25rem' }}>
                  {errorDetails.map((detail, idx) => (
                    <li key={idx}>{detail}</li>
                  ))}
                </ul>
              )}
            </div>
          )}
          
          <div className={styles.sectionNotice}>
            <p>Register as a property owner on DRAS to create and manage apartment listings.</p>
          </div>
          
          <form onSubmit={handleSubmit} className={styles.form}>
            <div className={styles.formGrid}>
              <div className={styles.formGroup}>
                <label htmlFor="ownerFirstName" className={styles.label}>First Name</label>
                <input 
                  id="ownerFirstName" 
                  type="text" 
                  required 
                  maxLength={OWNER_CONSTRAINTS.FIRST_NAME_MAX}
                  value={formData.firstName}
                  onChange={e => setFormData({...formData, firstName: e.target.value})} 
                  className={styles.input} 
                  placeholder="e.g. John" 
                />
              </div>
              
              <div className={styles.formGroup}>
                <label htmlFor="ownerLastName" className={styles.label}>Last Name</label>
                <input 
                  id="ownerLastName" 
                  type="text" 
                  required 
                  maxLength={OWNER_CONSTRAINTS.LAST_NAME_MAX}
                  value={formData.lastName}
                  onChange={e => setFormData({...formData, lastName: e.target.value})} 
                  className={styles.input} 
                  placeholder="e.g. Doe" 
                />
              </div>
              
              <div className={styles.formGroup}>
                <label htmlFor="ownerPhone" className={styles.label}>
                  <Phone size={16} /> Phone Number
                </label>
                <input 
                  id="ownerPhone" 
                  type="tel" 
                  required 
                  minLength={OWNER_CONSTRAINTS.PHONE_MIN}
                  maxLength={OWNER_CONSTRAINTS.PHONE_MAX}
                  value={formData.phoneNumber}
                  onChange={e => setFormData({...formData, phoneNumber: e.target.value})} 
                  className={styles.input} 
                  placeholder="+30 210 1234567" 
                  pattern="^\+?[0-9. ()-]{7,25}$"
                  title="Enter a valid phone number with 7-25 digits."
                />
              </div>
            </div>

            <hr className={styles.divider} />

            <div className={styles.formActions}>
              <button type="submit" disabled={loading} className={`${styles.btn} ${styles.btnSuccess}`}>
                {loading ? (
                  'Submitting...'
                ) : (
                  <>
                    <CheckCircle size={20} /> Create Owner Profile <ChevronRight size={18} />
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
