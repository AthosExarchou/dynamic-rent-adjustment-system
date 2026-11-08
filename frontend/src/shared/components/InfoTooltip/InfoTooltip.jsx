import React, { useState, useEffect } from 'react';
import { HelpCircle } from 'lucide-react';
import styles from './InfoTooltip.module.css';

/**
 * Accessible, mobile touch-friendly information tooltip.
 *
 * Supports:
 * - Desktop hover (mouseenter/mouseleave)
 * - Touch & click toggling with outside-click dismissal
 * - Keyboard navigation (Enter/Space to toggle, Escape to close)
 * - WAI-ARIA role="tooltip" on bubble and role="button" on trigger
 * - Atomic word-wrapping via optional `children` prop
 */
export default function InfoTooltip({ text, children, size = 14, className = '' }) {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (!visible) return;
    const handleClickOutside = () => setVisible(false);
    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  }, [visible]);

  const icon = (
    <span
      className={`${styles.tooltipContainer} ${className}`}
      onClick={(e) => {
        e.preventDefault();
        e.stopPropagation();
        setVisible((v) => !v);
      }}
      onMouseEnter={() => setVisible(true)}
      onMouseLeave={() => setVisible(false)}
      onFocus={() => setVisible(true)}
      onBlur={() => setVisible(false)}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          setVisible((v) => !v);
        } else if (e.key === 'Escape') {
          setVisible(false);
        }
      }}
      tabIndex={0}
      role="button"
      aria-label={text}
      aria-expanded={visible}
    >
      <HelpCircle size={size} className={styles.helpIcon} />
      {visible && (
        <span className={styles.tooltipBubble} role="tooltip">
          {text}
        </span>
      )}
    </span>
  );

  if (!children) {
    return icon;
  }

  return (
    <span className={styles.tooltipNoWrap}>
      {children}
      {icon}
    </span>
  );
}
