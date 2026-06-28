import React from 'react';
import { Link } from 'react-router-dom';
import { SearchX, Home } from 'lucide-react';
import styles from './ErrorPage.module.css';

export default function NotFoundPage() {
  return (
    <div className={styles.container}>
      <div className={`${styles.card} ${styles.cardHover}`}>
        <div className={styles.iconWrapper}>
          <SearchX className={styles.icon} size={64} />
        </div>
        <h1 className={styles.gradientText}>404 - Page Not Found</h1>
        <p className={styles.description}>
          The resource you are looking for no longer exists or has been moved. 
          Please check the URL or try returning to the home page.
        </p>
        <Link to="/" className={`${styles.btn} ${styles.btnPrimary}`}>
          <Home size={18} /> Back to Home
        </Link>
      </div>
    </div>
  );
}
