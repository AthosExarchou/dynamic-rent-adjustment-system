import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../auth';
import styles from './Profile.module.css';

export default function Profile() {
  const { user, roles } = useAuth();

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h2 className={styles.title}>Account Profile</h2>
        
        <div className={styles.details}>
          <p><strong>Username:</strong> {user?.username}</p>
          <p><strong>Email Address:</strong> {user?.email}</p>
          <p><strong>Account Roles:</strong> {roles.join(', ')}</p>
        </div>

        <div className={styles.actions}>
          <Link to="/profile/edit" className={styles.btn}>Edit Profile Details</Link>
          <Link to="/profile/password" className={styles.btn}>Change Password</Link>
          <Link to="/profile/delete" className={styles.deleteBtn}>Delete Account Permanently</Link>
        </div>
      </div>
    </div>
  );
}
