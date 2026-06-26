import React from 'react';
import styles from './StaticPage.module.css';

export default function AboutPage() {
  return (
    <div className={styles.container}>
      <h1 className={styles.title}>About Us</h1>
      <p className={styles.content}>
        Welcome to the <strong>Dynamic Rent Adjustment System (DRAS)</strong>. We are dedicated to providing a fair, transparent, and modern platform for managing residential rentals.
      </p>
      <p className={styles.content}>
        Our platform connects property owners directly with prospective tenants, offering automated listing approvals, lease application workflows, and automated rent adjustment recommendations based on localized market data and inflation parameters.
      </p>
    </div>
  );
}
