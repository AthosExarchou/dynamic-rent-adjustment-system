import React, { useState, useEffect, useRef, useCallback, useLayoutEffect } from 'react';
import { createPortal } from 'react-dom';
import { HelpCircle } from 'lucide-react';
import styles from './InfoTooltip.module.css';

/**
 * Accessible, mobile touch-friendly information tooltip.
 *
 * Rendered via React Portal directly into document.body to prevent clipping
 * by parent cards, modals, tables, or overflow:hidden boundaries.
 * Includes automatic vertical flipping (top/bottom) and horizontal viewport clamping.
 */
export default function InfoTooltip({ text, children, size = 14, className = '' }) {
  const [visible, setVisible] = useState(false);
  const [pinned, setPinned] = useState(false);
  const [coords, setCoords] = useState(null);
  const triggerRef = useRef(null);
  const bubbleRef = useRef(null);

  const updatePosition = useCallback(() => {
    if (!triggerRef.current) return;
    const triggerRect = triggerRef.current.getBoundingClientRect();

    // If trigger element has zero size or is hidden, or scrolled completely out of viewport
    if (
      (triggerRect.width === 0 && triggerRect.height === 0) ||
      triggerRect.bottom < 0 ||
      triggerRect.top > window.innerHeight
    ) {
      setVisible(false);
      setPinned(false);
      return;
    }

    const bubble = bubbleRef.current;
    const bubbleWidth = bubble ? bubble.offsetWidth : 220;
    const bubbleHeight = bubble ? bubble.offsetHeight : 60;

    const spaceAbove = triggerRect.top;
    const spaceBelow = window.innerHeight - triggerRect.bottom;
    const placeAbove = spaceAbove >= bubbleHeight + 12 || spaceAbove >= spaceBelow;

    const placement = placeAbove ? 'top' : 'bottom';
    const top = placeAbove
      ? triggerRect.top - bubbleHeight - 8
      : triggerRect.bottom + 8;

    const triggerCenter = triggerRect.left + triggerRect.width / 2;
    const idealLeft = triggerCenter - bubbleWidth / 2;
    const minLeft = 12;
    const maxLeft = Math.max(minLeft, window.innerWidth - bubbleWidth - 12);
    const clampedLeft = Math.max(minLeft, Math.min(idealLeft, maxLeft));

    const arrowLeft = Math.max(12, Math.min(triggerCenter - clampedLeft, bubbleWidth - 12));

    setCoords({
      top,
      left: clampedLeft,
      placement,
      arrowLeft
    });
  }, []);

  useEffect(() => {
    if (!visible) {
      setCoords(null);
      return;
    }

    updatePosition();

    const handleScrollOrResize = () => {
      updatePosition();
    };

    const handleClickOutside = (e) => {
      if (triggerRef.current && triggerRef.current.contains(e.target)) return;
      setVisible(false);
      setPinned(false);
    };

    window.addEventListener('scroll', handleScrollOrResize, true);
    window.addEventListener('resize', handleScrollOrResize);
    document.addEventListener('click', handleClickOutside);

    return () => {
      window.removeEventListener('scroll', handleScrollOrResize, true);
      window.removeEventListener('resize', handleScrollOrResize);
      document.removeEventListener('click', handleClickOutside);
    };
  }, [visible, updatePosition]);

  useLayoutEffect(() => {
    if (visible && bubbleRef.current) {
      updatePosition();
    }
  }, [visible, updatePosition, text]);

  const handleTriggerClick = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setPinned((p) => {
      const next = !p;
      setVisible(next);
      return next;
    });
  };

  const handleMouseEnter = () => {
    if (!pinned) setVisible(true);
  };

  const handleMouseLeave = () => {
    if (!pinned) setVisible(false);
  };

  const handleFocus = () => {
    setVisible(true);
  };

  const handleBlur = () => {
    if (!pinned) setVisible(false);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      e.stopPropagation();
      setPinned((p) => {
        const next = !p;
        setVisible(next);
        return next;
      });
    } else if (e.key === 'Escape') {
      setVisible(false);
      setPinned(false);
    }
  };

  const icon = (
    <span
      ref={triggerRef}
      className={`${styles.tooltipContainer} ${className}`}
      onClick={handleTriggerClick}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      onFocus={handleFocus}
      onBlur={handleBlur}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      role="button"
      aria-label={text}
      aria-expanded={visible}
    >
      <HelpCircle size={size} className={styles.helpIcon} />
    </span>
  );

  const portalBubble = visible && typeof document !== 'undefined'
    ? createPortal(
        <span
          ref={bubbleRef}
          className={`${styles.tooltipBubble} ${
            coords?.placement === 'bottom' ? styles.placementBottom : styles.placementTop
          }`}
          style={{
            top: coords ? `${coords.top}px` : '-9999px',
            left: coords ? `${coords.left}px` : '-9999px',
            opacity: coords ? 1 : 0,
          }}
          role="tooltip"
        >
          {text}
          <span
            className={styles.tooltipArrow}
            style={{
              left: coords?.arrowLeft != null ? `${coords.arrowLeft}px` : '50%',
            }}
            aria-hidden="true"
          />
        </span>,
        document.body
      )
    : null;

  if (!children) {
    return (
      <>
        {icon}
        {portalBubble}
      </>
    );
  }

  return (
    <span className={styles.tooltipNoWrap}>
      {children}
      {icon}
      {portalBubble}
    </span>
  );
}
