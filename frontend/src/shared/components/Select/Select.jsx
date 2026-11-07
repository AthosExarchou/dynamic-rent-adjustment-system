import React, { useState, useRef, useEffect } from 'react';
import { ChevronDown, Check } from 'lucide-react';
import styles from './Select.module.css';

/**
 * Custom dropdown select menu.
 */
export default function Select({
  value = '',
  onChange,
  options = [],
  placeholder = 'Select...',
  icon: Icon,
  prefix = '',
  disabled = false,
  align = 'auto',
  className = '',
  id,
  ariaLabel,
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [dropdownAlign, setDropdownAlign] = useState(align === 'right' ? 'right' : 'left');
  const containerRef = useRef(null);

  // Normalize options to { value, label }
  const normalizedOptions = options.map(opt => {
    if (typeof opt === 'object' && opt !== null) {
      return { value: opt.value ?? '', label: opt.label ?? String(opt.value ?? '') };
    }
    return { value: opt, label: String(opt) };
  });

  const selectedOption = normalizedOptions.find(opt => String(opt.value) === String(value));

  useEffect(() => {
    if (align === 'right') {
      setDropdownAlign('right');
      return;
    }
    if (align === 'left') {
      setDropdownAlign('left');
      return;
    }
    if (isOpen && containerRef.current) {
      const rect = containerRef.current.getBoundingClientRect();
      const menuWidth = Math.max(rect.width, 200);
      if (rect.left + menuWidth > window.innerWidth - 16) {
        setDropdownAlign('right');
      } else {
        setDropdownAlign('left');
      }
    }
  }, [isOpen, align]);

  // Close on click outside
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

  // Close on Escape
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (isOpen && e.key === 'Escape') {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
    }
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen]);

  const handleSelect = (val) => {
    onChange?.(val);
    setIsOpen(false);
  };

  return (
    <div className={styles.container} ref={containerRef}>
      <button
        type="button"
        id={id}
        className={`${styles.trigger} ${isOpen ? styles.triggerActive : ''} ${className}`}
        onClick={() => !disabled && setIsOpen(!isOpen)}
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        aria-label={ariaLabel}
      >
        <span className={styles.triggerContent}>
          {Icon && <Icon size={16} className={styles.triggerIcon} />}
          <span className={selectedOption ? styles.triggerValue : styles.triggerPlaceholder}>
            {prefix ? `${prefix} ` : ''}
            {selectedOption ? selectedOption.label : placeholder}
          </span>
        </span>
        <ChevronDown size={16} className={`${styles.chevron} ${isOpen ? styles.chevronOpen : ''}`} />
      </button>

      {isOpen && (
        <div
          className={`${styles.dropdown} ${dropdownAlign === 'right' ? styles.dropdownRight : ''}`}
          role="listbox"
          tabIndex={-1}
        >
          <div className={styles.optionsList}>
            {normalizedOptions.map((opt) => {
              const isSelected = String(opt.value) === String(value);
              return (
                <button
                  key={String(opt.value)}
                  type="button"
                  className={`${styles.option} ${isSelected ? styles.optionSelected : ''}`}
                  onClick={() => handleSelect(opt.value)}
                  role="option"
                  aria-selected={isSelected}
                >
                  <span className={styles.optionLabel}>{opt.label}</span>
                  {isSelected && <Check size={15} className={styles.checkIcon} />}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
