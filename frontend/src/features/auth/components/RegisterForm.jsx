import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import styles from './AuthForm.module.css';

export default function RegisterForm() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ username: '', email: '', password: '', confirmPassword: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      await apiClient('/saveUser', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          username: formData.username,
          email: formData.email,
          password: formData.password
        }).toString()
      });
      setSuccess(true);
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      setError('Registration failed. Username or email may be taken.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h2 className={styles.title}>Create Account</h2>
        {error && <div className={styles.errorAlert}>{error}</div>}
        {success && <div className={styles.successAlert}>Registration successful! Redirecting...</div>}
        
        <form onSubmit={handleSubmit}>
          <div className={styles.formGroup}>
            <label htmlFor="regUsername" className={styles.label}>Username</label>
            <input 
              id="regUsername"
              type="text" 
              required 
              value={formData.username} 
              onChange={e => setFormData({...formData, username: e.target.value})} 
              className={styles.input} 
              placeholder="Choose a username" 
            />
          </div>
          
          <div className={styles.formGroup}>
            <label htmlFor="regEmail" className={styles.label}>Email Address</label>
            <input 
              id="regEmail"
              type="email" 
              required 
              value={formData.email} 
              onChange={e => setFormData({...formData, email: e.target.value})} 
              className={styles.input} 
              placeholder="Enter your email" 
            />
          </div>
          
          <div className={styles.formGroup}>
            <label htmlFor="regPassword" className={styles.label}>Password</label>
            <div className={styles.inputGroup}>
              <input 
                id="regPassword"
                type={showPassword ? 'text' : 'password'} 
                required 
                value={formData.password} 
                onChange={e => setFormData({...formData, password: e.target.value})} 
                className={styles.input} 
                placeholder="Enter password" 
              />
              <button 
                type="button" 
                onClick={() => setShowPassword(!showPassword)} 
                className={styles.toggleBtn}
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
            </div>
          </div>
          
          <div className={styles.formGroup}>
            <label htmlFor="regConfirmPassword" className={styles.label}>Confirm Password</label>
            <input 
              id="regConfirmPassword"
              type={showPassword ? 'text' : 'password'} 
              required 
              value={formData.confirmPassword} 
              onChange={e => setFormData({...formData, confirmPassword: e.target.value})} 
              className={styles.input} 
              placeholder="Re-enter password" 
            />
          </div>
          
          <button type="submit" disabled={loading} className={styles.submitBtn}>
            {loading ? 'Creating...' : 'Register'}
          </button>
        </form>
        
        <div className={styles.footerText}>
          Already have an account? <Link to="/login" className={styles.footerLink}>Sign in here</Link>
        </div>
      </div>
    </div>
  );
}
