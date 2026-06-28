import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../auth';
import { Trash2, AlertTriangle, ArrowLeft } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import styles from './ProfileForm.module.css';

export default function DeleteAccountConfirm() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ confirmationPhrase: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (formData.confirmationPhrase !== 'DELETE MY ACCOUNT') {
      setError('Confirmation phrase does not match.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      // BUG-F05 FIX: Explicitly pick only the fields the backend expects.
      // Sending formData wholesale is brittle — any future fields added to the
      // form state would be silently included in the payload.
      const payload = {
        confirmationPhrase: formData.confirmationPhrase,
        password: formData.password,
      };
      await apiClient('/user/delete/self', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams(payload).toString()
      });
      await logout();
      navigate('/');
    } catch {
      setError('Failed to delete account. Incorrect password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.topActions}>
        <Link to="/profile" className={styles.backLink}>
          <ArrowLeft size={18} /> Back to Profile
        </Link>
      </div>

      <div className={`${styles.card} ${styles.cardHover} ${styles.cardDanger}`}>
        <div className={styles.headerDanger}>
          <h2 className={styles.title}>
            <AlertTriangle className={styles.titleIconLight} size={24} /> Delete Account
          </h2>
        </div>
        
        <div className={styles.body}>
          <div className={`${styles.alert} ${styles.alertWarning}`}>
            <AlertTriangle size={24} />
            <div>
              <strong>Warning:</strong> Deleting your account is permanent. All your data, property listings, and rental histories will be irrecoverably lost.
            </div>
          </div>
          
          {error && <div className={`${styles.alert} ${styles.alertDanger}`}>{error}</div>}

          <form onSubmit={handleSubmit} className={styles.form}>
            <div className={styles.formGroup}>
              <label htmlFor="confirmationPhrase" className={`${styles.label} ${styles.textDanger}`}>
                Type "DELETE MY ACCOUNT" to confirm:
              </label>
              <input 
                id="confirmationPhrase" 
                type="text" 
                required 
                value={formData.confirmationPhrase} 
                onChange={e => setFormData({...formData, confirmationPhrase: e.target.value})} 
                className={`${styles.input} ${styles.inputDanger}`} 
                placeholder="DELETE MY ACCOUNT" 
              />
            </div>
            
            <div className={styles.formGroup}>
              <label htmlFor="password" className={styles.label}>
                Enter your password:
              </label>
              <input 
                id="password" 
                type="password" 
                required 
                value={formData.password} 
                onChange={e => setFormData({...formData, password: e.target.value})} 
                className={styles.input} 
              />
            </div>
            
            <hr className={styles.divider} />
            
            <div className={styles.formActions}>
              <button type="submit" disabled={loading} className={`${styles.btn} ${styles.btnDanger}`}>
                <Trash2 size={18} /> {loading ? 'Deleting...' : 'Delete Account Permanently'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
