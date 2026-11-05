import React, { useEffect, useState } from 'react';
import { CheckCircle, XCircle, AlertTriangle, Info, X } from 'lucide-react';
import styles from './Toast.module.css';

const ICONS = {
  success: CheckCircle,
  error: XCircle,
  warning: AlertTriangle,
  info: Info,
};

function ToastItem({ toast, onRemove }) {
  const [exiting, setExiting] = useState(false);
  const [isPaused, setIsPaused] = useState(false);

  useEffect(() => {
    if (toast.duration <= 0 || isPaused || exiting) return;
    const timeout = setTimeout(() => {
      setExiting(true);
    }, toast.duration);
    return () => clearTimeout(timeout);
  }, [toast.duration, isPaused, exiting]);

  // Safety fallback: ensure toast is removed even if CSS animationend event doesn't fire
  useEffect(() => {
    if (!exiting) return;
    const fallback = setTimeout(() => {
      onRemove(toast.id);
    }, 350);
    return () => clearTimeout(fallback);
  }, [exiting, onRemove, toast.id]);

  const handleAnimationEnd = () => {
    if (exiting) onRemove(toast.id);
  };

  const handleClose = () => setExiting(true);

  const Icon = ICONS[toast.type] || Info;

  const renderMessage = () => {
    if (typeof toast.message === 'string' || typeof toast.message === 'number') {
      return toast.message;
    }
    if (toast.message instanceof Error) {
      return toast.message.message;
    }
    if (toast.message && typeof toast.message === 'object' && !React.isValidElement(toast.message)) {
      return toast.message.message || JSON.stringify(toast.message);
    }
    return toast.message;
  };

  return (
    <div
      className={`${styles.toast} ${styles[toast.type] || styles.info} ${exiting ? styles.exit : styles.enter}`}
      onAnimationEnd={handleAnimationEnd}
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
      role="alert"
    >
      <Icon size={20} className={styles.icon} />
      <span className={styles.message}>{renderMessage()}</span>
      <button
        type="button"
        className={styles.closeBtn}
        onClick={handleClose}
        aria-label="Dismiss notification"
      >
        <X size={16} />
      </button>
    </div>
  );
}

export default function ToastContainer({ toasts, onRemove }) {
  return (
    <div className={styles.container} aria-live="polite">
      {toasts.map(toast => (
        <ToastItem key={toast.id} toast={toast} onRemove={onRemove} />
      ))}
    </div>
  );
}
