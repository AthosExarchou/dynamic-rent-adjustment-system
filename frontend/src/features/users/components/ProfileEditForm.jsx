import React, {useEffect, useState} from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../auth';
import { Settings, User, Mail, CheckCircle, ArrowLeft } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import styles from './ProfileForm.module.css';

export default function ProfileEditForm() {
  const { user, refreshUser } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ username: user?.username || '', email: user?.email || '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (user) {
      setFormData({ username: user.username || '', email: user.email || '' });
    }
  }, [user]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await apiClient(`/user/edit/${user?.id}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          username: formData.username,
          email: formData.email
        }).toString()
      });
      setSuccess(true);
      if (refreshUser) await refreshUser();
    } catch {
      setError('Failed to update details. Email or Username may be taken.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let timeoutId;
    if (success) {
      timeoutId = setTimeout(() => navigate('/profile'), 2000);
    }
    return () => {
      if (timeoutId) clearTimeout(timeoutId);
    };
  }, [success, navigate]);

  return (
    <div className={styles.container}>
      <div className={styles.topActions}>
        <Link to="/profile" className={styles.backLink}>
          <ArrowLeft size={18} /> Back to Profile
        </Link>
      </div>

      <div className={`${styles.card} ${styles.cardHover}`}>
        <div className={styles.header}>
          <h2 className={styles.title}>
            <Settings className={styles.titleIcon} size={24} /> Edit Profile Details
          </h2>
        </div>
        
        <div className={styles.body}>
          {error && <div className={`${styles.alert} ${styles.alertDanger}`}>{error}</div>}
          {success && (
            <div className={`${styles.alert} ${styles.alertSuccess}`}>
              <CheckCircle size={18} /> Profile details updated successfully! Redirecting...
            </div>
          )}

          <form onSubmit={handleSubmit} className={styles.form}>
            <div className={styles.formGroup}>
              <label htmlFor="username" className={styles.label}>
                <User size={16} /> Username
              </label>
              <input 
                id="username" 
                type="text" 
                required 
                value={formData.username} 
                onChange={e => setFormData({...formData, username: e.target.value})} 
                className={styles.input} 
              />
            </div>
            
            <div className={styles.formGroup}>
              <label htmlFor="email" className={styles.label}>
                <Mail size={16} /> Email Address
              </label>
              <input 
                id="email" 
                type="email" 
                required 
                value={formData.email} 
                onChange={e => setFormData({...formData, email: e.target.value})} 
                className={styles.input} 
              />
            </div>
            
            <hr className={styles.divider} />
            
            <div className={styles.formActions}>
              <button type="submit" disabled={loading || success} className={`${styles.btn} ${styles.btnPrimary}`}>
                {loading ? 'Saving Changes...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
