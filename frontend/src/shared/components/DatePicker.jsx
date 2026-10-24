import React, { useState, useRef, useEffect } from 'react';
import { Calendar, ChevronLeft, ChevronRight, X } from 'lucide-react';
import styles from './DatePicker.module.css';

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];

const WEEKDAY_NAMES = ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su'];

/**
 * Format Date to "YYYY-MM-DD"
 */
function toDateString(year, month, day) {
  const m = String(month + 1).padStart(2, '0');
  const d = String(day).padStart(2, '0');
  return `${year}-${m}-${d}`;
}

/**
 * Formats "YYYY-MM-DD" into human-readable date, e.g. "21 Oct 2026"
 */
function formatDisplayDate(dateStr) {
  if (!dateStr) return '';
  const [y, m, d] = dateStr.split('-').map(Number);
  if (!y || !m || !d) return dateStr;
  const shortMonth = MONTH_NAMES[m - 1]?.slice(0, 3) || '';
  return `${d} ${shortMonth} ${y}`;
}

export default function DatePicker({
  value = '',
  onChange,
  placeholder = 'Select date',
  min,
  max,
  id,
  align = 'auto',
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [dropdownAlign, setDropdownAlign] = useState(align === 'right' ? 'right' : 'left');
  const containerRef = useRef(null);

  // Update alignment when opening or when align prop changes
  useEffect(() => {
    if (align === 'right') {
      setDropdownAlign('right');
      return;
    }
    if (align === 'left') {
      setDropdownAlign('left');
      return;
    }
    // Auto-alignment: check if opening to the right would overflow the viewport
    if (isOpen && containerRef.current) {
      const rect = containerRef.current.getBoundingClientRect();
      const dropdownWidth = 280;
      if (rect.left + dropdownWidth > window.innerWidth - 16) {
        setDropdownAlign('right');
      } else {
        setDropdownAlign('left');
      }
    }
  }, [isOpen, align]);

  // Initialize viewing month/year from current value or today
  const initialDate = value ? new Date(value + 'T00:00:00') : new Date();
  const [viewYear, setViewYear] = useState(initialDate.getFullYear());
  const [viewMonth, setViewMonth] = useState(initialDate.getMonth());

  // Keep viewing month in sync if external value changes
  useEffect(() => {
    if (value) {
      const d = new Date(value + 'T00:00:00');
      if (!isNaN(d.getTime())) {
        setViewYear(d.getFullYear());
        setViewMonth(d.getMonth());
      }
    }
  }, [value]);

  // Click outside to close
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  // Month navigation
  const handlePrevMonth = (e) => {
    e.stopPropagation();
    if (viewMonth === 0) {
      setViewMonth(11);
      setViewYear(y => y - 1);
    } else {
      setViewMonth(m => m - 1);
    }
  };

  const handleNextMonth = (e) => {
    e.stopPropagation();
    if (viewMonth === 11) {
      setViewMonth(0);
      setViewYear(y => y + 1);
    } else {
      setViewMonth(m => m + 1);
    }
  };

  // Days calculations (Monday = 0, Sunday = 6)
  const firstDay = new Date(viewYear, viewMonth, 1).getDay();
  const startOffset = (firstDay + 6) % 7;
  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
  const daysInPrevMonth = new Date(viewYear, viewMonth, 0).getDate();

  const todayStr = toDateString(
    new Date().getFullYear(),
    new Date().getMonth(),
    new Date().getDate()
  );

  const handleSelectDay = (day) => {
    const selectedStr = toDateString(viewYear, viewMonth, day);
    if (min && selectedStr < min) return;
    if (max && selectedStr > max) return;
    onChange(selectedStr);
    setIsOpen(false);
  };

  const handleClear = (e) => {
    e.stopPropagation();
    onChange('');
  };

  const handleToday = () => {
    const today = new Date();
    const currentTodayStr = toDateString(today.getFullYear(), today.getMonth(), today.getDate());
    if (min && currentTodayStr < min) return;
    if (max && currentTodayStr > max) return;
    setViewYear(today.getFullYear());
    setViewMonth(today.getMonth());
    onChange(currentTodayStr);
    setIsOpen(false);
  };

  return (
    <div className={styles.container} ref={containerRef}>
      <button
        type="button"
        id={id}
        className={`${styles.trigger} ${isOpen ? styles.triggerActive : ''}`}
        onClick={() => setIsOpen(!isOpen)}
        aria-haspopup="dialog"
        aria-expanded={isOpen}
      >
        <span className={styles.triggerContent}>
          <Calendar size={15} className={styles.calendarIcon} />
          <span className={value ? styles.triggerValue : styles.triggerPlaceholder}>
            {value ? formatDisplayDate(value) : placeholder}
          </span>
        </span>
        {value && (
          <span
            role="button"
            tabIndex={0}
            className={styles.clearBtn}
            onClick={handleClear}
            onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') handleClear(e); }}
            title="Clear date"
            aria-label="Clear date"
          >
            <X size={14} />
          </span>
        )}
      </button>

      {isOpen && (
        <div 
          className={`${styles.dropdown} ${dropdownAlign === 'right' ? styles.dropdownRight : ''}`} 
          role="dialog" 
          aria-label="Calendar"
        >
          {/* Calendar Header */}
          <div className={styles.header}>
            <button
              type="button"
              className={styles.navBtn}
              onClick={handlePrevMonth}
              aria-label="Previous month"
            >
              <ChevronLeft size={16} />
            </button>
            <span className={styles.monthYear}>
              {MONTH_NAMES[viewMonth]} {viewYear}
            </span>
            <button
              type="button"
              className={styles.navBtn}
              onClick={handleNextMonth}
              aria-label="Next month"
            >
              <ChevronRight size={16} />
            </button>
          </div>

          {/* Weekday Labels */}
          <div className={styles.weekdays}>
            {WEEKDAY_NAMES.map(day => (
              <span key={day} className={styles.weekday}>
                {day}
              </span>
            ))}
          </div>

          {/* Days Grid */}
          <div className={styles.daysGrid}>
            {/* Previous month trailing days */}
            {Array.from({ length: startOffset }).map((_, i) => {
              const dayNum = daysInPrevMonth - startOffset + i + 1;
              return (
                <span key={`prev-${i}`} className={styles.otherMonthDay}>
                  {dayNum}
                </span>
              );
            })}

            {/* Current month days */}
            {Array.from({ length: daysInMonth }).map((_, i) => {
              const day = i + 1;
              const dateStr = toDateString(viewYear, viewMonth, day);
              const isSelected = value === dateStr;
              const isToday = todayStr === dateStr;
              const isDisabled = (min && dateStr < min) || (max && dateStr > max);

              return (
                <button
                  key={day}
                  type="button"
                  disabled={isDisabled}
                  onClick={() => handleSelectDay(day)}
                  className={`
                    ${styles.dayBtn}
                    ${isSelected ? styles.selectedDay : ''}
                    ${isToday ? styles.today : ''}
                    ${isDisabled ? styles.disabledDay : ''}
                  `}
                >
                  {day}
                </button>
              );
            })}
          </div>

          {/* Quick Footer */}
          <div className={styles.footer}>
            <button
              type="button"
              className={styles.footerBtn}
              onClick={handleToday}
              disabled={(min && todayStr < min) || (max && todayStr > max)}
            >
              Today
            </button>
            {value && (
              <button
                type="button"
                className={styles.footerBtnClear}
                onClick={handleClear}
              >
                Clear
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
