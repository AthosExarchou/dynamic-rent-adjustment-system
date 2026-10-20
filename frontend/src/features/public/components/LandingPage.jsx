import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../../../shared/api/client';
import {
  Building2, 
  ShieldCheck, 
  Headset, 
  ChevronDown, 
  ChevronUp, 
  Factory, 
  Bed, 
  Bath, 
  Maximize, 
  Calendar, 
  Search,
  UserPlus,
  LogIn,
  UserCircle,
  TrendingUp,
  FileText
} from 'lucide-react';
import { useAuth } from '../../auth';
import styles from './LandingPage.module.css';

const FAQ_ITEMS = [
  {
    question: 'How can I search for property listings on DRAS?',
    answer: (
      <>
        <p>DRAS makes it easy and quick to find your ideal place. Here is how you can search for property listings:</p>
        <ol>
          <li>Visit the DRAS website.</li>
          <li>Go to the Listings page.</li>
          <li>Refine your search further by using the available filters (price range, area size, property type, etc.).</li>
          <li>Click &quot;Search&quot; to display listings that match your criteria.</li>
          <li>Browse the listings you are interested in, and click &quot;Apply&quot; to submit your rental application.</li>
        </ol>
      </>
    )
  },
  {
    question: 'What is the process for listing a property on DRAS?',
    answer: (
      <>
        <p>If you want to publish a rental property on DRAS, the process is straightforward:</p>
        <ol>
          <li>Register or sign in to your account.</li>
          <li>Navigate to the Listings page. If you are not yet an owner, click &quot;Become an Owner to List&quot; to register your Owner Profile.</li>
          <li>Once registered as an owner, the button changes to &quot;Create New Apartment&quot; - click it to open the listing form.</li>
          <li>Complete all the required fields with your property details (and optionally apply our real-time Market Price Suggestion).</li>
          <li>Submit your listing. Once reviewed and approved by administrators, it becomes available to prospective tenants.</li>
        </ol>
      </>
    )
  },
  {
    question: 'How does the Market Price Suggestion tool work for property owners?',
    answer: (
      <>
        <p>When creating or editing a listing, DRAS provides an intelligent, automated price recommendation calculated from:</p>
        <ul>
          <li><strong>Auto-ARIMA Micro-Forecasts:</strong> Statistical time-series trend forecasting with 95% confidence intervals.</li>
          <li><strong>Scraped Market Benchmarks:</strong> Live comparative market benchmarks aggregated from residential rental platforms in Athens.</li>
          <li><strong>Property Micro-Adjustments:</strong> Feature-level evaluations based on area size (m²), floor level, year built, bedrooms, and bathrooms.</li>
        </ul>
        <p>Owners can view the estimated fair market range, inspect trend signals, and apply the suggested competitive rate with a single click.</p>
      </>
    )
  },
  {
    question: 'What do the "Price recently lowered" and "Below market average" badges mean?',
    answer: (
      <>
        <p>DRAS automatically analyzes rental prices across all active listings to help tenants discover great opportunities:</p>
        <ul>
          <li><strong>Price recently lowered:</strong> Highlighted when a property owner has reduced the monthly rent within the past 30 days.</li>
          <li><strong>Below market average:</strong> Indicated when a listing&#39;s rent per square meter (€/m²) is lower than the prevailing citywide benchmark for comparable properties.</li>
        </ul>
        <p>These badges allow tenants to quickly spot high-value deals and promote transparent, market-aligned pricing.</p>
      </>
    )
  },
  {
    question: 'How do rental applications work for tenants and owners?',
    answer: (
      <>
        <p>DRAS connects prospective tenants and property owners through a streamlined application workflow:</p>
        <ul>
          <li><strong>For Tenants:</strong> Click &quot;Apply&quot; on any available apartment to submit your tenant application with your contact information and rental notes. You can monitor all your pending and approved applications from your profile.</li>
          <li><strong>For Owners:</strong> Review incoming applicant profiles, contact information, and lease notes directly from your Listings &amp; Applications Dashboard to approve or reject candidates with one click.</li>
        </ul>
      </>
    )
  },
  {
    question: 'Can I be both a tenant and a property owner on the same account?',
    answer: (
      <>
        <p>Yes! DRAS features a flexible multi-role system designed for modern renters and owners:</p>
        <ul>
          <li>Every registered user can explore apartments and apply as a tenant.</li>
          <li>To list a property, click &quot;Become an Owner to List&quot; on the Listings page to add an Owner profile to your existing account.</li>
          <li>Once registered, you can use &quot;Create New Apartment&quot; to post properties while maintaining all your tenant applications from a single unified account.</li>
        </ul>
      </>
    )
  },
  {
    question: 'What additional services or tools can help me?',
    answer: (
      <>
        <p>By creating an account on DRAS, your personal dashboard allows you to:</p>
        <ul>
          <li>Seamlessly manage both tenant and owner activities from a single profile.</li>
          <li>Track all your pending, approved, and rejected rental applications.</li>
          <li>Monitor applicant pipelines and property statuses in real time.</li>
          <li>Access automated price suggestions and data-driven market forecasts.</li>
        </ul>
        <p>This provides complete visibility and control over your rental journey in one place.</p>
      </>
    )
  }
];

export default function LandingPage() {
  const { isAuthenticated } = useAuth();
  const [openFaq, setOpenFaq] = useState(null);

  const [featuredApartments, setFeaturedApartments] = useState([]);

  useEffect(() => {
    const controller = new AbortController();
    fetchFeaturedListings(controller.signal);
    return () => controller.abort();
  }, []);

  const fetchFeaturedListings = async (signal) => {
    try {
      const data = await apiClient('/listings', { signal });
      const approved = data.filter(l => l.status === 'APPROVED' || l.approved);
      if (approved.length > 0) {
        setFeaturedApartments(approved.slice(0, 3));
      } else {
        setFeaturedApartments(getFallbackMocks());
      }
    } catch (err) {
      if (err.name === 'AbortError') return;
      console.error("Failed to fetch featured listings:", err);
      setFeaturedApartments(getFallbackMocks());
    }
  };

  const getFallbackMocks = () => [
    {
      id: 1,
      title: "Luxury Downtown Loft",
      subtitle: "Spacious loft in prime city center",
      address: "123 Main St, Metropolis",
      bedrooms: 2,
      bathrooms: 2,
      sizeM2: 120,
      yearBuilt: 2018,
      price: 1500
    },
    {
      id: 2,
      title: "Cozy Suburb Apartment",
      subtitle: "Quiet neighborhood with beautiful garden",
      address: "45 Elm St, Springfield",
      bedrooms: 3,
      bathrooms: 1,
      sizeM2: 95,
      yearBuilt: 2005,
      price: 900
    },
    {
      id: 3,
      title: "Modern Studio",
      subtitle: "Compact and modern living space",
      address: "88 Tech Blvd, Metropolis",
      bedrooms: 1,
      bathrooms: 1,
      sizeM2: 55,
      yearBuilt: 2022,
      price: 1100
    }
  ];

  return (
    <div className={styles.pageWrapper}>
      {/* Hero Section */}
      <section className={styles.heroSection}>
        <div className={styles.heroOverlay}></div>
        <div className={styles.heroContent}>
          <h1 className={styles.heroTitle}>Find Your Dream Apartment</h1>
          <p className={styles.heroSubtitle}>
            Browse listings, connect with owners, and move into your new home with ease.
          </p>
          <Link to="/listings" className={styles.heroBtn}>
            <Factory className={styles.btnIcon} size={20} /> Browse Apartments
          </Link>
        </div>
      </section>

      <div className={styles.container}>
        {/* Features Section */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Why Choose Us?</h2>
          <div className={styles.featuresGrid}>
            <div className={styles.featureCard}>
              <Building2 size={48} className={styles.featureIcon} />
              <h3 className={styles.featureTitle}>Dynamically Updated Listings</h3>
              <p className={styles.featureDesc}>Experience smart, real-time rent adjustments that ensure competitive pricing and fair market value.</p>
            </div>
            <div className={styles.featureCard}>
              <ShieldCheck size={48} className={styles.featureIcon} />
              <h3 className={styles.featureTitle}>Verified Listings</h3>
              <p className={styles.featureDesc}>Every apartment is carefully reviewed and approved by our team for quality and accuracy.</p>
            </div>
            <div className={styles.featureCard}>
              <UserCircle size={48} className={styles.featureIcon} />
              <h3 className={styles.featureTitle}>Trusted Platform</h3>
              <p className={styles.featureDesc}>Secure user accounts and transparent communication between tenants and property owners.</p>
            </div>
            <div className={styles.featureCard}>
              <TrendingUp size={48} className={styles.featureIcon} />
              <h3 className={styles.featureTitle}>Market Analytics</h3>
              <p className={styles.featureDesc}>Leverage deep insights into the rental market to make informed decisions for your property.</p>
            </div>
            <div className={styles.featureCard}>
              <FileText size={48} className={styles.featureIcon} />
              <h3 className={styles.featureTitle}>Seamless Applications</h3>
              <p className={styles.featureDesc}>Apply for apartments with a single click and manage all your rental workflows from one dashboard.</p>
            </div>
            <div className={styles.featureCard}>
              <Headset size={48} className={styles.featureIcon} />
              <h3 className={styles.featureTitle}>24/7 Support</h3>
              <p className={styles.featureDesc}>Our dedicated team is here around the clock to help with any questions or issues you may encounter.</p>
            </div>
          </div>
        </section>

        {/* Featured Apartments Section */}
        {featuredApartments.length > 0 && (
          <section className={styles.section}>
            <h2 className={styles.sectionTitle}>Featured Apartments</h2>
            <div className={styles.apartmentsGrid}>
              {featuredApartments.map(apt => (
                <div key={apt.id} className={styles.apartmentCard}>
                  <div className={styles.apartmentBody}>
                    <h5 className={styles.apartmentTitle} title={apt.title}>{apt.title}</h5>
                    {apt.subtitle && (
                      <p className={styles.apartmentSubtitle} title={apt.subtitle}>{apt.subtitle}</p>
                    )}
                    <p className={styles.apartmentLocation} title={apt.address}>{apt.address}</p>

                    <div className={styles.badgesWrapper}>
                      <span className={`${styles.badge} ${styles.badgePremium}`}>
                        Featured
                      </span>
                      <span className={`${styles.badge} ${styles.badgePrimary}`}>
                        <Bed size={14} /> {apt.bedrooms} Beds
                      </span>
                      <span className={`${styles.badge} ${styles.badgeSecondary}`}>
                        <Bath size={14} /> {apt.bathrooms} Baths
                      </span>
                      <span className={`${styles.badge} ${styles.badgeInfo}`}>
                        <Maximize size={14} /> {apt.sizeM2} m²
                      </span>
                      <span className={`${styles.badge} ${styles.badgeLight}`}>
                        <Calendar size={14} /> {apt.yearBuilt}
                      </span>
                    </div>

                    <p className={styles.apartmentPrice}>{apt.price} € / month</p>

                    <div className={styles.apartmentFooter}>
                      <Link to={`/listings/${apt.id}`} className={styles.viewBtn}>
                        <Search size={16} /> View Details
                      </Link>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Ready to Get Started */}
        <section className={`${styles.section} ${styles.textCenter}`}>
          <h2 className={styles.sectionTitle}>Ready to Get Started?</h2>
          
          {!isAuthenticated ? (
            <div className={styles.ctaWrapper}>
              <p className={styles.ctaDesc}>Sign up today to find the perfect apartment or list your own property.</p>
              <div className={styles.ctaButtons}>
                <Link to="/register" className={`${styles.btn} ${styles.btnGradient}`}>
                  <UserPlus size={20} /> Create an Account
                </Link>
                <Link to="/login" className={`${styles.btn} ${styles.btnOutline}`}>
                  <LogIn size={20} /> Sign In
                </Link>
              </div>
            </div>
          ) : (
            <div className={styles.ctaWrapper}>
              <p className={styles.ctaDesc}>Browse available apartments or manage your listings directly from your account.</p>
              <div className={styles.ctaButtons}>
                <Link to="/listings" className={`${styles.btn} ${styles.btnPrimary} ${styles.btnPulse}`}>
                  <Factory size={20} /> Browse Apartments
                </Link>
                <Link to="/profile" className={`${styles.btn} ${styles.btnOutlineInfo}`}>
                  <UserCircle size={20} /> My Profile
                </Link>
              </div>
            </div>
          )}
        </section>

        {/* FAQ Section */}
        <section className={`${styles.section} ${styles.faqSection}`}>
          <h2 className={styles.sectionTitle}>Frequently Asked Questions</h2>
          <div className={styles.faqList}>
            {FAQ_ITEMS.map((item, index) => (
              <div key={index} className={styles.faqItem}>
                <button
                  type="button"
                  className={styles.faqQuestion}
                  onClick={() => setOpenFaq(openFaq === index ? null : index)}
                  aria-expanded={openFaq === index}
                >
                  <span>{item.question}</span>
                  {openFaq === index ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
                </button>
                {openFaq === index && (
                  <div className={styles.faqAnswer}>
                    {item.answer}
                  </div>
                )}
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}
