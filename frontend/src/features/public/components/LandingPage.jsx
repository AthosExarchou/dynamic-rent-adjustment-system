import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../../../shared/api/client';
import { FavouriteButton } from '../../favourites';
import {
  Building2, 
  ShieldCheck, 
  Headset, 
  ChevronDown, 
  ChevronUp, 
  Bed, 
  Bath, 
  Maximize, 
  Calendar, 
  Search,
  UserPlus,
  LogIn,
  UserCircle,
  TrendingUp,
  FileText,
  Camera
} from 'lucide-react';
import { useAuth } from '../../auth';
import { getImageUrl, DEFAULT_LISTING_IMAGE, REMOTE_FALLBACK_IMAGE } from '../../../shared/utils/imageUtils';
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
          <li>Once registered as an owner, the button changes to &quot;Create Listing&quot; - click it to open the listing form.</li>
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
          <li><strong>For Tenants:</strong> Click &quot;Apply&quot; on any available property to submit your rental application with your contact information and rental notes. You can monitor all your pending and approved applications from your profile.</li>
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
          <li>Every registered user can explore listings and apply as a tenant.</li>
          <li>To list a property, click &quot;Become an Owner to List&quot; on the Listings page to add an Owner profile to your existing account.</li>
          <li>Once registered, you can use &quot;Create Listing&quot; to post properties while maintaining all your tenant applications from a single unified account.</li>
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

  const [featuredListings, setFeaturedListings] = useState([]);
  const [topValueListings, setTopValueListings] = useState([]);

  useEffect(() => {
    const controller = new AbortController();
    fetchShowcaseListings(controller.signal);
    return () => controller.abort();
  }, []);

  const fetchShowcaseListings = async (signal) => {
    try {
      const data = await apiClient('/listings', { signal });
      const approved = data.filter(l => (l.status === 'APPROVED' || l.approved) && !l.rented && l.status !== 'RENTED');

      if (approved.length > 0) {
        // Featured Listings (Newest / Premium approved)
        const getTime = (l) => {
          const raw = l.updatedAt || l.createdAt;
          if (!raw) return 0;
          const time = new Date(raw).getTime();
          return isNaN(time) ? 0 : time;
        };

        const sortedByDate = [...approved].sort((a, b) => {
          const timeDiff = getTime(b) - getTime(a);
          return timeDiff !== 0 ? timeDiff : (Number(b.id || 0) - Number(a.id || 0));
        });

        const featuredWithPhotos = sortedByDate.filter(l => l.images && l.images.length > 0);
        const featuredWithoutPhotos = sortedByDate.filter(l => !l.images || l.images.length === 0);
        const featured = [...featuredWithPhotos, ...featuredWithoutPhotos].slice(0, 3);
        setFeaturedListings(featured.length > 0 ? featured : getFeaturedFallbackMocks());

        // Top Value Deals (Best price / size ratio)
        // Guard against string types, zero/null sizes, and unrealistic test values
        const validForRatio = approved.filter(l => {
          const price = Number(l.price);
          const size = Number(l.sizeM2);
          return !isNaN(price) && !isNaN(size) && price >= 50 && size >= 15;
        });

        const sortedByRatio = [...validForRatio].sort((a, b) => {
          const ratioA = Number(a.price) / Number(a.sizeM2);
          const ratioB = Number(b.price) / Number(b.sizeM2);
          return ratioA - ratioB || (Number(b.id || 0) - Number(a.id || 0));
        });

        const ratioWithPhotos = sortedByRatio.filter(l => l.images && l.images.length > 0);
        const ratioWithoutPhotos = sortedByRatio.filter(l => !l.images || l.images.length === 0);
        const valuePicks = [...ratioWithPhotos, ...ratioWithoutPhotos].slice(0, 3);
        setTopValueListings(valuePicks.length > 0 ? valuePicks : getTopValueFallbackMocks());
      } else {
        setFeaturedListings(getFeaturedFallbackMocks());
        setTopValueListings(getTopValueFallbackMocks());
      }
    } catch (err) {
      if (err.name === 'AbortError') return;
      console.error("Failed to fetch showcase listings:", err);
      setFeaturedListings(getFeaturedFallbackMocks());
      setTopValueListings(getTopValueFallbackMocks());
    }
  };

  const getFeaturedFallbackMocks = () => [
    {
      id: 101,
      title: "Luxury Downtown Loft",
      subtitle: "Spacious loft in prime city center",
      address: "123 Main St, Metropolis",
      bedrooms: 2,
      bathrooms: 2,
      sizeM2: 120,
      yearBuilt: 2021,
      price: 1500
    },
    {
      id: 102,
      title: "Modern Minimalist Penthouse",
      subtitle: "Panoramic skyline views and private terrace",
      address: "77 Acropolis View, Athens",
      bedrooms: 3,
      bathrooms: 2,
      sizeM2: 135,
      yearBuilt: 2023,
      price: 1800
    },
    {
      id: 103,
      title: "Urban Executive Residence",
      subtitle: "High-end finishes in a vibrant district",
      address: "12 Queen Sofia Ave, Athens",
      bedrooms: 2,
      bathrooms: 1,
      sizeM2: 85,
      yearBuilt: 2022,
      price: 1250
    }
  ];

  const getTopValueFallbackMocks = () => [
    {
      id: 201,
      title: "Spacious Family Maisonette",
      subtitle: "Exceptional value per square meter in quiet suburb",
      address: "45 Elm St, Marousi",
      bedrooms: 3,
      bathrooms: 2,
      sizeM2: 140,
      yearBuilt: 2012,
      price: 950
    },
    {
      id: 202,
      title: "Sunlit Suburban Apartment",
      subtitle: "Generous layout with large balcony and garden",
      address: "18 Pine St, Kifisia",
      bedrooms: 2,
      bathrooms: 1,
      sizeM2: 95,
      yearBuilt: 2008,
      price: 700
    },
    {
      id: 203,
      title: "Renovated Garden Studio",
      subtitle: "Affordable and efficient modern space",
      address: "9 Green Park, Chalandri",
      bedrooms: 1,
      bathrooms: 1,
      sizeM2: 55,
      yearBuilt: 2015,
      price: 450
    }
  ];

  return (
    <div className={styles.pageWrapper}>
      {/* Hero Section */}
      <section className={styles.heroSection}>
        <div className={styles.heroOverlay}></div>
        <div className={styles.heroContent}>
          <h1 className={styles.heroTitle}>Find Your Ideal Rental Property</h1>
          <p className={styles.heroSubtitle}>
            Browse listings, connect with owners, and move into your new home with ease.
          </p>
          <Link to="/listings" className={styles.heroBtn}>
            <Building2 className={styles.btnIcon} size={20} /> Browse Listings
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
              <p className={styles.featureDesc}>Every property listing is carefully reviewed and approved by our team for quality and accuracy.</p>
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
              <p className={styles.featureDesc}>Apply for properties with a single click and manage all your rental workflows from one dashboard.</p>
            </div>
            <div className={styles.featureCard}>
              <Headset size={48} className={styles.featureIcon} />
              <h3 className={styles.featureTitle}>24/7 Support</h3>
              <p className={styles.featureDesc}>Our dedicated team is here around the clock to help with any questions or issues you may encounter.</p>
            </div>
          </div>
        </section>

        {/* Featured Listings Section */}
        {featuredListings.length > 0 && (
          <section className={styles.section}>
            <h2 className={styles.sectionTitle}>Featured Listings</h2>
            <p className={styles.sectionSubtitle}>
              Hand-picked and newly verified properties across Athens.
            </p>
            <div className={styles.apartmentsGrid}>
              {featuredListings.map(apt => {
                const hasImages = apt.images && apt.images.length > 0;
                const cardImageUrl = hasImages ? (getImageUrl(apt.images[0]) || DEFAULT_LISTING_IMAGE) : DEFAULT_LISTING_IMAGE;
                return (
                  <div key={apt.id} className={styles.apartmentCard}>
                    <div className={styles.imageWrapper}>
                      <FavouriteButton listing={apt} variant="card" />
                      <Link to={`/listings/${apt.id}`} className={styles.imageLink} aria-label={`View details for ${apt.title}`}>
                        <img
                          src={cardImageUrl}
                          alt={hasImages ? apt.title : 'Generic demo preview'}
                          className={styles.image}
                          loading="lazy"
                          decoding="async"
                          onError={(e) => {
                            if (e.target.src.includes('demo/image1.jpg')) {
                              e.target.src = REMOTE_FALLBACK_IMAGE;
                            } else if (e.target.src !== DEFAULT_LISTING_IMAGE && e.target.src !== REMOTE_FALLBACK_IMAGE) {
                              e.target.src = DEFAULT_LISTING_IMAGE;
                            }
                          }}
                        />
                      </Link>
                      {hasImages ? (
                        apt.images.length > 1 && (
                          <span className={styles.imageCountBadge} title={`${apt.images.length} photos available`}>
                            <Camera size={13} /> {apt.images.length}
                          </span>
                        )
                      ) : (
                        <span className={styles.demoBadge} title="Demo image - no photos uploaded">
                          <Camera size={12} /> Demo Preview
                        </span>
                      )}
                    </div>
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
              );
            })}
            </div>
          </section>
        )}

        {/* Top Value Deals Section */}
        {topValueListings.length > 0 && (
          <section className={styles.section}>
            <h2 className={styles.sectionTitle}>Top Value Deals</h2>
            <p className={styles.sectionSubtitle}>
              Top verified properties offering the best price-to-space ratio on the market.
            </p>
            <div className={styles.apartmentsGrid}>
              {topValueListings.map(apt => {
                const hasImages = apt.images && apt.images.length > 0;
                const cardImageUrl = hasImages ? (getImageUrl(apt.images[0]) || DEFAULT_LISTING_IMAGE) : DEFAULT_LISTING_IMAGE;
                const numPrice = Number(apt.price);
                const numSize = Number(apt.sizeM2);
                const pricePerM2 = (!isNaN(numPrice) && !isNaN(numSize) && numSize > 0)
                  ? (numPrice / numSize).toFixed(1)
                  : (apt.pricePerM2 ? Number(apt.pricePerM2).toFixed(1) : null);
                return (
                  <div key={apt.id} className={styles.apartmentCard}>
                    <div className={styles.imageWrapper}>
                      <FavouriteButton listing={apt} variant="card" />
                      <Link to={`/listings/${apt.id}`} className={styles.imageLink} aria-label={`View details for ${apt.title}`}>
                        <img
                          src={cardImageUrl}
                          alt={hasImages ? apt.title : 'Generic demo preview'}
                          className={styles.image}
                          loading="lazy"
                          decoding="async"
                          onError={(e) => {
                            if (e.target.src.includes('demo/image1.jpg')) {
                              e.target.src = REMOTE_FALLBACK_IMAGE;
                            } else if (e.target.src !== DEFAULT_LISTING_IMAGE && e.target.src !== REMOTE_FALLBACK_IMAGE) {
                              e.target.src = DEFAULT_LISTING_IMAGE;
                            }
                          }}
                        />
                      </Link>
                      {hasImages ? (
                        apt.images.length > 1 && (
                          <span className={styles.imageCountBadge} title={`${apt.images.length} photos available`}>
                            <Camera size={13} /> {apt.images.length}
                          </span>
                        )
                      ) : (
                        <span className={styles.demoBadge} title="Demo image - no photos uploaded">
                          <Camera size={12} /> Demo Preview
                        </span>
                      )}
                    </div>
                    <div className={styles.apartmentBody}>
                    <h5 className={styles.apartmentTitle} title={apt.title}>{apt.title}</h5>
                    {apt.subtitle && (
                      <p className={styles.apartmentSubtitle} title={apt.subtitle}>{apt.subtitle}</p>
                    )}
                    <p className={styles.apartmentLocation} title={apt.address}>{apt.address}</p>

                    <div className={styles.badgesWrapper}>
                      <span className={`${styles.badge} ${styles.badgePremium}`}>
                        <TrendingUp size={12} /> Best Value
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

                    <p className={styles.apartmentPrice}>
                      {apt.price} € / month
                      {pricePerM2 && (
                        <span className={styles.pricePerM2}>
                          {' '}• {pricePerM2} €/m²
                        </span>
                      )}
                    </p>

                    <div className={styles.apartmentFooter}>
                      <Link to={`/listings/${apt.id}`} className={styles.viewBtn}>
                        <Search size={16} /> View Details
                      </Link>
                    </div>
                  </div>
                </div>
              );
            })}
            </div>
          </section>
        )}

        {/* Ready to Get Started */}
        <section className={`${styles.section} ${styles.textCenter}`}>
          <h2 className={styles.sectionTitle}>Ready to Get Started?</h2>
          
          {!isAuthenticated ? (
            <div className={styles.ctaWrapper}>
              <p className={styles.ctaDesc}>Sign up today to find the perfect rental property or list your own.</p>
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
              <p className={styles.ctaDesc}>Browse available properties or manage your listings directly from your account.</p>
              <div className={styles.ctaButtons}>
                <Link to="/listings" className={`${styles.btn} ${styles.btnPrimary} ${styles.btnPulse}`}>
                  <Building2 size={20} /> Browse Listings
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
