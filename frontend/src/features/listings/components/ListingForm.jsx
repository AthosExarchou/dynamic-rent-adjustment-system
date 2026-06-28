import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth';
import apiClient from '../../../shared/api/client';
import { LISTING_CONSTRAINTS } from '../models/listing';
import styles from './ListingForm.module.css';

export default function ListingForm() {
  const { roles } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    title: '', subtitle: '', description: '', price: '', address: '',
    sizeM2: '', propertyType: 'APARTMENT', rentalDuration: 'LONG_TERM',
    floor: '', yearBuilt: '', bedrooms: '', bathrooms: '',
    firstName: '', lastName: '', phoneNumber: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const isOwner = roles.includes('OWNER');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    // Prepare parameters for API matching MultiPart / URLParams in MVC controller
    const params = new URLSearchParams({
      title: formData.title,
      subtitle: formData.subtitle,
      description: formData.description,
      price: formData.price,
      pricePerM2: Math.round(Number(formData.sizeM2) > 0 ? Number(formData.price) / Number(formData.sizeM2) : 0),
      address: formData.address,
      sizeM2: formData.sizeM2,
      propertyType: formData.propertyType,
      rentalDuration: formData.rentalDuration,
      floor: formData.floor,
      yearBuilt: formData.yearBuilt,
      bedrooms: formData.bedrooms,
      bathrooms: formData.bathrooms
    });

    if (!isOwner) {
      params.append('firstName', formData.firstName);
      params.append('lastName', formData.lastName);
      params.append('phoneNumber', formData.phoneNumber);
    }

    try {
      await apiClient('/listings/new', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
      });
      navigate('/listings');
    } catch (err) {
      setError('Failed to submit listing. Please correct form validation errors.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h2 className={styles.title}>Submit Property Listing</h2>
        {error && <div className={styles.error}>{error}</div>}
        
        <form onSubmit={handleSubmit}>
          <div className={styles.formGroup}>
            <label htmlFor="title" className={styles.label}>Title</label>
            <input id="title" type="text" required value={formData.title}
                   onChange={e => setFormData(
                       {...formData, title: e.target.value})} className={styles.input} placeholder="Modern 2BR near Downtown" />
          </div>

          <div className={styles.formGroup}>
            <label htmlFor="subtitle" className={styles.label}>Subtitle</label>
            <input id="subtitle" type="text" value={formData.subtitle}
                   onChange={e => setFormData(
                       {...formData, subtitle: e.target.value})} className={styles.input} placeholder="Optional subtitle details" />
          </div>

          <div className={styles.formGroup}>
            <label htmlFor="description" className={styles.label}>Description</label>
            <textarea id="description" rows="4" required value={formData.description}
                      onChange={e => setFormData(
                          {...formData, description: e.target.value})} className={styles.input}></textarea>
          </div>

          <div className={styles.grid2}>
            <div>
              <label htmlFor="price" className={styles.label}>Rent / Month (€)</label>
              <input id="price" type="number" required min={LISTING_CONSTRAINTS.PRICE_MIN}
                     max={LISTING_CONSTRAINTS.PRICE_MAX} value={formData.price}
                     onChange={e => setFormData(
                         {...formData, price: e.target.value})} className={styles.input} />
            </div>
            <div>
              <label htmlFor="sizeM2" className={styles.label}>Area Size (m²)</label>
              <input id="sizeM2" type="number" required min={LISTING_CONSTRAINTS.SIZE_M2_MIN}
                     max={LISTING_CONSTRAINTS.SIZE_M2_MAX} value={formData.sizeM2}
                     onChange={e => setFormData(
                         {...formData, sizeM2: e.target.value})} className={styles.input} />
            </div>
          </div>

          <div className={styles.formGroup}>
            <label htmlFor="address" className={styles.label}>Address</label>
            <input id="address" type="text" required value={formData.address}
                   onChange={e => setFormData(
                       {...formData, address: e.target.value})} className={styles.input} placeholder="123 Main Street, Athens" />
          </div>

          <div className={styles.grid3}>
            <div>
              <label htmlFor="propertyType" className={styles.label}>Property Type</label>
              <select id="propertyType" value={formData.propertyType}
                      onChange={e => setFormData(
                          {...formData, propertyType: e.target.value})} className={styles.input}>
                <option value="APARTMENT">APARTMENT</option>
                <option value="HOUSE">HOUSE</option>
                <option value="STUDIO">STUDIO</option>
                <option value="MAISONETTE">MAISONETTE</option>
                <option value="LOFT">LOFT</option>
                <option value="VILLA">VILLA</option>
                <option value="OTHER">OTHER</option>
              </select>
            </div>
            <div>
              <label htmlFor="rentalDuration" className={styles.label}>Lease Term</label>
              <select id="rentalDuration" value={formData.rentalDuration}
                      onChange={e => setFormData(
                          {...formData, rentalDuration: e.target.value})} className={styles.input}>
                <option value="LONG_TERM">LONG_TERM</option>
                <option value="SHORT_TERM">SHORT_TERM</option>
                <option value="INDEFINITE">INDEFINITE</option>
                <option value="FIXED_TERM">FIXED_TERM</option>
                <option value="OTHER">OTHER</option>
              </select>
            </div>
            <div>
              <label htmlFor="floor" className={styles.label}>Floor</label>
              <input id="floor" type="number" required min={LISTING_CONSTRAINTS.FLOOR_MIN}
                     max={LISTING_CONSTRAINTS.FLOOR_MAX} value={formData.floor}
                     onChange={e => setFormData(
                         {...formData, floor: e.target.value})} className={styles.input} />
            </div>
          </div>

          <div className={styles.grid3Last}>
            <div>
              <label htmlFor="bedrooms" className={styles.label}>Bedrooms</label>
              <input id="bedrooms" type="number" required min={LISTING_CONSTRAINTS.BEDROOMS_MIN}
                     max={LISTING_CONSTRAINTS.BEDROOMS_MAX} value={formData.bedrooms}
                     onChange={e => setFormData(
                         {...formData, bedrooms: e.target.value})} className={styles.input} />
            </div>
            <div>
              <label htmlFor="bathrooms" className={styles.label}>Bathrooms</label>
              <input id="bathrooms" type="number" required min={LISTING_CONSTRAINTS.BATHROOMS_MIN}
                     max={LISTING_CONSTRAINTS.BATHROOMS_MAX} value={formData.bathrooms}
                     onChange={e => setFormData(
                         {...formData, bathrooms: e.target.value})} className={styles.input} />
            </div>
            <div>
              <label htmlFor="yearBuilt" className={styles.label}>Year Built</label>
              <input id="yearBuilt" type="number" required min={1900}
                     max={new Date().getFullYear()} value={formData.yearBuilt}
                     onChange={e => setFormData(
                         {...formData, yearBuilt: e.target.value})} className={styles.input} />
            </div>
          </div>

          {!isOwner && (
            <div className={styles.ownerSection}>
              <h4 className={styles.ownerTitle}>Register Owner Profile</h4>
              <div className={styles.grid2}>
                <div>
                  <label htmlFor="firstName" className={styles.label}>First Name</label>
                  <input id="firstName" type="text" required value={formData.firstName}
                         onChange={e => setFormData(
                             {...formData, firstName: e.target.value})} className={styles.input} />
                </div>
                <div>
                  <label htmlFor="lastName" className={styles.label}>Last Name</label>
                  <input id="lastName" type="text" required value={formData.lastName}
                         onChange={e => setFormData(
                             {...formData, lastName: e.target.value})} className={styles.input} />
                </div>
              </div>
              <div>
                <label htmlFor="phoneNumber" className={styles.label}>Phone Number</label>
                <input id="phoneNumber" type="text" required value={formData.phoneNumber}
                       onChange={e => setFormData(
                           {...formData, phoneNumber: e.target.value})} className={styles.input} placeholder="+30 690 1234567" />
              </div>
            </div>
          )}

          <button type="submit" disabled={loading} className={styles.submitBtn}>
            {loading ? 'Submitting...' : 'Submit Property'}
          </button>
        </form>
      </div>
    </div>
  );
}
