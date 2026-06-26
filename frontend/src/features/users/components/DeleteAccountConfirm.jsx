import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth';
import apiClient from '../../../shared/api/client';
import styles from './ProfileForm.module.css';

export default function DeleteAccountConfirm() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ confirmationPhrase: '', password: '' });
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (formData.confirmationPhrase !== 'DELETE MY ACCOUNT') {
      setError('Confirmation phrase does not match.');
      return;
    }
    setError('');
    try {
      await apiClient('/user/delete/self', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams(formData).toString()
      });
      await logout();
      navigate('/');
    } catch {
      setError('Failed to delete account. Incorrect password.');
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h2 className={styles.titleDanger}>Delete Account</h2>
        <div className={styles.warning}>
          <strong>Warning:</strong> Deleting your account is permanent. All your data, property listings, and rental histories will be lost.
        </div>
        
        {error && <div className={styles.error}>{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className={styles.formGroup}>
            <label htmlFor="confirmationPhrase" className={styles.labelDanger}>
              Type "DELETE MY ACCOUNT" to confirm:
            </label>
            <input id="confirmationPhrase" type="text" required value={formData.confirmationPhrase} onChange={e => setFormData({...formData, confirmationPhrase: e.target.value})} className={styles.input} placeholder="DELETE MY ACCOUNT" />
          </div>
          <div className={styles.formGroupLarge}>
            <label htmlFor="password" className={styles.label}>
              Enter password:
            </label>
            <input id="password" type="password" required value={formData.password} onChange={e => setFormData({...formData, password: e.target.value})} className={styles.input} />
          </div>

          <button type="submit" className={styles.deleteBtn}>Delete Account Permanently</button>
        </form>
      </div>
    </div>
  );
}
