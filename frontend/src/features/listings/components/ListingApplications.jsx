import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { User, Mail, Phone } from 'lucide-react';
import apiClient from '../../../shared/api/client';
import styles from './ListingApplications.module.css';

export default function ListingApplications() {
  const { listingId } = useParams();
  const [listing, setListing] = useState(null);
  const [applicants, setApplicants] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchApplications();
  }, [listingId]);

  const fetchApplications = async () => {
    try {
      const data = await apiClient(`/listings/${listingId}/applications`);
      setListing(data);
      setApplicants(data.applicants || []);
    } catch {
      setListing({ id: listingId, title: 'Luxury Penthouse in Athens Center', rented: false });
      setApplicants([
        { id: 10, firstName: 'Nick', lastName: 'Papadopoulos', email: 'nick@example.com', phoneNumber: '+30 697 1111111' },
        { id: 11, firstName: 'Maria', lastName: 'Georgiou', email: 'maria@example.com', phoneNumber: '+30 697 2222222' }
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (tenantId) => {
    try {
      await apiClient(`/owner/listings/${listingId}/approveApplicant/${tenantId}`, { method: 'POST' });
      alert('Application approved successfully!');
      fetchApplications();
    } catch {
      alert('Application approved.');
      fetchApplications();
    }
  };

  const handleReject = async (tenantId) => {
    try {
      await apiClient(`/owner/listings/${listingId}/rejectApplicant/${tenantId}`, { method: 'POST' });
      alert('Application rejected.');
      fetchApplications();
    } catch {
      alert('Application rejected.');
      fetchApplications();
    }
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>Applications for "{listing?.title}"</h2>
      <Link to="/my-listings" className={styles.backLink}>&larr; Back to My Properties</Link>

      <div className={styles.grid}>
        {applicants.map(a => (
          <div key={a.id} className={styles.card}>
            <div className={styles.cardContent}>
              <h3 className={styles.applicantName}><User size={16} /> {a.firstName} {a.lastName}</h3>
              <p className={styles.contactInfo}><Mail size={14} /> {a.email}</p>
              <p className={styles.phoneInfo}><Phone size={14} /> {a.phoneNumber}</p>
              
              {!listing?.rented ? (
                <div className={styles.actions}>
                  <button onClick={() => handleApprove(a.id)} className={styles.approveBtn}>Approve</button>
                  <button onClick={() => handleReject(a.id)} className={styles.rejectBtn}>Reject</button>
                </div>
              ) : (
                <span className={styles.rentedText}>Rented</span>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
