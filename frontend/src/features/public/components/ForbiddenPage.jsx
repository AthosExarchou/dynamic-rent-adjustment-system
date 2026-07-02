import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldAlert, Home } from 'lucide-react';
import styles from './ErrorPage.module.css';

export default function ForbiddenPage() {
  return (
    <div className={styles.container}>
      <div className={`${styles.card} ${styles.cardHover}`}>
        <div className={styles.iconWrapper}>
          <ShieldAlert className={`${styles.icon} ${styles.iconWarning}`} size={64} />
        </div>
        <h1 className={styles.titleText}>403 - Access Denied</h1>
        <p className={styles.description}>
          You do not have permission to view this page. If you believe this is a mistake, 
          please contact an administrator or sign in with an authorized account.
        </p>
        <Link to="/" className={`${styles.btn} ${styles.btnPrimary}`}>
          <Home size={18} /> Back to Home
        </Link>
      </div>
    </div>
  );
}
