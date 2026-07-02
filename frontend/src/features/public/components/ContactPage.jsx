import React, { useState } from 'react';
import { MapPin, Phone, Mail, Clock, Send } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import styles from './ContactPage.module.css';

export default function ContactPage() {
  const [formData, setFormData] = useState({ name: '', email: '', subject: '', message: '' });
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await apiClient('/contact/send', {
        method: 'POST',
        body: formData
      });
      setSuccess(true);
      setError('');
      setFormData({ name: '', email: '', subject: '', message: '' });
    } catch (err) {
      setError('Failed to send message. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className={styles.pageContainer}>
      <div className={styles.header}>
        <h1 className={styles.titleText}>Get in Touch</h1>
        <p className={styles.leadText}>
          We're here to help with all your real estate needs - property inquiries, valuations,
          or just advice about the market. Reach out and our team will get back to you promptly.
        </p>
      </div>

      <div className={styles.gridContainer}>
        {/* Contact Info Card */}
        <div className={`${styles.card} ${styles.contactInfoCard}`}>
          <h3 className={styles.sectionTitle}>Contact Information</h3>
          
          <div className={styles.contactItem}>
            <MapPin className={styles.contactIcon} size={24} />
            <div>
              <strong>Office Address:</strong>
              <address className={styles.contactText}>
                123 DRAS Ave,<br />
                Property City, PC 12345<br />
                Country
              </address>
            </div>
          </div>

          <div className={styles.contactItem}>
            <Phone className={styles.contactIcon} size={24} />
            <div>
              <strong>Phone:</strong><br />
              <a href="tel:+1234567890" className={styles.contactLink}>+1 (234) 567-890</a>
            </div>
          </div>

          <div className={styles.contactItem}>
            <Mail className={styles.contactIcon} size={24} />
            <div>
              <strong>General Inquiries:</strong><br />
              <a href="mailto:realestate2025project@gmail.com"
                 className={styles.contactLink}>realestate2025project@gmail.com</a>
            </div>
          </div>

          <div className={styles.contactItem}>
            <Clock className={styles.contactIcon} size={24} />
            <div>
              <strong>Business Hours:</strong><br />
              <p className={styles.contactText}>Mon - Fri: 9:00 AM - 6:00 PM</p>
              <p className={styles.contactText}>Saturday: 10:00 AM - 4:00 PM</p>
              <p className={styles.contactText}>Sunday: By Appointment Only</p>
            </div>
          </div>
        </div>

        {/* Contact Form Card */}
        <div className={`${styles.card} ${styles.formCard}`}>
          <h3 className={styles.sectionTitle}>Send Us a Message</h3>
          
          {success && <div className={styles.alertSuccess}>Your message has been sent successfully!</div>}
          {error && <div className={styles.alertError}>{error}</div>}

          <form onSubmit={handleSubmit} className={styles.form}>
            <div className={styles.inputGroup}>
              <label htmlFor="name" className={styles.label}>Full Name</label>
              <input type="text" id="name" required maxLength={100} value={formData.name}
                     onChange={e => setFormData(
                         {...formData, name: e.target.value})} className={styles.input} placeholder="John Doe" />
            </div>

            <div className={styles.inputGroup}>
              <label htmlFor="email" className={styles.label}>Email Address</label>
              <input type="email" id="email" required maxLength={254} value={formData.email}
                     onChange={e => setFormData(
                         {...formData, email: e.target.value})} className={styles.input} placeholder="name@example.com" />
            </div>

            <div className={styles.inputGroup}>
              <label htmlFor="subject" className={styles.label}>Subject</label>
              <input type="text" id="subject" required maxLength={150} value={formData.subject}
                     onChange={e => setFormData(
                         {...formData, subject: e.target.value})} className={styles.input} placeholder="e.g., Property Inquiry" />
            </div>

            <div className={styles.inputGroup}>
              <label htmlFor="message" className={styles.label}>Message</label>
              <textarea id="message" rows="5" required maxLength={2000} value={formData.message}
                        onChange={e => setFormData(
                            {...formData, message: e.target.value})} className={styles.textarea} placeholder="Your message here..."></textarea>
            </div>

            <div className={styles.submitWrapper}>
              <button type="submit" className={styles.submitBtn} disabled={isSubmitting}>
                <Send size={20} />
                <span>{isSubmitting ? 'Sending...' : 'Send Message'}</span>
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Map Section */}
      <div className={styles.mapSection}>
        <h3 className={styles.mapTitle}>Our Location</h3>
        <div className={styles.mapContainer}>
          <iframe
            src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3144.381488219614!2d23.72583861532298!3d37.98380997972232!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x14a1bd1f0b0a8d5b%3A0x33b47f0d0d0d0d0d!2sAcropolis%20of%20Athens!5e0!3m2!1sen!2sgr!4v1663332221111!5m2!1sen!2sgr"
            width="100%"
            height="450"
            style={{ border: 0 }}
            allowFullScreen
            sandbox="allow-scripts allow-same-origin allow-popups"
            loading="lazy"
            referrerPolicy="no-referrer-when-downgrade"
            title="Google Maps Location"
          ></iframe>
        </div>
      </div>
    </div>
  );
}
