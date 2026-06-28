import React from 'react';
import { Link } from 'react-router-dom';
import { Factory, Target, Gift, CheckCircle, Lock, Bell, UserCheck, Heart, Mail } from 'lucide-react';
import styles from './AboutPage.module.css';

export default function AboutPage() {
  return (
    <div className={styles.container}>
      <h1 className={styles.pageTitle}>
        <Factory className={styles.titleIcon} size={36} /> 
        About Us
      </h1>

      <p className={styles.lead}>
        Welcome to the <strong>Dynamic Rent Adjustment System</strong>, your trusted destination for finding and renting apartments with ease.
      </p>

      <hr className={styles.divider} />

      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>
          <Target className={styles.sectionIcon} size={24} /> Our Mission
        </h3>
        <p className={styles.text}>
          Our mission is to connect apartment owners and tenants through a secure, intuitive platform.
          We simplify the rental process, making it transparent, reliable, and stress-free.
        </p>
      </section>

      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>
          <Gift className={styles.sectionIcon} size={24} /> What We Offer
        </h3>
        <ul className={styles.listGroup}>
          <li className={styles.listItem}>
            <CheckCircle className={`${styles.itemIcon} ${styles.iconSuccess}`} size={20} />
            A wide selection of verified apartments for rent
          </li>
          <li className={styles.listItem}>
            <Lock className={`${styles.itemIcon} ${styles.iconPrimary}`} size={20} />
            Secure account management for tenants and owners
          </li>
          <li className={styles.listItem}>
            <Bell className={`${styles.itemIcon} ${styles.iconWarning}`} size={20} />
            Streamlined communication and notifications
          </li>
          <li className={styles.listItem}>
            <UserCheck className={`${styles.itemIcon} ${styles.iconInfo}`} size={20} />
            Admin-approved listings to ensure quality and trust
          </li>
        </ul>
      </section>

      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>
          <Heart className={styles.sectionIcon} size={24} /> Our Values
        </h3>
        <p className={styles.text}>
          We believe in <strong>trust</strong>, <strong>transparency</strong>, and <strong>accessibility</strong>.
          Our platform is built with these core values in mind so you can confidently find your next home or list your property with ease.
        </p>
      </section>

      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>
          <Mail className={styles.sectionIcon} size={24} /> Contact Us
        </h3>
        <p className={styles.text}>
          Have questions or suggestions? We’d love to hear from you. Reach out via our
            <Link to="/contact" className={styles.link}> Contact Form</Link> or email us at
            <a href="mailto:realestate2025project@gmail.com" className={styles.link}> realestate2025project@gmail.com</a>.
        </p>
      </section>

      <footer className={styles.footer}>
        <p>&copy; DRAS · Helping people find the perfect home since 2025.</p>
      </footer>
    </div>
  );
}
