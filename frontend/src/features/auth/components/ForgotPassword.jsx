import React, { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { CheckCircle2, Lock, Eye, EyeOff, RotateCcw } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import { useToast } from '../../../shared/components/Toast';
import styles from './AuthForm.module.css';

export default function ForgotPassword() {
  const [searchParams] = useSearchParams();
  const initialEmail = searchParams.get('email') || '';

  // Step 1: 'request-code', Step 2: 'reset-password', Step 3: 'success'
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState(initialEmail);
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [error, setError] = useState('');
  const toast = useToast();

  const handleRequestCode = async (e) => {
    e.preventDefault();
    const cleanEmail = email.trim();
    if (!cleanEmail) return;

    setLoading(true);
    setError('');

    try {
      await apiClient('/auth/forgot-password', {
        method: 'POST',
        body: { email: cleanEmail }
      });

      setStep(2);
      toast.success('Confirmation code sent to your email.');
    } catch (err) {
      const errorMsg = err.message || 'Unable to send confirmation code. Please check your email and try again.';
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleResendCode = async () => {
    const cleanEmail = email.trim();
    if (!cleanEmail || resending) return;

    setResending(true);
    setError('');

    try {
      await apiClient('/auth/forgot-password', {
        method: 'POST',
        body: { email: cleanEmail }
      });
      toast.success('A fresh confirmation code was sent to your email.');
    } catch (err) {
      setError(err.message || 'Failed to resend confirmation code.');
    } finally {
      setResending(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    setError('');

    const cleanCode = code.trim();
    if (!cleanCode) {
      setError('Please enter the 6-digit confirmation code.');
      return;
    }

    if (cleanCode.length !== 6) {
      setError('Confirmation code must be exactly 6 digits.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('New password and confirmation do not match.');
      return;
    }

    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;
    if (!passwordRegex.test(newPassword)) {
      setError('Password must contain 8+ characters, including uppercase, lowercase, a number, and a special character.');
      return;
    }

    setLoading(true);

    try {
      await apiClient('/auth/reset-password', {
        method: 'POST',
        body: {
          email: email.trim(),
          code: cleanCode,
          newPassword,
          confirmPassword
        }
      });

      setStep(3);
      toast.success('Password reset successfully!');
    } catch (err) {
      const errorMsg = err.message || 'Failed to reset password. Please check your code and try again.';
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
          <h4 className={styles.subtitle}>
            {step === 3 ? 'Password Updated' : 'Reset Password'}
          </h4>
          <p className={styles.subtext}>
            {step === 1 && 'Enter your registered email to receive a 6-digit confirmation code.'}
            {step === 2 && `Enter the 6-digit code sent to ${email} and choose a new password.`}
            {step === 3 && 'Your account password has been updated successfully.'}
          </p>
        </div>

        {error && (
          <div className={`${styles.alert} ${styles.alertDanger}`}>
            {error}
          </div>
        )}

        {/* Step 1: Request confirmation code */}
        {step === 1 && (
          <form onSubmit={handleRequestCode} className={styles.form}>
            <div className={styles.formGroup}>
              <label htmlFor="resetEmail" className={styles.label}>
                Registered Email
              </label>
              <input
                id="resetEmail"
                type="email"
                required
                maxLength={50}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className={styles.input}
                placeholder="Enter your registered email"
                autoFocus
              />
            </div>

            <button type="submit" disabled={loading} className={styles.submitBtn}>
              {loading ? 'Sending Code...' : 'Send Confirmation Code'}
            </button>

            <div className={styles.footerText}>
              Remember your password?{' '}
              <Link to="/login" className={styles.footerLink}>
                Sign in here
              </Link>
            </div>
          </form>
        )}

        {/* Step 2: Enter code & new password */}
        {step === 2 && (
          <form onSubmit={handleResetPassword} className={styles.form}>
            <div className={styles.formGroup}>
              <label htmlFor="resetCode" className={styles.label}>
                6-Digit Confirmation Code
              </label>
              <input
                id="resetCode"
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                maxLength={6}
                required
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                className={styles.input}
                placeholder="123456"
                style={{
                  fontFamily: 'monospace',
                  letterSpacing: '0.35em',
                  textAlign: 'center',
                  fontSize: '1.25rem',
                  fontWeight: '600'
                }}
                autoFocus
              />
            </div>

            <div className={styles.formGroup}>
              <label htmlFor="newPassword" className={styles.label}>
                New Password
              </label>
              <div className={styles.inputGroup}>
                <input
                  id="newPassword"
                  type={showNewPassword ? 'text' : 'password'}
                  required
                  maxLength={100}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className={styles.input}
                  placeholder="Enter new password"
                />
                <button
                  type="button"
                  className={styles.toggleBtn}
                  onClick={() => setShowNewPassword(!showNewPassword)}
                  aria-label={showNewPassword ? 'Hide password' : 'Show password'}
                >
                  {showNewPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              <p className={styles.passwordHint}>
                Must contain 8+ characters, with uppercase, lowercase, a number, and a special character.
              </p>
            </div>

            <div className={styles.formGroup}>
              <label htmlFor="confirmPassword" className={styles.label}>
                Confirm New Password
              </label>
              <div className={styles.inputGroup}>
                <input
                  id="confirmPassword"
                  type={showConfirmPassword ? 'text' : 'password'}
                  required
                  maxLength={100}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className={styles.input}
                  placeholder="Re-enter new password"
                />
                <button
                  type="button"
                  className={styles.toggleBtn}
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                >
                  {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <button type="submit" disabled={loading} className={styles.submitBtn}>
              {loading ? 'Resetting Password...' : 'Reset Password'}
            </button>

            <div style={{ marginTop: '1.25rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.85rem' }}>
              <button
                type="button"
                onClick={() => {
                  setStep(1);
                  setCode('');
                  setError('');
                }}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--text-secondary)',
                  cursor: 'pointer',
                  padding: 0,
                  textDecoration: 'underline'
                }}
              >
                Change email
              </button>

              <button
                type="button"
                onClick={handleResendCode}
                disabled={resending}
                style={{
                  background: 'none',
                  border: 'none',
                  color: '#0d6efd',
                  cursor: 'pointer',
                  padding: 0,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.25rem'
                }}
              >
                <RotateCcw size={14} /> {resending ? 'Resending...' : 'Resend Code'}
              </button>
            </div>
          </form>
        )}

        {/* Step 3: Success Confirmation */}
        {step === 3 && (
          <div>
            <div className={`${styles.alert} ${styles.alertSuccess}`}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 600, marginBottom: '0.35rem' }}>
                <CheckCircle2 size={18} /> Password Reset Successful
              </div>
              <div>
                Your password has been successfully updated. You can now log in using your new credentials.
              </div>
            </div>

            <Link
              to="/login"
              className={styles.submitBtn}
              style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', textDecoration: 'none' }}
            >
              <Lock size={16} /> Sign In With New Password
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}
