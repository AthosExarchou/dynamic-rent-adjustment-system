import React from 'react';
import { Link } from 'react-router-dom';
import { Lock } from 'lucide-react';
import styles from './PrivacyPolicy.module.css';

export default function PrivacyPolicy() {
  return (
    <div className={styles.container}>
      <h1 className={styles.pageTitle}>
        <Lock className={styles.titleIcon} size={36} /> Privacy Policy
      </h1>
      <p className={styles.lead}>
        Your privacy is important to us. This Privacy Policy explains how we handle your personal information when you use the Dynamic Rent Adjustment System.
      </p>
      
      <hr className={styles.divider} />

      <div className={styles.content}>
        <section className={styles.section}>
          <h3 className={styles.sectionTitle}>1. Information We Collect</h3>
          <p>We may collect personal details such as your name, email address, and contact information when you register or use our services.</p>
        </section>

        <section className={styles.section}>
          <h3 className={styles.sectionTitle}>2. How We Use Your Information</h3>
          <ul className={styles.list}>
            <li>To provide access to our apartment rental services.</li>
            <li>To improve our platform and user experience.</li>
            <li>To communicate updates, offers, and support.</li>
          </ul>
        </section>

        <section className={styles.section}>
          <h3 className={styles.sectionTitle}>3. Data Security</h3>
          <p>We implement strict security measures to protect your personal data. However, please be aware that no method of data transmission over the Internet is 100% secure.</p>
        </section>

        <section className={styles.section}>
          <h3 className={styles.sectionTitle}>4. Sharing of Information</h3>
          <p>We do not sell or trade your information. We may share limited data only with trusted partners who help us operate our platform.</p>
        </section>

        <section className={styles.section}>
          <h3 className={styles.sectionTitle}>5. Your Rights</h3>
          <p>You may request access, correction, or deletion of your data at any time by contacting our support team.</p>
        </section>

        <section className={styles.section}>
          <h3 className={styles.sectionTitle}>6. Updates to This Policy</h3>
          <p>We may update this policy from time to time. Any changes will be posted here with the updated date.</p>
        </section>

        <section className={styles.section}>
          <h3 className={styles.sectionTitle}>7. Contact Us</h3>
          <p>
            Have questions about our Privacy Policy? Reach out via our <Link to="/contact" className={styles.link}>Contact Form</Link> or email us at <a href="mailto:realestate2025project@gmail.com" className={styles.link}>realestate2025project@gmail.com</a>.
          </p>
        </section>
      </div>

      <footer className={styles.footer}>
        <p>DRAS · Helping people find the perfect place to live since 2025.</p>
      </footer>
    </div>
  );
}
