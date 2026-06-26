import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Building2, ShieldCheck, TrendingUp } from 'lucide-react';
import { useAuth } from '../../auth';
import apiClient from '../../../shared/api/client';
import styles from './LandingPage.module.css';

export default function LandingPage() {
  const { isAuthenticated } = useAuth();
  useEffect(() => {
    // Placeholder for future landing page logic
  }, []);

  return (
    <div className={styles.container}>
      <section className={styles.hero}>
        <h1 className={styles.title}>
          Find Your Dream Apartment
        </h1>
        <p className={styles.subtitle}>
          Browse verified listings, coordinate with verified owners, and calculate rent parameters seamlessly.
        </p>
        <Link to="/listings" className={styles.ctaButton}>
          Browse Apartments
        </Link>
      </section>

      <section className={styles.featuresSection}>
        <h2 className={styles.featuresTitle}>Why Choose Us?</h2>
        <div className={styles.featuresGrid}>
          <div className={styles.card}>
            <h3 className={styles.cardTitle}>
              <Building2 size={24} /> Verified Listings
            </h3>
            <p className={styles.cardDesc}>Every apartment is carefully reviewed and approved by our team for quality and accuracy.</p>
          </div>
          <div className={styles.card}>
            <h3 className={styles.cardTitle}>
              <ShieldCheck size={24} /> Trusted Platform
            </h3>
            <p className={styles.cardDesc}>Secure user profiles and transparent lease applications matching Spring Security standards.</p>
          </div>
          <div className={styles.card}>
            <h3 className={styles.cardTitle}>
              <TrendingUp size={24} /> Dynamic Adjustments
            </h3>
            <p className={styles.cardDesc}>Our system monitors market trends to provide fair, algorithmic rent adjustments automatically.</p>
          </div>
        </div>
      </section>
    </div>
  );
}
