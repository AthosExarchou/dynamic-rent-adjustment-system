import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import {
  ShieldCheck, 
  ChevronDown, 
  List, 
  Database, 
  Lock, 
  TrendingUp, 
  Share2, 
  UserCheck, 
  Clock, 
  Mail, 
  CheckCircle2
} from 'lucide-react';
import styles from './PrivacyPolicy.module.css';

const SECTIONS = [
  { id: 'section1', label: '1. Introduction & Overview', icon: ShieldCheck },
  { id: 'section2', label: '2. Information We Collect', icon: Database },
  { id: 'section3', label: '3. How We Use Your Data', icon: Lock },
  { id: 'section4', label: '4. Market Data & Cookies', icon: TrendingUp },
  { id: 'section5', label: '5. Security & Retention', icon: Lock },
  { id: 'section6', label: '6. Third-Party Disclosures', icon: Share2 },
  { id: 'section7', label: '7. Your Rights & GDPR', icon: UserCheck },
  { id: 'section8', label: '8. Updates & Contact', icon: Mail },
];

export default function PrivacyPolicy() {
  const [activeSection, setActiveSection] = useState('section1');
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const sectionRefs = useRef({});

  useEffect(() => {
    const handleScroll = () => {
      const scrollPosition = window.scrollY + 250;

      for (const [id, el] of Object.entries(sectionRefs.current)) {
        if (el) {
          const top = el.offsetTop;
          const height = el.offsetHeight;
          if (scrollPosition >= top && scrollPosition < top + height) {
            setActiveSection(id);
          }
        }
      }
    };

    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const scrollToSection = (e, id) => {
    e.preventDefault();
    if (sectionRefs.current[id]) {
      window.scrollTo({
        top: sectionRefs.current[id].offsetTop - 100,
        behavior: 'smooth'
      });
      setActiveSection(id);
      setMobileMenuOpen(false);
    }
  };

  const currentSection = SECTIONS.find(s => s.id === activeSection) || SECTIONS[0];

  return (
    <div className={styles.container}>
      {/* Header */}
      <header className={styles.header}>
        <div className={styles.headerBadge}>
          <ShieldCheck size={16} className={styles.badgeIcon} />
          <span>Privacy & Data Protection</span>
        </div>
        <h1 className={styles.title}>Privacy Policy</h1>
        <p className={styles.subtitle}>
          Last Updated: June 20, 2026 · Effective Date: January 1, 2025
        </p>
      </header>

      <div className={styles.layout}>
        {/* Sticky Sidebar / Table of Contents */}
        <aside className={styles.sidebar}>
          <div className={styles.sidebarWrapper}>
            <button
              type="button"
              className={styles.mobileToggle}
              onClick={() => setMobileMenuOpen(prev => !prev)}
              aria-expanded={mobileMenuOpen}
              aria-controls="privacy-toc-nav"
              aria-label="Toggle Table of Contents"
            >
              <div className={styles.mobileToggleLeft}>
                <List size={18} className={styles.tocIcon} />
                <span className={styles.mobileToggleLabel}>Table of Contents</span>
              </div>
              <div className={styles.mobileToggleRight}>
                <span className={styles.activeBadge}>{currentSection.label.split('.')[0]}</span>
                <ChevronDown
                  size={18}
                  className={`${styles.chevron} ${mobileMenuOpen ? styles.chevronOpen : ''}`}
                />
              </div>
            </button>

            <nav
              id="privacy-toc-nav"
              className={`${styles.nav} ${mobileMenuOpen ? styles.navOpen : ''}`}
              aria-label="Table of Contents"
            >
              <div className={styles.desktopSidebarTitle}>Table of Contents</div>
              {SECTIONS.map((sec) => (
                <a
                  key={sec.id}
                  href={`#${sec.id}`}
                  className={`${styles.navLink} ${activeSection === sec.id ? styles.active : ''}`}
                  onClick={(e) => scrollToSection(e, sec.id)}
                >
                  <span className={styles.navLinkText}>{sec.label}</span>
                </a>
              ))}
            </nav>
          </div>
        </aside>

        {/* Policy Content */}
        <main className={styles.mainContent}>
          {/* Section 1 */}
          <section
            id="section1"
            ref={(el) => (sectionRefs.current['section1'] = el)}
            className={styles.contentSection}
          >
            <div className={styles.sectionHeaderRow}>
              <div className={`${styles.sectionIconBox} ${styles.iconBlue}`}>
                <ShieldCheck size={22} />
              </div>
              <h2 className={styles.sectionTitle}>1. Introduction & Overview</h2>
            </div>
            <p>
              This Privacy Policy explains how the <strong>Dynamic Rent Adjustment System</strong> ("DRAS", "we", "us", or "our")
              handles and protects user information when you use our website and application services.
            </p>
            <p>
              By accessing DRAS, registering an account, submitting an apartment listing, or submitting a rental application,
              you acknowledge the data practices described in this document.
            </p>
            <div className={styles.calloutBox}>
              <strong className={styles.calloutTitle}>Commitment to Privacy:</strong>
              <p className={styles.calloutText}>
                We do not sell personal data or distribute user information to commercial advertising networks. Data is processed solely to operate account features, listings, rental applications, and price suggestions.
              </p>
            </div>
          </section>

          {/* Section 2 */}
          <section
            id="section2"
            ref={(el) => (sectionRefs.current['section2'] = el)}
            className={styles.contentSection}
          >
            <div className={styles.sectionHeaderRow}>
              <div className={`${styles.sectionIconBox} ${styles.iconPurple}`}>
                <Database size={22} />
              </div>
              <h2 className={styles.sectionTitle}>2. Information We Collect</h2>
            </div>
            <p>We collect information necessary to operate the application and support user accounts:</p>
            
            <div className={styles.subCard}>
              <h3 className={styles.subCardTitle}>A. User Account Details</h3>
              <p>Username, email address, and encrypted passwords (hashed with BCrypt). Plain-text passwords are never stored.</p>
            </div>

            <div className={styles.subCard}>
              <h3 className={styles.subCardTitle}>B. Profile Details</h3>
              <ul className={styles.checkList}>
                <li><CheckCircle2 size={16} className={styles.checkIcon} /> <strong>Owners:</strong> Name, phone number, and optional company name.</li>
                <li><CheckCircle2 size={16} className={styles.checkIcon} /> <strong>Tenants:</strong> Name, contact phone number, employment status, and gross annual income used in rental applications.</li>
              </ul>
            </div>

            <div className={styles.subCard}>
              <h3 className={styles.subCardTitle}>C. Apartment Listings Data</h3>
              <p>Property characteristics (square meters, bedrooms, floor, energy class, heating type, construction year, rental price, address) and uploaded photos.</p>
            </div>

            <div className={styles.subCard}>
              <h3 className={styles.subCardTitle}>D. Technical Audit Information</h3>
              <p>Standard server security logs, authentication events, and audit records to maintain system integrity.</p>
            </div>
          </section>

          {/* Section 3 */}
          <section
            id="section3"
            ref={(el) => (sectionRefs.current['section3'] = el)}
            className={styles.contentSection}
          >
            <div className={styles.sectionHeaderRow}>
              <div className={`${styles.sectionIconBox} ${styles.iconGreen}`}>
                <Lock size={22} />
              </div>
              <h2 className={styles.sectionTitle}>3. How We Use Your Data</h2>
            </div>
            <p>Data collected by DRAS is used for specific platform operations:</p>
            
            <ul className={styles.bulletList}>
              <li><strong>Authentication & Role Management:</strong> Verifying logins and maintaining permissions for Users, Tenants, Owners, and Admins.</li>
              <li><strong>Rental Applications:</strong> Allowing tenants to apply for listings and enabling owners to review applicant details.</li>
              <li><strong>Price Suggestions:</strong> Providing informational price suggestions based on regional benchmarks and statistical forecasting.</li>
              <li><strong>Notifications:</strong> Sending in-app notifications and transactional emails regarding account events and listing decisions.</li>
              <li><strong>Security:</strong> Monitoring access to prevent unauthorized account changes and system abuse.</li>
            </ul>
          </section>

          {/* Section 4 */}
          <section
            id="section4"
            ref={(el) => (sectionRefs.current['section4'] = el)}
            className={styles.contentSection}
          >
            <div className={styles.sectionHeaderRow}>
              <div className={`${styles.sectionIconBox} ${styles.iconBlue}`}>
                <TrendingUp size={22} />
              </div>
              <h2 className={styles.sectionTitle}>4. Market Data & Cookies</h2>
            </div>
            <p>
              DRAS uses aggregated real estate market indicators to provide price estimates.
            </p>
            <div className={styles.highlightBlock}>
              <h4>Anonymized Benchmarks</h4>
              <p>
                Market benchmark data is aggregated from public real estate listings. No personal user data is included in market benchmarks or forecasting models.
              </p>
            </div>
            <h3 className={styles.innerHeading}>Cookies & Local Storage</h3>
            <p>
              We only use functional cookies and storage necessary for application functionality:
            </p>
            <ul className={styles.checkList}>
              <li><CheckCircle2 size={16} className={styles.checkIcon} /> <strong>Session Tokens:</strong> Token-based authentication (JWT) stored to maintain your active login session.</li>
              <li><CheckCircle2 size={16} className={styles.checkIcon} /> <strong>Theme Preferences:</strong> Local storage setting for Light / Dark visual mode.</li>
            </ul>
          </section>

          {/* Section 5 */}
          <section
            id="section5"
            ref={(el) => (sectionRefs.current['section5'] = el)}
            className={styles.contentSection}
          >
            <div className={styles.sectionHeaderRow}>
              <div className={`${styles.sectionIconBox} ${styles.iconAmber}`}>
                <Clock size={22} />
              </div>
              <h2 className={styles.sectionTitle}>5. Security & Retention</h2>
            </div>
            <p>
              We take appropriate security measures to safeguard stored data against unauthorized access or loss:
            </p>
            <ul className={styles.bulletList}>
              <li><strong>Encrypted Connections:</strong> Encrypted data transmission using standard TLS.</li>
              <li><strong>Database Integrity:</strong> Relational constraints and orphan record cleanup on user and listing changes.</li>
              <li><strong>Account Deletion:</strong> When an account is deleted, associated profile data, applications, and user notifications are removed from the database.</li>
            </ul>
          </section>

          {/* Section 6 */}
          <section
            id="section6"
            ref={(el) => (sectionRefs.current['section6'] = el)}
            className={styles.contentSection}
          >
            <div className={styles.sectionHeaderRow}>
              <div className={`${styles.sectionIconBox} ${styles.iconPurple}`}>
                <Share2 size={22} />
              </div>
              <h2 className={styles.sectionTitle}>6. Third-Party Disclosures</h2>
            </div>
            <p>
              We do not sell or rent personal information to third parties. Information is only shared under limited circumstances:
            </p>
            <ul className={styles.bulletList}>
              <li><strong>Infrastructure Services:</strong> Service providers hosting system databases, transactional email delivery, or map embeds.</li>
              <li><strong>Legal Requirements:</strong> If required by applicable law, court order, or official regulation.</li>
            </ul>
          </section>

          {/* Section 7 */}
          <section
            id="section7"
            ref={(el) => (sectionRefs.current['section7'] = el)}
            className={styles.contentSection}
          >
            <div className={styles.sectionHeaderRow}>
              <div className={`${styles.sectionIconBox} ${styles.iconGreen}`}>
                <UserCheck size={22} />
              </div>
              <h2 className={styles.sectionTitle}>7. Your Rights & Account Controls</h2>
            </div>
            <p>You have control over your data within the system:</p>

            <div className={styles.rightsGrid}>
              <div className={styles.rightCard}>
                <h4 className={styles.rightTitle}>View Profile Details</h4>
                <p>View your stored account, owner, or tenant profile records directly from your account page.</p>
              </div>

              <div className={styles.rightCard}>
                <h4 className={styles.rightTitle}>Update Information</h4>
                <p>Edit your username, email, phone number, or profile details anytime through account settings.</p>
              </div>

              <div className={styles.rightCard}>
                <h4 className={styles.rightTitle}>Account Deletion</h4>
                <p>Delete your account and all associated domain data at any time via the account deletion confirmation page.</p>
              </div>

              <div className={styles.rightCard}>
                <h4 className={styles.rightTitle}>Inquiries</h4>
                <p>Contact the DRAS administration team if you have questions about your stored data.</p>
              </div>
            </div>
          </section>

          {/* Section 8 */}
          <section
            id="section8"
            ref={(el) => (sectionRefs.current['section8'] = el)}
            className={styles.contentSection}
          >
            <div className={styles.sectionHeaderRow}>
              <div className={`${styles.sectionIconBox} ${styles.iconBlue}`}>
                <Mail size={22} />
              </div>
              <h2 className={styles.sectionTitle}>8. Updates & Contact</h2>
            </div>
            <p>
              This policy may be updated periodically to reflect system modifications or operational adjustments. The "Last Updated" date indicates when the current version took effect.
            </p>
            <p>
              If you have any questions regarding this Privacy Policy, please contact us:
            </p>
            <div className={styles.contactCard}>
              <div className={styles.contactDetails}>
                <strong>DRAS Administration</strong>
                <p>Email: <a href="mailto:dras.system@gmail.com" className={styles.link}>dras.system@gmail.com</a></p>
                <p>Location: Athens, Greece</p>
              </div>
              <Link to="/contact" className={styles.contactButton}>
                Open Contact Form
              </Link>
            </div>
          </section>

          <footer className={styles.footer}>
            <p>Dynamic Rent Adjustment System (DRAS)</p>
          </footer>
        </main>
      </div>
    </div>
  );
}
