import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  MapPin, 
  Phone, 
  Mail, 
  Clock, 
  Send, 
  CheckCircle2, 
  AlertCircle, 
  MessageSquare,
  HelpCircle,
  Building
} from 'lucide-react';
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
      setError(err.message || 'Failed to send message. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className={styles.pageContainer}>
      {/* Header Section */}
      <header className={styles.header}>
        <div className={styles.headerBadge}>
          <MessageSquare size={16} className={styles.badgeIcon} />
          <span>Contact DRAS</span>
        </div>
        <h1 className={styles.titleText}>
          Get in Touch <span className={styles.gradientText}>with Us</span>
        </h1>
        <p className={styles.leadText}>
          Have questions about the platform, feedback on listing price estimates, or need assistance
          with your account? Send us a message and we will get back to you.
        </p>
      </header>

      {/* Main Grid: Contact Channels + Form */}
      <div className={styles.gridContainer}>
        {/* Contact Information Hub */}
        <div className={`${styles.card} ${styles.contactInfoCard}`}>
          <div className={styles.cardHeader}>
            <h2 className={styles.sectionTitle}>Contact Information</h2>
            <p className={styles.sectionSubtitle}>
              Direct contact details for platform inquiries and technical support.
            </p>
          </div>

          <div className={styles.contactList}>
            <div className={styles.contactItem}>
              <div className={`${styles.iconContainer} ${styles.iconBlue}`}>
                <MapPin size={22} />
              </div>
              <div className={styles.itemContent}>
                <span className={styles.itemLabel}>Location</span>
                <address className={styles.contactAddress}>
                  Athens, Greece
                </address>
              </div>
            </div>

            <div className={styles.contactItem}>
              <div className={`${styles.iconContainer} ${styles.iconGreen}`}>
                <Phone size={22} />
              </div>
              <div className={styles.itemContent}>
                <span className={styles.itemLabel}>Telephone</span>
                <a href="tel:+1234567890" className={styles.contactLink}>
                  +1 (234) 567-890
                </a>
              </div>
            </div>

            <div className={styles.contactItem}>
              <div className={`${styles.iconContainer} ${styles.iconPurple}`}>
                <Mail size={22} />
              </div>
              <div className={styles.itemContent}>
                <span className={styles.itemLabel}>Email</span>
                <a href="mailto:dras.system@gmail.com" className={styles.contactLink}>
                  dras.system@gmail.com
                </a>
              </div>
            </div>

            <div className={styles.contactItem}>
              <div className={`${styles.iconContainer} ${styles.iconAmber}`}>
                <Clock size={22} />
              </div>
              <div className={styles.itemContent}>
                <span className={styles.itemLabel}>Availability</span>
                <div className={styles.hoursList}>
                  <p><span>Mon - Fri:</span> 9:00 AM - 6:00 PM (EET)</p>
                  <p><span>Saturday:</span> 10:00 AM - 4:00 PM (EET)</p>
                  <p><span>Sunday:</span> Closed</p>
                </div>
              </div>
            </div>
          </div>

          {/* Simple support note */}
          <div className={styles.commitmentBox}>
            <CheckCircle2 size={20} className={styles.commitmentIcon} />
            <div>
              <strong>Support Inquiries:</strong>
              <p>We review incoming messages and reply via email to the address you provide.</p>
            </div>
          </div>
        </div>

        {/* Contact Form Card */}
        <div className={`${styles.card} ${styles.formCard}`}>
          <div className={styles.cardHeader}>
            <h2 className={styles.sectionTitle}>Send Us a Message</h2>
            <p className={styles.sectionSubtitle}>
              Fill out the form below and we will follow up with you.
            </p>
          </div>

          {success && (
            <div className={styles.alertSuccess} role="alert">
              <CheckCircle2 size={22} className={styles.alertIcon} />
              <div>
                <strong>Message Sent Successfully</strong>
                <p>Thank you for contacting us. We will reply to your email address.</p>
              </div>
            </div>
          )}

          {error && (
            <div className={styles.alertError} role="alert">
              <AlertCircle size={22} className={styles.alertIcon} />
              <div>
                <strong>Failed to Send Message</strong>
                <p>{error}</p>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} className={styles.form} noValidate={false}>
            <div className={styles.inputGroup}>
              <label htmlFor="name" className={styles.label}>
                Full Name <span className={styles.requiredMark}>*</span>
              </label>
              <input
                type="text"
                id="name"
                required
                maxLength={100}
                value={formData.name}
                onChange={e => setFormData({ ...formData, name: e.target.value })}
                className={styles.input}
                placeholder="e.g. John Doe"
              />
            </div>

            <div className={styles.inputGroup}>
              <label htmlFor="email" className={styles.label}>
                Email Address <span className={styles.requiredMark}>*</span>
              </label>
              <input
                type="email"
                id="email"
                required
                maxLength={254}
                value={formData.email}
                onChange={e => setFormData({ ...formData, email: e.target.value })}
                className={styles.input}
                placeholder="name@example.com"
              />
            </div>

            <div className={styles.inputGroup}>
              <label htmlFor="subject" className={styles.label}>
                Subject <span className={styles.requiredMark}>*</span>
              </label>
              <input
                type="text"
                id="subject"
                required
                maxLength={150}
                value={formData.subject}
                onChange={e => setFormData({ ...formData, subject: e.target.value })}
                className={styles.input}
                placeholder="e.g. Property Listing Question or Rental Inquiry"
              />
            </div>

            <div className={styles.inputGroup}>
              <label htmlFor="message" className={styles.label}>
                Message <span className={styles.requiredMark}>*</span>
              </label>
              <textarea
                id="message"
                rows={5}
                required
                maxLength={2000}
                value={formData.message}
                onChange={e => setFormData({ ...formData, message: e.target.value })}
                className={styles.textarea}
                placeholder="Your message here..."
              />
              <span className={styles.charCount}>{formData.message.length} / 2000 characters</span>
            </div>

            <div className={styles.submitWrapper}>
              <button 
                type="submit" 
                className={styles.submitBtn} 
                disabled={isSubmitting}
                aria-busy={isSubmitting}
              >
                <Send size={18} />
                <span>{isSubmitting ? 'Sending Message...' : 'Send Message'}</span>
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Map Section */}
      <section className={styles.mapSection}>
        <div className={styles.mapHeader}>
          <div className={styles.mapHeaderIcon}>
            <Building size={24} />
          </div>
          <div>
            <h2 className={styles.mapTitle}>Location</h2>
            <p className={styles.mapSubtitle}>Athens, Greece</p>
          </div>
        </div>
        <div className={styles.mapContainer}>
          <iframe
            src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3144.381488219614!2d23.72583861532298!3d37.98380997972232!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x14a1bd1f0b0a8d5b%3A0x33b47f0d0d0d0d0d!2sAcropolis%20of%20Athens!5e0!3m2!1sen!2sgr!4v1663332221111!5m2!1sen!2sgr"
            width="100%"
            height="420"
            style={{ border: 0 }}
            allowFullScreen
            sandbox="allow-scripts allow-same-origin allow-popups"
            loading="lazy"
            referrerPolicy="no-referrer-when-downgrade"
            title="DRAS Office Location"
          />
        </div>
      </section>

      {/* FAQ Banner */}
      <section className={styles.faqBanner}>
        <div className={styles.faqIconBox}>
          <HelpCircle size={28} />
        </div>
        <div className={styles.faqContent}>
          <h3 className={styles.faqTitle}>Frequently Asked Questions</h3>
          <p className={styles.faqText}>
            Find answers to common questions about creating an owner profile, listing an apartment, or submitting tenant applications on our homepage.
          </p>
        </div>
        <Link to="/" className={styles.faqBtn}>
          View FAQs
        </Link>
      </section>
    </div>
  );
}
