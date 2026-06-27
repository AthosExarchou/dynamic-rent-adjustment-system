import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Building2, ShieldCheck, TrendingUp, ChevronDown, ChevronUp } from 'lucide-react';
import { useAuth } from '../../auth';
import apiClient from '../../../shared/api/client';
import styles from './LandingPage.module.css';

export default function LandingPage() {
  const { isAuthenticated } = useAuth();
  const [openFaq, setOpenFaq] = useState(null);

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

      <section className={styles.faqSection}>
        <h2 className={styles.faqTitle}>Frequently Asked Questions</h2>
        <div className={styles.faqList}>
          
          <div className={styles.faqItem}>
            <button 
              className={styles.faqQuestion} 
              onClick={() => setOpenFaq(openFaq === 0 ? null : 0)}
            >
              How can I search for property listings on DRAS?
              {openFaq === 0 ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
            </button>
            {openFaq === 0 && (
              <div className={styles.faqAnswer}>
                <p>DRAS makes it easy and quick to find your ideal place in the world. Here's how you can search for property listings:</p>
                <ol>
                  <li>Visit the DRAS website.</li>
                  <li>Go to the Listings page.</li>
                  <li>Refine your search further by using the available filters (price, area, property type, etc).</li>
                  <li>Click "Search" to display listings that match your criteria.</li>
                  <li>Browse the listings you're interested in. If a property catches your attention, click Apply to submit your tenant application.</li>
                </ol>
              </div>
            )}
          </div>

          <div className={styles.faqItem}>
            <button 
              className={styles.faqQuestion} 
              onClick={() => setOpenFaq(openFaq === 1 ? null : 1)}
            >
              What is the process for listing a property on DRAS?
              {openFaq === 1 ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
            </button>
            {openFaq === 1 && (
              <div className={styles.faqAnswer}>
                <p>If you're a property owner and want to publish a property for rent on DRAS, the process is very straightforward:</p>
                <ol>
                  <li>Register or sign in to your account.</li>
                  <li>Navigate to the Listings page and click "Submit Property."</li>
                  <li>Complete all the required fields with your property's details.</li>
                  <li>If this is your first time, you will automatically be registered with an Owner Profile.</li>
                  <li>Click "Submit" to post your listing. It will be reviewed by administrators and then made available to prospective tenants.</li>
                </ol>
              </div>
            )}
          </div>

          <div className={styles.faqItem}>
            <button 
              className={styles.faqQuestion} 
              onClick={() => setOpenFaq(openFaq === 2 ? null : 2)}
            >
              What additional services or tools can help me?
              {openFaq === 2 ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
            </button>
            {openFaq === 2 && (
              <div className={styles.faqAnswer}>
                <p>By creating an account on DRAS, you can use your personal profile to:</p>
                <ul>
                  <li>Become a tenant or an owner simply by interacting with the platform.</li>
                  <li>Manage all your pending and approved rental applications.</li>
                  <li>Track applicants for your properties in a single Dashboard.</li>
                </ul>
                <p>This gives you complete control over your rental workflows, with everything organized in one place just for you.</p>
              </div>
            )}
          </div>

        </div>
      </section>
    </div>
  );
}
