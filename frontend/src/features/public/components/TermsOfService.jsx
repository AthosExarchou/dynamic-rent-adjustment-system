import React from 'react';
import styles from './StaticPage.module.css';

export default function TermsOfService() {
  return (
    <div className={styles.container}>
      <h1 className={styles.title}>Terms of Service</h1>
      <p className={styles.date}>Last updated: June 2026</p>
      <p className={styles.content}>
        By registering or using the DRAS Platform, you agree to comply with our Terms of Service.
      </p>
    </div>
  );
}
