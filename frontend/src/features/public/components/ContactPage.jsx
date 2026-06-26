import React, { useState } from 'react';
import apiClient from '../../../shared/api/client';
import styles from './ContactPage.module.css';

export default function ContactPage() {
  const [formData, setFormData] = useState({ name: '', email: '', subject: '', message: '' });
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
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
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h1 className={styles.title}>Contact Us</h1>
        {success && <div className={styles.success}>Your message has been sent successfully!</div>}
        {error && <div className={styles.error}>{error}</div>}
        
        <form onSubmit={handleSubmit}>
          <div className={styles.formGroup}>
            <label htmlFor="contactName" className={styles.label}>Name</label>
            <input id="contactName" type="text" required value={formData.name} onChange={e => setFormData({...formData, name: e.target.value})} className={styles.input} />
          </div>
          <div className={styles.formGroup}>
            <label htmlFor="contactEmail" className={styles.label}>Email Address</label>
            <input id="contactEmail" type="email" required value={formData.email} onChange={e => setFormData({...formData, email: e.target.value})} className={styles.input} />
          </div>
          <div className={styles.formGroup}>
            <label htmlFor="contactSubject" className={styles.label}>Subject</label>
            <input id="contactSubject" type="text" required value={formData.subject} onChange={e => setFormData({...formData, subject: e.target.value})} className={styles.input} />
          </div>
          <div className={styles.formGroupLarge}>
            <label htmlFor="contactMessage" className={styles.label}>Message</label>
            <textarea id="contactMessage" rows="5" required value={formData.message} onChange={e => setFormData({...formData, message: e.target.value})} className={styles.input}></textarea>
          </div>
          <button type="submit" className={styles.btn}>Send Message</button>
        </form>
      </div>
    </div>
  );
}
