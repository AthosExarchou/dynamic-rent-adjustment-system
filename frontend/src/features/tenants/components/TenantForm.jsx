import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../../auth';
import { UserPlus, UserCheck, Phone, CheckCircle, ChevronRight, ArrowLeft, Info } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import { TENANT_CONSTRAINTS } from '../models/tenant';
import styles from './TenantForm.module.css';

export default function TenantForm() {
  const { listingId } = useParams();
  const { roles, refreshUser } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const fromListingDetail = location.state?.fromListingDetail;
  const [formData, setFormData] = useState({ firstName: '', lastName: '', phoneNumber: '' });
  const [error, setError] = useState('');
  const [errorDetails, setErrorDetails] = useState([]);
  const [loading, setLoading] = useState(false);
  const [alreadyApplied, setAlreadyApplied] = useState(false);
  const [rentingListingId, setRentingListingId] = useState(null);

  const handleBackToDetails = (e) => {
    e.preventDefault();
    if (fromListingDetail) {
      navigate(-1);
    } else {
      navigate(`/listings/${listingId}`, { replace: true });
    }
  };

  const isTenant = roles.includes('TENANT');

  useEffect(() => {
    let active = true;
    if (isTenant && listingId) {
      apiClient('/tenant/applications')
        .then(data => {
          if (!active) return;
          if (data?.rentingListingId != null) {
            setRentingListingId(data.rentingListingId);
          }
          if (data?.appliedListingIds?.includes(Number(listingId))) {
            setAlreadyApplied(true);
            setError('You have already applied for this listing.');
          }
        })
        .catch(() => {});
    }
    return () => { active = false; };
  }, [listingId, isTenant]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (alreadyApplied || rentingListingId != null) return;
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

    setError('');
    setErrorDetails([]);

    try {
      await apiClient(`/tenant/rent/${listingId}`, requestOptions);
      if (!isTenant) await refreshUser();
      alert('Application submitted successfully!');
      navigate('/listings');
    } catch (err) {
      const msg = err.message || 'Failed to submit application. Profile may already exist or there was a server error.';
      setError(msg);
      if (Array.isArray(err.details) && err.details.length > 0) {
        setErrorDetails(err.details);
      }
      if (msg.toLowerCase().includes('already applied')) {
        setAlreadyApplied(true);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.topActions}>
        <a href={`/listings/${listingId}`} onClick={handleBackToDetails} className={styles.backLink}>
          <ArrowLeft size={18} /> Back to Property Details
        </a>
      </div>

      <div className={`${styles.card} ${styles.cardHover}`}>
        <div className={styles.header}>
          <h3 className={styles.title}>
            <UserPlus className={styles.titleIcon} size={28} />
            Tenant Rental Application
          </h3>
        </div>
        
        <div className={styles.body}>
          {rentingListingId != null ? (
            <div className={`${styles.alert} ${styles.alertInfo}`}>
              <Info size={22} style={{ flexShrink: 0, marginTop: '2px' }} />
              <div>
                <strong style={{ display: 'block', marginBottom: '0.25rem' }}>Active Tenancy In Progress</strong>
                <span>
                  {rentingListingId === Number(listingId)
                    ? 'You are currently renting this property.'
                    : `You are currently renting another property (Property #${rentingListingId}). Platform rules permit renting only one property at a time.`}
                </span>
                <div style={{ marginTop: '0.75rem' }}>
                  <Link to="/listings" className={styles.backLinkBtn}>
                    Return to Listings
                  </Link>
                </div>
              </div>
            </div>
          ) : alreadyApplied ? (
            <div className={`${styles.alert} ${styles.alertInfo}`}>
              <Info size={22} style={{ flexShrink: 0, marginTop: '2px' }} />
              <div>
                <strong style={{ display: 'block', marginBottom: '0.25rem' }}>Application Already Submitted</strong>
                <span>You have already submitted a rental application for this listing. The owner will review it shortly.</span>
                <div style={{ marginTop: '0.75rem' }}>
                  <Link to="/listings" className={styles.backLinkBtn}>
                    Return to Listings
                  </Link>
                </div>
              </div>
            </div>
          ) : error ? (
            <div className={`${styles.alert} ${styles.alertDanger}`}>
              <div>{error}</div>
              {errorDetails && errorDetails.length > 1 && (
                <ul style={{ marginTop: '0.5rem', marginBottom: 0, paddingLeft: '1.25rem' }}>
                  {errorDetails.map((detail, idx) => (
                    <li key={idx}>{detail}</li>
                  ))}
                </ul>
              )}
            </div>
          ) : null}
          
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
                      maxLength={TENANT_CONSTRAINTS.FIRST_NAME_MAX}
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
                      maxLength={TENANT_CONSTRAINTS.LAST_NAME_MAX}
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
                      minLength={TENANT_CONSTRAINTS.PHONE_MIN}
                      maxLength={TENANT_CONSTRAINTS.PHONE_MAX}
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
              <button 
                type="submit" 
                disabled={loading || alreadyApplied || rentingListingId != null} 
                className={`${styles.btn} ${(alreadyApplied || rentingListingId != null) ? styles.btnDisabled : styles.btnSuccess}`}
              >
                {rentingListingId != null ? (
                  <>
                    <CheckCircle size={20} /> Currently Renting
                  </>
                ) : alreadyApplied ? (
                  <>
                    <CheckCircle size={20} /> Already Applied
                  </>
                ) : loading ? (
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
