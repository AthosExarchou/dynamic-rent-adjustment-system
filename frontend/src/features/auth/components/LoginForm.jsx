import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import styles from './AuthForm.module.css';

export default function LoginForm() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await login(username, password);
      navigate('/');
    } catch (err) {
      setError('Invalid username or password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={`${styles.card} ${styles.loginCard}`}>
        <div className={styles.header}>
          <h2 className={styles.gradientText}>DRAS</h2>
          <h4 className={styles.subtitle}>Sign in to continue</h4>
        </div>

        {error && (
          <div className={`${styles.alert} ${styles.alertDanger}`}>
            {error}
          </div>
        )}
        
        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.formGroup}>
            <label htmlFor="loginUsername" className={styles.label}>Username</label>
            <input 
              id="loginUsername"
              type="text" 
              required 
              value={username} 
              onChange={e => setUsername(e.target.value)} 
              className={styles.input} 
              placeholder="Enter your username" 
            />
          </div>
          
          <div className={styles.formGroup}>
            <label htmlFor="loginPassword" className={styles.label}>Password</label>
            <div className={styles.inputGroup}>
              <input 
                id="loginPassword"
                type={showPassword ? 'text' : 'password'} 
                required 
                value={password} 
                onChange={e => setPassword(e.target.value)} 
                className={styles.input} 
                placeholder="Enter your password" 
              />
              <button 
                type="button" 
                onClick={() => setShowPassword(!showPassword)} 
                className={styles.toggleBtn}
                aria-label={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          <div className={styles.formOptions} style={{ justifyContent: 'flex-end' }}>
            <Link to="/contact" className={styles.forgotLink}>Forgot Password?</Link>
          </div>
          
          <button type="submit" disabled={loading} className={styles.submitBtn}>
            {loading ? 'Signing In...' : 'Sign In'}
          </button>
        </form>
        
        <div className={styles.footerText}>
          Don't have an account? <Link to="/register" className={styles.footerLink}>Register here</Link>
        </div>
      </div>
    </div>
  );
}
