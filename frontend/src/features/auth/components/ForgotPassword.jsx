import React, { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { CheckCircle2, ArrowLeft } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import { useToast } from '../../../shared/components/Toast';
import styles from './AuthForm.module.css';

export default function ForgotPassword() {
  const [searchParams] = useSearchParams();
  const initialEmail = searchParams.get('email') || '';
  const [email, setEmail] = useState(initialEmail);
  const [submitted, setSubmitted] = useState(false);
  const [submittedEmail, setSubmittedEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const toast = useToast();

  const handleSubmit = async (e) => {
    e.preventDefault();
    const cleanEmail = email.trim();
    if (!cleanEmail) return;

    setLoading(true);
    setError('');

    try {
      const response = await apiClient('/auth/forgot-password', {
        method: 'POST',
        body: {
          email: cleanEmail
        }
      });

      setSubmittedEmail(response?.email || cleanEmail);
      setSubmitted(true);
      toast.success('Password reset request submitted successfully.');
    } catch (err) {
      const errorMsg = err.message || 'Unable to submit reset request. Please try again or contact support.';
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={`${styles.card} ${styles.loginCard}`}>
        <div className={styles.header}>
          <h2 className={styles.titleText}>DRAS</h2>
          <h4 className={styles.subtitle}>Reset Password</h4>
          <p className={styles.subtext}>
            Enter your email to request account access recovery.
          </p>
        </div>

        {error && (
          <div className={`${styles.alert} ${styles.alertDanger}`}>
            {error}
          </div>
        )}

        {submitted ? (
          <div>
            <div className={`${styles.alert} ${styles.alertSuccess}`}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 600, marginBottom: '0.35rem' }}>
                <CheckCircle2 size={18} /> Request Received
              </div>
              <div>
                We have received your recovery request for <strong>{submittedEmail}</strong>. Our administrative support team will review your account and contact you at this email address to assist with restoring access.
              </div>
            </div>

            <Link
              to="/login"
              className={styles.submitBtn}
              style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', textDecoration: 'none' }}
            >
              <ArrowLeft size={16} /> Back to Sign In
            </Link>

            <div style={{ marginTop: '1rem', textAlign: 'center' }}>
              <button
                type="button"
                onClick={() => setSubmitted(false)}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--text-secondary)',
                  fontSize: '0.85rem',
                  cursor: 'pointer',
                  textDecoration: 'underline'
                }}
              >
                Send for a different email
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className={styles.form}>
            <div className={styles.formGroup}>
              <label htmlFor="resetEmail" className={styles.label}>
                Registered Email
              </label>
              <input
                id="resetEmail"
                type="email"
                required
                maxLength="50"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className={styles.input}
                placeholder="Enter your registered email"
                autoFocus
              />
            </div>

            <button type="submit" disabled={loading} className={styles.submitBtn}>
              {loading ? 'Submitting Request...' : 'Send Recovery Request'}
            </button>
          </form>
        )}

        <div className={styles.footerText}>
          Remember your password?{' '}
          <Link to="/login" className={styles.footerLink}>
            Sign in here
          </Link>
        </div>
      </div>
    </div>
  );
}
