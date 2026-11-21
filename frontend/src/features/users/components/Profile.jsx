import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../auth';
import { UserCircle, Shield, Settings, Key, Trash2, Briefcase, Calendar, Clock, Lock } from 'lucide-react';
import { InfoTooltip } from '../../../shared/components/InfoTooltip';
import UserAvatar from '../../../shared/components/UserAvatar';
import styles from './Profile.module.css';

const formatDate = (dateStr) => {
  if (!dateStr) return 'N/A';
  const d = new Date(dateStr.endsWith('Z') ? dateStr : dateStr + 'Z');
  return d.toLocaleDateString(undefined,
      { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' });
};

export default function Profile() {
  const { user, roles } = useAuth();
  const nameParts = [user?.firstName, user?.lastName].map(part => (part || '').trim()).filter(Boolean);
  const repeatedName = nameParts.length === 2 && nameParts[0].toLowerCase() === nameParts[1].toLowerCase();
  const displayName = repeatedName ? nameParts[0] : nameParts.join(' ') || user?.username || 'User';
  const hasName = nameParts.length > 0;

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
          <div className={styles.profileIdentity}>
            <UserAvatar user={user} size={72} />
            <div className={styles.identityText}>
              <h3 className={styles.identityName}>{displayName}</h3>
              <p className={styles.identityEmail}>
                {hasName ? `${user?.username} · ${user?.email}` : user?.email}
              </p>
            </div>
          </div>

          <div className={styles.profileGrid}>
            <div className={styles.infoGroup}>
              <div className={styles.infoLabel}>
                <Calendar size={18} /> Member Since
              </div>
              <div className={styles.infoValue}>{formatDate(user?.createdAt)}</div>
            </div>

            <div className={styles.infoGroup}>
              <div className={styles.infoLabel}>
                <Clock size={18} /> Last Login
              </div>
              <div className={styles.infoValue}>{formatDate(user?.lastLogin)}</div>
            </div>

            <div className={styles.infoGroup}>
              <div className={styles.infoLabel}>
                <Lock size={18} /> Password Last Changed
              </div>
              <div className={styles.infoValue}>
                {user?.passwordChangedAt ? formatDate(user.passwordChangedAt) : 'Never'}
              </div>
            </div>

            <div className={styles.infoGroup}>
              <div className={styles.infoLabel}>
                <Shield size={18} /> Account Roles
                <InfoTooltip text="Platform access roles assigned to your account. You can register as an owner to list and manage properties." />
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
              {!roles.includes('ADMIN') && !user?.isSystem && (
                <Link to="/profile/edit" className={`${styles.actionBtn} ${styles.btnPrimary}`}>
                  <Settings size={18} /> Edit Profile Details
                </Link>
              )}

              {!roles.includes('OWNER') && !roles.includes('ADMIN') && !user?.isSystem && (
                <Link to="/owner/new" className={`${styles.actionBtn} ${styles.btnOutlinePrimary}`}>
                  <Briefcase size={18} /> Register as Owner
                </Link>
              )}

              <Link to="/profile/password" className={`${styles.actionBtn} ${styles.btnOutlinePrimary}`}>
                <Key size={18} /> Change Password
              </Link>

              {!roles.includes('ADMIN') && !user?.isSystem && (
                <Link to="/profile/delete" className={`${styles.actionBtn} ${styles.btnDanger}`}>
                  <Trash2 size={18} /> Delete Account
                </Link>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
