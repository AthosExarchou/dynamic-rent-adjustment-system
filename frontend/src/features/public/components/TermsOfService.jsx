import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import styles from './TermsOfService.module.css';

export default function TermsOfService() {
  const [activeSection, setActiveSection] = useState('section1');
  
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

    window.addEventListener('scroll', handleScroll);
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
    }
  };

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <h1 className={styles.title}>Terms of Service</h1>
        <p className={styles.subtitle}>Last Updated: September 20, 2025</p>
      </header>

      <div className={styles.layout}>
        <aside className={styles.sidebar}>
          <nav className={styles.nav}>
            <a href="#section1" className={`${styles.navLink} ${activeSection === 'section1' ? styles.active : ''}`}
               onClick={(e) => scrollToSection(e, 'section1')}>1. Agreement to Terms</a>
            <a href="#section2" className={`${styles.navLink} ${activeSection === 'section2' ? styles.active : ''}`}
               onClick={(e) => scrollToSection(e, 'section2')}>2. User Accounts</a>
            <a href="#section3" className={`${styles.navLink} ${activeSection === 'section3' ? styles.active : ''}`}
               onClick={(e) => scrollToSection(e, 'section3')}>3. Use of the Service</a>
            <a href="#section4" className={`${styles.navLink} ${activeSection === 'section4' ? styles.active : ''}`}
               onClick={(e) => scrollToSection(e, 'section4')}>4. User-Generated Content</a>
            <a href="#section5" className={`${styles.navLink} ${activeSection === 'section5' ? styles.active : ''}`}
               onClick={(e) => scrollToSection(e, 'section5')}>5. Intellectual Property</a>
            <a href="#section6" className={`${styles.navLink} ${activeSection === 'section6' ? styles.active : ''}`}
               onClick={(e) => scrollToSection(e, 'section6')}>6. Disclaimers</a>
            <a href="#section7" className={`${styles.navLink} ${activeSection === 'section7' ? styles.active : ''}`}
               onClick={(e) => scrollToSection(e, 'section7')}>7. Limitation of Liability</a>
            <a href="#section8" className={`${styles.navLink} ${activeSection === 'section8' ? styles.active : ''}`}
               onClick={(e) => scrollToSection(e, 'section8')}>8. Termination</a>
            <a href="#section9" className={`${styles.navLink} ${activeSection === 'section9' ? styles.active : ''}`}
               onClick={(e) => scrollToSection(e, 'section9')}>9. Governing Law</a>
            <a href="#section10" className={`${styles.navLink} ${activeSection === 'section10' ? styles.active : ''}`}
               onClick={(e) => scrollToSection(e, 'section10')}>10. Changes to Terms</a>
            <a href="#section11" className={`${styles.navLink} ${activeSection === 'section11' ? styles.active : ''}`}
               onClick={(e) => scrollToSection(e, 'section11')}>11. Contact Us</a>
          </nav>
        </aside>

        <main className={styles.mainContent}>
          <section id="section1" ref={el => sectionRefs.current['section1'] = el} className={styles.section}>
            <h2 className={styles.sectionTitle}>1. Agreement to Terms</h2>
            <p>By accessing or using the services provided by <strong>DRAS</strong> ("we," "us," or "our") through our website and platform (the "Service"), you agree to be bound by these Terms of Service ("Terms"). If you do not agree to these Terms, you may not access or use the Service.</p>
          </section>

          <section id="section2" ref={el => sectionRefs.current['section2'] = el} className={styles.section}>
            <h2 className={styles.sectionTitle}>2. User Accounts</h2>
            <p><strong>2.1 Eligibility:</strong> You must be at least 18 years old to create an account and use the Service.</p>
            <p><strong>2.2 Account Responsibilities:</strong> You are responsible for maintaining the confidentiality of your account password and for all activities that occur under your account. You agree to provide accurate, current, and complete information during the registration process and to update such information to keep it accurate.</p>
            <p><strong>2.3 Account Security:</strong> You must notify us immediately of any unauthorized use of your account or any other breach of security.</p>
          </section>

          <section id="section3" ref={el => sectionRefs.current['section3'] = el} className={styles.section}>
            <h2 className={styles.sectionTitle}>3. Use of the Service</h2>
            <p>You agree not to use the Service for any unlawful purpose or to engage in any of the following prohibited activities:</p>
            <ul className={styles.list}>
              <li>Posting false, inaccurate, misleading, defamatory, or libelous content.</li>
              <li>Distributing viruses or any other technologies that may harm the Service or the interests or property of users.</li>
              <li>Using any robot, spider, scraper, or other automated means to access our Service for any purpose without our express written permission.</li>
              <li>Infringing upon the intellectual property rights of others or of <strong>DRAS</strong>.</li>
              <li>Violating any applicable laws or regulations.</li>
            </ul>
          </section>

          <section id="section4" ref={el => sectionRefs.current['section4'] = el} className={styles.section}>
            <h2 className={styles.sectionTitle}>4. User-Generated Content</h2>
            <p><strong>4.1 Your Content:</strong> Users may post content, including property listings, photographs, descriptions, and comments ("User Content"). You are solely responsible for the User Content you post.</p>
            <p><strong>4.2 License Grant:</strong> By posting User Content, you grant <strong>DRAS</strong> a worldwide, non-exclusive, royalty-free, transferable license to use, reproduce, display, distribute, and prepare derivative works of your User Content in connection with providing the Service.</p>
            <p><strong>4.3 Our Rights:</strong> We reserve the right, but are not obligated, to remove or modify User Content for any reason, including User Content that we believe violates these Terms or our policies.</p>
          </section>

          <section id="section5" ref={el => sectionRefs.current['section5'] = el} className={styles.section}>
            <h2 className={styles.sectionTitle}>5. Intellectual Property</h2>
            <p>The Service and its original content (excluding User Content), features, and functionality are and will remain the exclusive property of <strong>DRAS</strong> and its licensors. Our trademarks and trade dress may not be used in connection with any product or service without our prior written consent.</p>
          </section>

          <section id="section6" ref={el => sectionRefs.current['section6'] = el} className={styles.section}>
            <h2 className={styles.sectionTitle}>6. Disclaimers</h2>
            <p>The Service is provided on an "AS IS" and "AS AVAILABLE" basis. We make no representations or warranties of any kind, express or implied, regarding the operation of our Service or the information, content, or materials included therein.</p>
            <p>We do not warrant the accuracy, completeness, legality, or safety of any property listing, user communication, or other content on the Service. All users are encouraged to perform their own due diligence.</p>
          </section>

          <section id="section7" ref={el => sectionRefs.current['section7'] = el} className={styles.section}>
            <h2 className={styles.sectionTitle}>7. Limitation of Liability</h2>
            <p>In no event shall <strong>DRAS</strong>, nor its directors, employees, partners, or agents, be liable for any indirect, incidental, special, consequential, or punitive damages, including without limitation, loss of profits, data, use, goodwill, or other intangible losses, resulting from your access to or use of or inability to access or use the Service.</p>
          </section>

          <section id="section8" ref={el => sectionRefs.current['section8'] = el} className={styles.section}>
            <h2 className={styles.sectionTitle}>8. Termination</h2>
            <p>We may terminate or suspend your account and bar access to the Service immediately, without prior notice or liability, for any reason whatsoever, including without limitation if you breach the Terms. You may terminate your account at any time by contacting us or through your account settings.</p>
          </section>

          <section id="section9" ref={el => sectionRefs.current['section9'] = el} className={styles.section}>
            <h2 className={styles.sectionTitle}>9. Governing Law</h2>
            <p>These Terms shall be governed and construed in accordance with the laws of <strong>Greece</strong>, without regard to its conflict of law provisions. Any legal action or proceeding arising under these Terms will be brought exclusively in the courts located in <strong>Athens, Greece</strong>.</p>
          </section>

          <section id="section10" ref={el => sectionRefs.current['section10'] = el} className={styles.section}>
            <h2 className={styles.sectionTitle}>10. Changes to Terms</h2>
            <p>We reserve the right, at our sole discretion, to modify or replace these Terms at any time. If a revision is material, we will provide at least 30 days' notice prior to any new terms taking effect. What constitutes a material change will be determined at our sole discretion.</p>
          </section>

          <section id="section11" ref={el => sectionRefs.current['section11'] = el} className={styles.section}>
            <h2 className={styles.sectionTitle}>11. Contact Us</h2>
            <p>
              Have questions about our Terms and Conditions? Reach out via our <Link to="/contact" className={styles.link}>Contact Form</Link> or email us at <a href="mailto:realestate2025project@gmail.com" className={styles.link}>realestate2025project@gmail.com</a>.
            </p>
          </section>
        </main>
      </div>

      <footer className={styles.footer}>
        <p>DRAS · Helping people find the perfect place to live since 2025.</p>
      </footer>
    </div>
  );
}
