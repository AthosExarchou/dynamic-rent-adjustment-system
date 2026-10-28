import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { AlertCircle, Lock, UploadCloud, X, Check } from 'lucide-react';
import { useAuth } from '../../auth';
import apiClient from '../../../shared/api/client';
import { LISTING_CONSTRAINTS } from '../models/listing';
import { OWNER_CONSTRAINTS } from '../../owners/models/owner';
import PriceSuggestionWidget from './PriceSuggestionWidget';
import { getImageUrl } from '../../../shared/utils/imageUtils';
import styles from './ListingForm.module.css';

export default function ListingForm() {
  const { listingId } = useParams();
  const isEditMode = Boolean(listingId);
  const { user, roles, refreshUser } = useAuth();
  const navigate = useNavigate();
  const fileInputRef = useRef(null);
  const [formData, setFormData] = useState({
    title: '', subtitle: '', description: '', price: '', address: '',
    sizeM2: '', propertyType: 'APARTMENT', rentalDuration: 'LONG_TERM',
    floor: '', yearBuilt: '', bedrooms: '', bathrooms: '',
    firstName: '', lastName: '', phoneNumber: ''
  });
  const [images, setImages] = useState([]);
  const [isDragging, setIsDragging] = useState(false);
  const maxImages = 10;
  const [error, setError] = useState('');
  const [errorDetails, setErrorDetails] = useState([]);
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(isEditMode);
  const [unauthorized, setUnauthorized] = useState(false);
  const [isRented, setIsRented] = useState(false);
  const [currentListingStatus, setCurrentListingStatus] = useState(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [showConfirmModal, setShowConfirmModal] = useState(false);

  const isOwner = roles.includes('OWNER');
  const willResetToPending = isEditMode && !isRented && !roles.includes('ADMIN') && (currentListingStatus === 'APPROVED' || currentListingStatus === 'REJECTED');

  useEffect(() => {
    if (!isEditMode) return;
    let active = true;
    setInitialLoading(true);

    apiClient(`/listings/${listingId}`)
      .then((data) => {
        if (!active) return;
        if (data) {
          const isUserAdmin = roles.includes('ADMIN');
          const isListingOwner = user && data.owner && data.owner.userId === user.id;

          if (!isUserAdmin && !isListingOwner) {
            setUnauthorized(true);
            setError('You do not have permission to edit this listing because you are not its owner.');
            return;
          }

          const isListingRented = Boolean(data.rented || data.status === 'RENTED');
          setIsRented(isListingRented);

          if (isListingRented) {
            setError('This listing has an active rental and cannot be edited.');
            return;
          }

          setCurrentListingStatus(data.status);
          setRejectionReason(data.rejectionReason || '');
          setFormData({
            title: data.title || '',
            subtitle: data.subtitle || '',
            description: data.description || '',
            price: data.price != null ? String(data.price) : '',
            address: data.address || '',
            sizeM2: data.sizeM2 != null ? String(data.sizeM2) : '',
            propertyType: data.propertyType || 'APARTMENT',
            rentalDuration: data.rentalDuration || 'LONG_TERM',
            floor: data.floor != null ? String(data.floor) : '',
            yearBuilt: data.yearBuilt != null ? String(data.yearBuilt) : '',
            bedrooms: data.bedrooms != null ? String(data.bedrooms) : '',
            bathrooms: data.bathrooms != null ? String(data.bathrooms) : '',
            firstName: '',
            lastName: '',
            phoneNumber: ''
          });
          if (Array.isArray(data.images) && data.images.length > 0) {
            setImages(data.images.map((path, idx) => ({
              id: `existing-${idx}-${path}`,
              type: 'existing',
              path: path,
              preview: getImageUrl(path)
            })));
          }
        }
      })
      .catch((err) => {
        if (!active) return;
        setError(err.message || 'Failed to load property details for editing.');
      })
      .finally(() => {
        if (active) setInitialLoading(false);
      });

    return () => { active = false; };
  }, [listingId, isEditMode]);

  /** Clamps a numeric formData field to [min, max] when the input loses focus. */
  const clampField = (field, min, max) => () => {
    const num = Number(formData[field]);
    if (formData[field] === '' || isNaN(num)) return;
    const clamped = String(Math.min(Math.max(num, min), max));
    if (clamped !== formData[field]) setFormData(prev => ({ ...prev, [field]: clamped }));
  };

  const handleAddFiles = (newFiles) => {
    if (!newFiles || !newFiles.length) return;
    const validTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
    const MAX_FILE_SIZE = 20 * 1024 * 1024; // 20 MB

    const filesArray = Array.from(newFiles);
    const oversized = filesArray.filter(f => f.size > MAX_FILE_SIZE);
    if (oversized.length > 0) {
      setError(`Some files exceed the 20MB limit: ${oversized.map(f => f.name).join(', ')}`);
      return;
    }

    const incoming = filesArray.filter(f => validTypes.includes(f.type));
    if (!incoming.length) {
      setError('Please select valid image files (JPG, PNG, WebP, or GIF).');
      return;
    }

    setImages(prev => {
      const remaining = maxImages - prev.length;
      if (remaining <= 0) return prev;
      const toAdd = incoming.slice(0, remaining).map(file => ({
        id: `file-${Date.now()}-${Math.random()}`,
        type: 'file',
        file,
        preview: URL.createObjectURL(file),
        name: file.name
      }));
      return [...prev, ...toAdd];
    });
  };

  const handleRemoveImage = (indexToRemove) => {
    setImages(prev => {
      const item = prev[indexToRemove];
      if (item && item.type === 'file' && item.preview) {
        URL.revokeObjectURL(item.preview);
      }
      return prev.filter((_, idx) => idx !== indexToRemove);
    });
  };

  // Revoke object URLs on unmount to prevent memory leaks
  useEffect(() => {
    return () => {
      images.forEach(img => {
        if (img.type === 'file' && img.preview) {
          URL.revokeObjectURL(img.preview);
        }
      });
    };
  }, [images]);

  const executeSubmit = async () => {
    setLoading(true);
    setError('');
    setErrorDetails([]);

    const formDataPayload = new FormData();
    formDataPayload.append('title', formData.title);
    formDataPayload.append('subtitle', formData.subtitle || '');
    formDataPayload.append('description', formData.description || '');
    formDataPayload.append('price', formData.price);
    formDataPayload.append(
      'pricePerM2',
      String(Math.round(Number(formData.sizeM2) > 0 ? Number(formData.price) / Number(formData.sizeM2) : 0))
    );
    formDataPayload.append('address', formData.address);
    formDataPayload.append('sizeM2', formData.sizeM2);
    formDataPayload.append('propertyType', formData.propertyType);
    formDataPayload.append('rentalDuration', formData.rentalDuration);
    formDataPayload.append('floor', formData.floor != null ? formData.floor : '');
    formDataPayload.append('yearBuilt', formData.yearBuilt != null ? formData.yearBuilt : '');
    formDataPayload.append('bedrooms', formData.bedrooms != null ? formData.bedrooms : '');
    formDataPayload.append('bathrooms', formData.bathrooms != null ? formData.bathrooms : '');

    if (!isOwner && !isEditMode) {
      formDataPayload.append('firstName', formData.firstName || '');
      formDataPayload.append('lastName', formData.lastName || '');
      formDataPayload.append('phoneNumber', formData.phoneNumber || '');
    }

    // Append retained existing images and new files
    images.forEach(img => {
      if (img.type === 'existing' && img.path) {
        formDataPayload.append('images', img.path);
      } else if (img.type === 'file' && img.file) {
        formDataPayload.append('imageFiles', img.file);
      }
    });

    const endpoint = isEditMode ? `/listings/edit/${listingId}` : '/listings/new';

    try {
      await apiClient(endpoint, {
        method: 'POST',
        body: formDataPayload
      });
      if (refreshUser && !isEditMode) {
        await refreshUser();
      }
      if (roles.includes('ADMIN')) {
        navigate(`/listings/${listingId}`);
      } else {
        navigate('/my-listings');
      }
    } catch (err) {
      setError(err.message || (isEditMode ? 'Failed to update listing.' : 'Failed to submit listing. Please correct form validation errors.'));
      if (Array.isArray(err.details) && err.details.length > 0) {
        setErrorDetails(err.details);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setError('');
    setErrorDetails([]);

    if (willResetToPending) {
      setShowConfirmModal(true);
    } else {
      executeSubmit();
    }
  };

  if (unauthorized || (!isEditMode && roles.includes('ADMIN'))) {
    return (
      <div className={styles.container}>
        <div className={styles.card}>
          <h2 className={styles.title} style={{ color: '#dc3545' }}>Access Denied</h2>
          <p style={{ textAlign: 'center', color: '#666', padding: '1.5rem 0' }}>
            {error || 'Administrators are not permitted to create listings. You can only edit existing listings.'}
          </p>
        </div>
      </div>
    );
  }

  if (isRented) {
    return (
      <div className={styles.container}>
        <div className={styles.card}>
          <h2 className={styles.title} style={{ color: '#0d6efd', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Lock size={26} /> Property Locked
          </h2>
          <div className={styles.noticeBanner} style={{ backgroundColor: '#eff6ff', borderColor: '#bfdbfe', color: '#1e40af' }}>
            <Lock className={styles.noticeIcon} size={22} style={{ color: '#2563eb' }} />
            <div>
              <strong>Active Rental Agreement:</strong> This listing is currently rented and cannot be edited until the rental agreement concludes.
            </div>
          </div>
          <div style={{ textAlign: 'center', marginTop: '1.5rem' }}>
            <Link 
              to={roles.includes('ADMIN') ? `/listings/${listingId}` : '/my-listings'} 
              className={styles.modalCancelBtn} 
              style={{ textDecoration: 'none', display: 'inline-block' }}
            >
              {roles.includes('ADMIN') ? 'Back to Property Details' : 'Back to My Properties'}
            </Link>
          </div>
        </div>
      </div>
    );
  }

  if (initialLoading) {
    return (
      <div className={styles.container}>
        <div className={styles.card}>
          <p style={{ textAlign: 'center', padding: '2rem' }}>Loading property details...</p>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <div className={`${styles.card} ${styles.cardHover}`}>
        <h2 className={styles.title}>{isEditMode ? 'Edit Property Listing' : 'Submit Property Listing'}</h2>
        {currentListingStatus === 'REJECTED' && rejectionReason && (
          <div className={styles.rejectionBanner}>
            <AlertCircle className={styles.rejectionIcon} size={22} />
            <div>
              <strong>Administrator Feedback:</strong>
              <p className={styles.rejectionText}>{rejectionReason}</p>
            </div>
          </div>
        )}
        {willResetToPending && (
          <div className={styles.noticeBanner}>
            <AlertCircle className={styles.noticeIcon} size={22} />
            <div>
              <strong>Important Notice:</strong> This listing is currently <strong>{currentListingStatus}</strong>.
              Saving your changes will reset its status to <strong>PENDING</strong> and remove it from public search results until an administrator reviews and approves it.
            </div>
          </div>
        )}
        {error && (
          <div className={styles.error}>
            <div>{error}</div>
            {errorDetails && errorDetails.length > 1 && (
              <ul style={{ marginTop: '0.5rem', marginBottom: 0, paddingLeft: '1.25rem' }}>
                {errorDetails.map((detail, idx) => (
                  <li key={idx}>{detail}</li>
                ))}
              </ul>
            )}
          </div>
        )}
        
        <form onSubmit={handleSubmit}>
          <div className={styles.formGroup}>
            <label htmlFor="title" className={styles.label}>Title</label>
            <input id="title" type="text" required maxLength={LISTING_CONSTRAINTS.TITLE_MAX} value={formData.title}
                   onChange={e => setFormData(
                       {...formData, title: e.target.value})} className={styles.input} placeholder="Modern 2BR near Downtown" />
          </div>

          <div className={styles.formGroup}>
            <label htmlFor="subtitle" className={styles.label}>Subtitle</label>
            <input id="subtitle" type="text" maxLength={LISTING_CONSTRAINTS.SUBTITLE_MAX} value={formData.subtitle}
                   onChange={e => setFormData(
                       {...formData, subtitle: e.target.value})} className={styles.input} placeholder="Optional subtitle details" />
          </div>

          <div className={styles.formGroup}>
            <label htmlFor="description" className={styles.label}>Description</label>
            <textarea id="description" rows="4" required maxLength={LISTING_CONSTRAINTS.DESCRIPTION_MAX} value={formData.description}
                      onChange={e => setFormData(
                          {...formData, description: e.target.value})} className={styles.textarea}></textarea>
          </div>

          <div className={styles.grid2}>
            <div>
              <label htmlFor="price" className={styles.label}>Rent / Month (€)</label>
              <input id="price" type="number" required min={LISTING_CONSTRAINTS.PRICE_MIN}
                     max={LISTING_CONSTRAINTS.PRICE_MAX} value={formData.price}
                     onChange={e => setFormData(
                         {...formData, price: e.target.value})}
                     onBlur={clampField('price', LISTING_CONSTRAINTS.PRICE_MIN, LISTING_CONSTRAINTS.PRICE_MAX)}
                     className={styles.input} />
            </div>
            <div>
              <label htmlFor="sizeM2" className={styles.label}>Area Size (m²)</label>
              <input id="sizeM2" type="number" required min={LISTING_CONSTRAINTS.SIZE_M2_MIN}
                     max={LISTING_CONSTRAINTS.SIZE_M2_MAX} value={formData.sizeM2}
                     onChange={e => setFormData(
                         {...formData, sizeM2: e.target.value})}
                     onBlur={clampField('sizeM2', LISTING_CONSTRAINTS.SIZE_M2_MIN, LISTING_CONSTRAINTS.SIZE_M2_MAX)}
                     className={styles.input} />
            </div>
          </div>

          <PriceSuggestionWidget
            formData={formData}
            onApplyPrice={(suggestedPrice) =>
              setFormData(prev => ({ ...prev, price: String(suggestedPrice) }))
            }
          />

          <div className={styles.formGroup}>
            <label htmlFor="address" className={styles.label}>Address</label>
            <input id="address" type="text" required maxLength={LISTING_CONSTRAINTS.ADDRESS_MAX} value={formData.address}
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
                         {...formData, floor: e.target.value})}
                     onBlur={clampField('floor', LISTING_CONSTRAINTS.FLOOR_MIN, LISTING_CONSTRAINTS.FLOOR_MAX)}
                     className={styles.input} />
            </div>
          </div>

          <div className={styles.grid3Last}>
            <div>
              <label htmlFor="bedrooms" className={styles.label}>Bedrooms</label>
              <input id="bedrooms" type="number" required min={LISTING_CONSTRAINTS.BEDROOMS_MIN}
                     max={LISTING_CONSTRAINTS.BEDROOMS_MAX} value={formData.bedrooms}
                     onChange={e => setFormData(
                         {...formData, bedrooms: e.target.value})}
                     onBlur={clampField('bedrooms', LISTING_CONSTRAINTS.BEDROOMS_MIN, LISTING_CONSTRAINTS.BEDROOMS_MAX)}
                     className={styles.input} />
            </div>
            <div>
              <label htmlFor="bathrooms" className={styles.label}>Bathrooms</label>
              <input id="bathrooms" type="number" required min={LISTING_CONSTRAINTS.BATHROOMS_MIN}
                     max={LISTING_CONSTRAINTS.BATHROOMS_MAX} value={formData.bathrooms}
                     onChange={e => setFormData(
                         {...formData, bathrooms: e.target.value})}
                     onBlur={clampField('bathrooms', LISTING_CONSTRAINTS.BATHROOMS_MIN, LISTING_CONSTRAINTS.BATHROOMS_MAX)}
                     className={styles.input} />
            </div>
            <div>
              <label htmlFor="yearBuilt" className={styles.label}>Year Built</label>
              <input id="yearBuilt" type="number" required min={1900}
                     max={new Date().getFullYear()} value={formData.yearBuilt}
                     onChange={e => setFormData(
                         {...formData, yearBuilt: e.target.value})}
                     onBlur={clampField('yearBuilt', 1900, new Date().getFullYear())}
                     className={styles.input} />
            </div>
          </div>

          {!isOwner && !isEditMode && (
            <div className={styles.ownerSection}>
              <h4 className={styles.ownerTitle}>Register Owner Profile</h4>
              <div className={styles.grid2}>
                <div>
                  <label htmlFor="firstName" className={styles.label}>First Name</label>
                  <input id="firstName" type="text" required maxLength={OWNER_CONSTRAINTS.FIRST_NAME_MAX} value={formData.firstName}
                         onChange={e => setFormData(
                             {...formData, firstName: e.target.value})} className={styles.input} />
                </div>
                <div>
                  <label htmlFor="lastName" className={styles.label}>Last Name</label>
                  <input id="lastName" type="text" required maxLength={OWNER_CONSTRAINTS.LAST_NAME_MAX} value={formData.lastName}
                         onChange={e => setFormData(
                             {...formData, lastName: e.target.value})} className={styles.input} />
                </div>
              </div>
              <div>
                <label htmlFor="phoneNumber" className={styles.label}>Phone Number</label>
                <input id="phoneNumber" type="tel" required
                       minLength={OWNER_CONSTRAINTS.PHONE_MIN}
                       maxLength={OWNER_CONSTRAINTS.PHONE_MAX}
                       pattern="^\+?[0-9. ()-]{7,25}$"
                       title="Enter a valid phone number with 7-25 digits."
                       value={formData.phoneNumber}
                       onChange={e => setFormData(
                           {...formData, phoneNumber: e.target.value})} className={styles.input} placeholder="+30 690 1234567" />
              </div>
            </div>
          )}

          <div className={styles.formGroup}>
            <label className={styles.label}>
              Property Images ({images.length}/{maxImages})
            </label>

            {/* Drag & Drop File Upload Area */}
            {images.length < maxImages && (
              <div
                className={`${styles.dropzone} ${isDragging ? styles.dropzoneActive : ''}`}
                onDragOver={(e) => {
                  e.preventDefault();
                  setIsDragging(true);
                }}
                onDragLeave={() => setIsDragging(false)}
                onDrop={(e) => {
                  e.preventDefault();
                  setIsDragging(false);
                  if (e.dataTransfer.files) {
                    handleAddFiles(e.dataTransfer.files);
                  }
                }}
                onClick={() => fileInputRef.current?.click()}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    fileInputRef.current?.click();
                  }
                }}
              >
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={(e) => {
                    if (e.target.files) {
                      handleAddFiles(e.target.files);
                    }
                    e.target.value = '';
                  }}
                  multiple
                  accept="image/jpeg,image/png,image/webp,image/gif"
                  style={{ display: 'none' }}
                />
                <UploadCloud size={36} className={styles.dropzoneIcon} />
                <p className={styles.dropzoneTitle}>
                  Drag & drop property photos here, or <span className={styles.browseLink}>browse</span>
                </p>
                <p className={styles.dropzoneSubtitle}>
                  Supports JPG, PNG, WebP or GIF (maximum {maxImages} photos)
                </p>
              </div>
            )}

            {/* Instant Thumbnail Previews Grid */}
            {images.length > 0 && (
              <div className={styles.previewGrid}>
                {images.map((item, index) => (
                  <div key={item.id || index} className={styles.previewCard}>
                    <img
                      src={item.preview}
                      alt={`Upload preview ${index + 1}`}
                      className={styles.previewImage}
                    />
                    {index === 0 && (
                      <span className={styles.coverBadge}>
                        <Check size={11} /> Cover
                      </span>
                    )}
                    <button
                      type="button"
                      onClick={() => handleRemoveImage(index)}
                      className={styles.removeImageBtn}
                      title="Remove this photo"
                      aria-label={`Remove photo ${index + 1}`}
                    >
                      <X size={14} />
                    </button>
                    <span className={styles.imageIndexBadge}>#{index + 1}</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          <button type="submit" disabled={loading} className={styles.submitBtn}>
            {loading ? (isEditMode ? 'Saving Changes...' : 'Submitting...') : (isEditMode ? 'Save Changes' : 'Submit Property')}
          </button>
        </form>
      </div>

      {showConfirmModal && (
        <div className={styles.modalBackdrop}>
          <div className={styles.modalContent}>
            <h3 className={styles.modalTitle}>
              <AlertCircle size={22} />
              {currentListingStatus === 'APPROVED' ? 'Reset to Pending Approval?' : 'Re-submit for Approval?'}
            </h3>
            <p className={styles.modalText}>
              {currentListingStatus === 'APPROVED'
                ? 'Saving changes to this approved listing will reset its status to PENDING. It will be temporarily removed from public searches while waiting for administrator re-approval. Do you want to proceed?'
                : 'Saving changes will reset this rejected listing to PENDING and submit it to the administrator for re-evaluation. Do you want to proceed?'}
            </p>
            <div className={styles.modalActions}>
              <button
                type="button"
                onClick={() => setShowConfirmModal(false)}
                className={styles.modalCancelBtn}
                disabled={loading}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowConfirmModal(false);
                  executeSubmit();
                }}
                className={styles.modalConfirmBtn}
                disabled={loading}
              >
                {loading ? 'Saving...' : 'Confirm & Save'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
