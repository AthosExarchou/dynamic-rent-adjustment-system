import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../auth';
import { UserCircle, Mail, Shield, Settings, Key, Trash2 } from 'lucide-react';
import styles from './Profile.module.css';

export default function Profile() {
  const { user, roles } = useAuth();

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h2 className={styles.title}>
          <UserCircle className={styles.titleIcon} size={28} /> My Profile
        </h2>
      </div>

      <hr className={styles.divider} />

      <div className={`${styles.card} ${styles.cardHover}`}>
        <div className={styles.cardHeader}>
          <h3 className={styles.cardTitle}>Account Details</h3>
        </div>
        
        <div className={styles.body}>
          <div className={styles.profileGrid}>
            <div className={styles.infoGroup}>
              <div className={styles.infoLabel}>
                <UserCircle size={18} /> Username
              </div>
              <div className={styles.infoValue}>{user?.username}</div>
            </div>

            <div className={styles.infoGroup}>
              <div className={styles.infoLabel}>
                <Mail size={18} /> Email Address
              </div>
              <div className={styles.infoValue}>{user?.email}</div>
            </div>

            <div className={styles.infoGroup}>
              <div className={styles.infoLabel}>
                <Shield size={18} /> Account Roles
              </div>
              <div className={styles.rolesContainer}>
                {roles.map(role => (
                  <span key={role} className={styles.roleBadge}>{role}</span>
                ))}
              </div>
            </div>
          </div>

          <div className={styles.actionsContainer}>
            <h4 className={styles.actionsTitle}>Account Management</h4>
            <div className={styles.actionsGrid}>
              <Link to="/profile/edit" className={`${styles.actionBtn} ${styles.btnPrimary}`}>
                <Settings size={18} /> Edit Profile Details
              </Link>
              
              <Link to="/profile/password" className={`${styles.actionBtn} ${styles.btnOutlinePrimary}`}>
                <Key size={18} /> Change Password
              </Link>
              
              <Link to="/profile/delete" className={`${styles.actionBtn} ${styles.btnDanger}`}>
                <Trash2 size={18} /> Delete Account
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
