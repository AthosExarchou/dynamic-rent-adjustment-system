import React from 'react';
import styles from './StaticPage.module.css';

export default function PrivacyPolicy() {
  return (
    <div className={styles.container}>
      <h1 className={styles.title}>Privacy Policy</h1>
      <p className={styles.date}>Last updated: June 2026</p>
      <p className={styles.content}>
        At DRAS Platform, we take your privacy seriously. This privacy policy describes how we collect, store, and process personal identification info, listing data, and lease histories.
      </p>
    </div>
  );
}
