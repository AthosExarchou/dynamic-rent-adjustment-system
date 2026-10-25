import React from 'react';
import { Link } from 'react-router-dom';
import {
  Building2, 
  Target, 
  BarChart3, 
  ShieldCheck, 
  TrendingUp, 
  Users, 
  Bell, 
  Lock, 
  Scale, 
  HeartHandshake, 
  ArrowRight, 
  Mail,
  CheckCircle2
} from 'lucide-react';
import styles from './AboutPage.module.css';

export default function AboutPage() {
  return (
    <div className={styles.pageContainer}>
      {/* Header Section */}
      <section className={styles.heroSection}>
        <div className={styles.heroBadge}>
          <Building2 size={16} className={styles.heroBadgeIcon} />
          <span>About the Platform</span>
        </div>
        <h1 className={styles.heroTitle}>
          Dynamic Rent <span className={styles.gradientText}>Adjustment System</span>
        </h1>
        <p className={styles.heroLead}>
          DRAS is an apartment rental and price suggestion platform designed to assist property owners and tenants
          with transparent rental pricing, admin-reviewed listings, and direct rental applications.
        </p>

        {/* Highlights Bar */}
        <div className={styles.metricsGrid}>
          <div className={styles.metricCard}>
            <div className={styles.metricValue}>Admin-Reviewed</div>
            <div className={styles.metricLabel}>Listings verified before publishing</div>
          </div>
          <div className={styles.metricCard}>
            <div className={styles.metricValue}>Price Guidance</div>
            <div className={styles.metricLabel}>Scraped benchmarks & ARIMA forecasts</div>
          </div>
          <div className={styles.metricCard}>
            <div className={styles.metricValue}>Direct Inquiries</div>
            <div className={styles.metricLabel}>Tenants apply directly to owners</div>
          </div>
          <div className={styles.metricCard}>
            <div className={styles.metricValue}>Role Portals</div>
            <div className={styles.metricLabel}>Dedicated tools for Owners & Tenants</div>
          </div>
        </div>
      </section>

      {/* Mission & Purpose */}
      <section className={styles.missionVisionGrid}>
        <div className={`${styles.card} ${styles.missionCard}`}>
          <div className={`${styles.iconWrapper} ${styles.iconPrimary}`}>
            <Target size={28} />
          </div>
          <h2 className={styles.cardTitle}>Our Purpose</h2>
          <p className={styles.cardText}>
            To provide a transparent, accessible platform where property owners can list apartments and prospective
            tenants can browse listings and apply with confidence, supported by clear property details.
          </p>
        </div>

        <div className={`${styles.card} ${styles.visionCard}`}>
          <div className={`${styles.iconWrapper} ${styles.iconPurple}`}>
            <BarChart3 size={28} />
          </div>
          <h2 className={styles.cardTitle}>Data-Informed Approach</h2>
          <p className={styles.cardText}>
            Rather than relying solely on guesswork, DRAS incorporates regional market scraping and statistical
            time-series models (Auto-ARIMA) to offer objective price-per-square-meter guidance.
          </p>
        </div>
      </section>

      {/* Core Platform Features */}
      <section className={styles.featuresSection}>
        <div className={styles.sectionHeader}>
          <span className={styles.sectionPill}>Platform Features</span>
          <h2 className={styles.sectionTitle}>Key Capabilities</h2>
          <p className={styles.sectionSubtitle}>
            Features designed to support both property owners and prospective tenants throughout the rental process.
          </p>
        </div>

        <div className={styles.featuresGrid}>
          <div className={styles.featureCard}>
            <div className={`${styles.featureIconBox} ${styles.iconSuccess}`}>
              <ShieldCheck size={24} />
            </div>
            <h3 className={styles.featureTitle}>Reviewed Listings</h3>
            <p className={styles.featureText}>
              Every submitted apartment is reviewed by platform administrators before appearing in public searches to help maintain accurate descriptions.
            </p>
            <ul className={styles.featureList}>
              <li><CheckCircle2 size={16} className={styles.bulletIcon} /> Admin review and approval workflow</li>
              <li><CheckCircle2 size={16} className={styles.bulletIcon} /> Structured amenities and photo uploads</li>
            </ul>
          </div>

          <div className={styles.featureCard}>
            <div className={`${styles.featureIconBox} ${styles.iconBlue}`}>
              <TrendingUp size={24} />
            </div>
            <h3 className={styles.featureTitle}>Price Suggestions</h3>
            <p className={styles.featureText}>
              Calculates market price suggestions for apartment listings using scraped regional market benchmarks and statistical forecasting.
            </p>
            <ul className={styles.featureList}>
              <li><CheckCircle2 size={16} className={styles.bulletIcon} /> Market benchmark price indicators</li>
              <li><CheckCircle2 size={16} className={styles.bulletIcon} /> Informational reference for owners & tenants</li>
            </ul>
          </div>

          <div className={styles.featureCard}>
            <div className={`${styles.featureIconBox} ${styles.iconPurple}`}>
              <Users size={24} />
            </div>
            <h3 className={styles.featureTitle}>Rental Applications</h3>
            <p className={styles.featureText}>
              Registered tenants can submit applications directly from apartment pages, providing owners with clear applicant profiles and contact information.
            </p>
            <ul className={styles.featureList}>
              <li><CheckCircle2 size={16} className={styles.bulletIcon} /> Tenant and Owner profile roles</li>
              <li><CheckCircle2 size={16} className={styles.bulletIcon} /> Structured application management</li>
            </ul>
          </div>

          <div className={styles.featureCard}>
            <div className={`${styles.featureIconBox} ${styles.iconWarning}`}>
              <Bell size={24} />
            </div>
            <h3 className={styles.featureTitle}>In-App & Email Alerts</h3>
            <p className={styles.featureText}>
              Receive status updates for listing submissions, application decisions, and profile changes through the notification center and transactional email.
            </p>
            <ul className={styles.featureList}>
              <li><CheckCircle2 size={16} className={styles.bulletIcon} /> In-app notification center</li>
              <li><CheckCircle2 size={16} className={styles.bulletIcon} /> Transactional email notifications</li>
            </ul>
          </div>
        </div>
      </section>

      {/* Core Principles */}
      <section className={styles.valuesSection}>
        <div className={styles.sectionHeader}>
          <span className={styles.sectionPill}>Principles</span>
          <h2 className={styles.sectionTitle}>What Guides the Platform</h2>
        </div>

        <div className={styles.valuesGrid}>
          <div className={styles.valueCard}>
            <div className={styles.valueHeader}>
              <Scale size={24} className={styles.valueIcon} />
              <h3 className={styles.valueTitle}>Transparency</h3>
            </div>
            <p className={styles.valueText}>
              Pricing trends, listing parameters, and application statuses are clearly visible to users without hidden intermediary fees.
            </p>
          </div>

          <div className={styles.valueCard}>
            <div className={styles.valueHeader}>
              <Lock size={24} className={styles.valueIcon} />
              <h3 className={styles.valueTitle}>Account Security</h3>
            </div>
            <p className={styles.valueText}>
              Personal records, passwords, and profiles are secured using standard encryption (BCrypt) and role-based access control.
            </p>
          </div>

          <div className={styles.valueCard}>
            <div className={styles.valueHeader}>
              <HeartHandshake size={24} className={styles.valueIcon} />
              <h3 className={styles.valueTitle}>Usability</h3>
            </div>
            <p className={styles.valueText}>
              Designed for a straightforward experience across both mobile and desktop screens, featuring clean navigation and dark mode support.
            </p>
          </div>
        </div>
      </section>

      {/* Call to Action */}
      <section className={styles.ctaBanner}>
        <div className={styles.ctaContent}>
          <h2 className={styles.ctaTitle}>Explore Available Apartments</h2>
          <p className={styles.ctaText}>
            Browse our listings catalog or register an account to submit your own properties or rental applications.
          </p>
          <div className={styles.ctaButtons}>
            <Link to="/listings" className={styles.ctaPrimaryBtn}>
              <span>Browse Apartments</span>
              <ArrowRight size={18} />
            </Link>
            <Link to="/contact" className={styles.ctaSecondaryBtn}>
              <Mail size={18} />
              <span>Contact Us</span>
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className={styles.pageFooter}>
        <p>Dynamic Rent Adjustment System (DRAS)</p>
      </footer>
    </div>
  );
}
