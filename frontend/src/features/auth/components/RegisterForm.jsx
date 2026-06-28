import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import styles from './AuthForm.module.css';

export default function RegisterForm() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ username: '', email: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      await register(formData.username, formData.email, formData.password);
      navigate('/login');
    } catch (err) {
      setError('Registration failed. Please check your details and try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.id]: e.target.value });
  };

  return (
    <div className={styles.container}>
      <div className={`${styles.card} ${styles.registerCard}`}>
        <div className={styles.header}>
          <h2 className={styles.gradientText}>DRAS</h2>
          <h4 className={styles.subtitle}>Create an Account</h4>
          <p className={styles.subtext}>Join the DRAS platform today!</p>
        </div>

        {error && (
          <div className={`${styles.alert} ${styles.alertDanger}`}>
            {error}
          </div>
        )}
        
        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.formGroup}>
            <label htmlFor="username" className={styles.label}>User Name</label>
            <input 
              id="username"
              type="text" 
              required 
              maxLength="20"
              value={formData.username} 
              onChange={handleChange} 
              className={styles.input} 
              placeholder="Choose a username" 
            />
          </div>

          <div className={styles.formGroup}>
            <label htmlFor="email" className={styles.label}>Email Address</label>
            <input 
              id="email"
              type="email" 
              required 
              maxLength="50"
              value={formData.email} 
              onChange={handleChange} 
              className={styles.input} 
              placeholder="Enter your email" 
            />
          </div>
          
          <div className={styles.formGroup}>
            <label htmlFor="password" className={styles.label}>Password</label>
            <div className={styles.inputGroup}>
              <input 
                id="password"
                type={showPassword ? 'text' : 'password'} 
                required 
                value={formData.password} 
                onChange={handleChange} 
                className={styles.input} 
                placeholder="Enter a secure password"
                pattern="^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$"
                title="Must contain at least 8 characters, including an uppercase letter, a lowercase letter, a number, and a special character (@$!%*?&)."
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
            <div className={styles.passwordHint}>
              Must contain 8+ characters, including uppercase, lowercase, a number, and a special character.
            </div>
          </div>
          
          <button type="submit" disabled={loading} className={styles.submitBtn}>
            {loading ? 'Signing Up...' : 'Sign Up'}
          </button>
        </form>
        
        <div className={styles.footerText}>
          Already have an account? <Link to="/login" className={styles.footerLink}>Sign in</Link>
        </div>
      </div>
    </div>
  );
}
