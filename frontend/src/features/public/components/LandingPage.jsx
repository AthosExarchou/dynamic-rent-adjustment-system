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
                    <h5 className={styles.apartmentTitle}>{apt.title}</h5>
                    <p className={styles.apartmentLocation}>{apt.address}</p>

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
            <div className={styles.faqItem}>
              <button className={styles.faqQuestion} onClick={() => setOpenFaq(openFaq === 0 ? null : 0)}>
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
              <button className={styles.faqQuestion} onClick={() => setOpenFaq(openFaq === 1 ? null : 1)}>
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
              <button className={styles.faqQuestion} onClick={() => setOpenFaq(openFaq === 2 ? null : 2)}>
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
    </div>
  );
}
