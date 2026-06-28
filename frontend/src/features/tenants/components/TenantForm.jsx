import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth';
import { UserPlus, UserCheck, Phone, CheckCircle, ChevronRight } from 'lucide-react';
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

    const requestOptions = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    };

    if (!isTenant) {
      requestOptions.body = {
        firstName: formData.firstName,
        lastName: formData.lastName,
        phoneNumber: formData.phoneNumber
      };
    }

    try {
      await apiClient(`/tenant/rent/${listingId}`, requestOptions);
      alert('Application submitted successfully!');
      navigate('/listings');
    } catch {
      setError('Failed to submit application. Profile may already exist or there was a server error.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={`${styles.card} ${styles.cardHover}`}>
        <div className={styles.header}>
          <h3 className={styles.title}>
            <UserPlus className={styles.titleIcon} size={28} />
            Tenant Rental Application
          </h3>
        </div>
        
        <div className={styles.body}>
          {error && <div className={`${styles.alert} ${styles.alertDanger}`}>{error}</div>}
          
          <form onSubmit={handleSubmit} className={styles.form}>
            {!isTenant ? (
              <>
                <div className={styles.sectionNotice}>
                  <p>You need to create a Tenant Profile before applying. Please fill out your details below.</p>
                </div>
                
                <div className={styles.formGrid}>
                  <div className={styles.formGroup}>
                    <label htmlFor="firstName" className={styles.label}>First Name</label>
                    <input 
                      id="firstName" 
                      type="text" 
                      required 
                      value={formData.firstName}
                      onChange={e => setFormData({...formData, firstName: e.target.value})} 
                      className={styles.input} 
                      placeholder="e.g. John" 
                    />
                  </div>
                  
                  <div className={styles.formGroup}>
                    <label htmlFor="lastName" className={styles.label}>Last Name</label>
                    <input 
                      id="lastName" 
                      type="text" 
                      required 
                      value={formData.lastName}
                      onChange={e => setFormData({...formData, lastName: e.target.value})} 
                      className={styles.input} 
                      placeholder="e.g. Doe" 
                    />
                  </div>
                  
                  <div className={styles.formGroup}>
                    <label htmlFor="phoneNumber" className={styles.label}>
                      <Phone size={16} /> Phone Number
                    </label>
                    <input 
                      id="phoneNumber" 
                      type="tel" 
                      required 
                      value={formData.phoneNumber}
                      onChange={e => setFormData({...formData, phoneNumber: e.target.value})} 
                      className={styles.input} 
                      placeholder="+30 690 1234567" 
                      pattern="^\+?[0-9. ()-]{7,25}$"
                      title="Enter a valid phone number (7-25 digits)"
                    />
                  </div>
                </div>
              </>
            ) : (
              <div className={styles.existingProfileMsg}>
                <div className={styles.iconCircle}>
                  <UserCheck size={32} color="#10b981" />
                </div>
                <h4>Active Tenant Profile Found</h4>
                <p>We will use your existing profile details to submit this rental application.</p>
              </div>
            )}

            <hr className={styles.divider} />

            <div className={styles.formActions}>
              <button type="submit" disabled={loading} className={`${styles.btn} ${styles.btnSuccess}`}>
                {loading ? (
                  'Submitting...'
                ) : (
                  <>
                    <CheckCircle size={20} /> Confirm & Apply <ChevronRight size={18} />
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
