import React, { useEffect, useRef } from 'react';
import { AlertTriangle, Info } from 'lucide-react';
import styles from './ConfirmDialog.module.css';

/**
 * Styled confirmation dialog that replaces browser's native window.confirm().
 *
 * @param {Object} props
 * @param {string} props.title - Dialog title
 * @param {string|React.ReactNode} props.message - Confirmation message body
 * @param {string} [props.confirmLabel='Confirm'] - Text for the confirm button
 * @param {string} [props.cancelLabel='Cancel'] - Text for the cancel button
 * @param {'danger'|'warning'|'primary'} [props.variant='danger'] - Button style variant
 * @param {boolean} [props.loading=false] - Whether confirmation is currently processing
 * @param {boolean} [props.disabled=false] - Disable confirmation buttons
 * @param {Function} props.onConfirm - Called when user confirms
 * @param {Function} props.onCancel - Called when user cancels
 */
export default function ConfirmDialog({
  title = 'Confirm Action',
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  variant = 'danger',
  loading = false,
  disabled = false,
  onConfirm,
  onCancel,
}) {
  const cancelBtnRef = useRef(null);
  const confirmBtnRef = useRef(null);
  const onCancelRef = useRef(onCancel);
  onCancelRef.current = onCancel;

  useEffect(() => {
    // Focus cancel button on mount for safe keyboard navigation
    cancelBtnRef.current?.focus();

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        onCancelRef.current?.();
      } else if (e.key === 'Tab') {
        // Trap focus between cancel and confirm buttons
        if (e.shiftKey) {
          if (document.activeElement === cancelBtnRef.current || document.activeElement !== confirmBtnRef.current) {
            e.preventDefault();
            confirmBtnRef.current?.focus();
          }
        } else {
          if (document.activeElement === confirmBtnRef.current || document.activeElement !== cancelBtnRef.current) {
            e.preventDefault();
            cancelBtnRef.current?.focus();
          }
        }
      }
    };

    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = originalOverflow;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  const Icon = variant === 'primary' ? Info : AlertTriangle;

  return (
    <div className={styles.overlay} onClick={onCancel}>
      <div
        className={styles.card}
        onClick={(e) => e.stopPropagation()}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        aria-describedby="confirm-dialog-message"
      >
        <div className={styles.header}>
          <Icon size={22} className={styles[`icon_${variant}`] || styles.icon_danger} />
          <h3 id="confirm-dialog-title" className={styles.title}>{title}</h3>
        </div>
        <p id="confirm-dialog-message" className={styles.message}>{message}</p>
        <div className={styles.actions}>
          <button
            ref={cancelBtnRef}
            type="button"
            className={styles.cancelBtn}
            onClick={onCancel}
            disabled={disabled || loading}
          >
            {cancelLabel}
          </button>
          <button
            ref={confirmBtnRef}
            type="button"
            className={`${styles.confirmBtn} ${styles[variant] || styles.danger}`}
            onClick={onConfirm}
            disabled={disabled || loading}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
