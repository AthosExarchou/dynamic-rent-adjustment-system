import React, { useRef, useEffect } from 'react';
import { ChevronUp, ChevronDown } from 'lucide-react';
import styles from './NumberInput.module.css';

/**
 * Number input with custom stepper controls and continuous hold support.
 */
export default function NumberInput({
  id,
  name,
  value = '',
  onChange,
  onBlur,
  min,
  max,
  step = 1,
  required = false,
  disabled = false,
  placeholder,
  className = '',
  containerClassName = '',
  ...restProps
}) {
  const inputRef = useRef(null);
  const holdTimeoutRef = useRef(null);
  const holdIntervalRef = useRef(null);
  const isHoldingRef = useRef(false);
  const isPointerActionRef = useRef(false);
  const cleanupWindowRef = useRef(null);

  const numValue = value === '' || isNaN(Number(value)) ? null : Number(value);
  const latestValRef = useRef(numValue);

  useEffect(() => {
    if (!isHoldingRef.current) {
      latestValRef.current = numValue;
    }
  }, [numValue]);

  const isUpDisabled = disabled || (max !== undefined && numValue !== null && numValue >= max);
  const isDownDisabled = disabled || (min !== undefined && numValue !== null && numValue <= min);

  const clearHoldTimers = () => {
    if (holdTimeoutRef.current) {
      clearTimeout(holdTimeoutRef.current);
      holdTimeoutRef.current = null;
    }
    if (holdIntervalRef.current) {
      clearInterval(holdIntervalRef.current);
      holdIntervalRef.current = null;
    }
    isHoldingRef.current = false;
  };

  const stopHold = () => {
    clearHoldTimers();
    if (cleanupWindowRef.current) {
      cleanupWindowRef.current();
      cleanupWindowRef.current = null;
    }
  };

  useEffect(() => {
    return () => {
      clearHoldTimers();
      cleanupWindowRef.current?.();
    };
  }, []);

  const fireChange = (newVal) => {
    if (inputRef.current) {
      inputRef.current.value = String(newVal);
    }
    onChange?.({
      target: {
        id,
        name: name || id,
        value: String(newVal),
      },
    });
  };

  const stepOnce = (direction) => {
    const current = latestValRef.current;
    let next;
    if (direction === 'up') {
      if (current !== null) {
        next = current + step;
      } else if (min !== undefined && min > 0) {
        next = min;
      } else {
        next = step;
      }
      if (max !== undefined && next >= max) {
        next = max;
        stopHold();
      }
    } else {
      if (current !== null) {
        next = current - step;
      } else if (min !== undefined && min > 0) {
        next = min;
      } else if (min !== undefined && min <= 0) {
        next = 0;
      } else {
        next = -step;
      }
      if (min !== undefined && next <= min) {
        next = min;
        stopHold();
      }
    }

    const precision = (String(step).split('.')[1] || '').length;
    if (precision > 0) {
      next = Number(next.toFixed(precision));
    }

    latestValRef.current = next;
    fireChange(next);
  };

  const handlePointerDown = (direction, e) => {
    if (e.button !== 0) return;
    if (direction === 'up' && isUpDisabled) return;
    if (direction === 'down' && isDownDisabled) return;
    e.preventDefault();

    if (!isHoldingRef.current) {
      latestValRef.current = numValue;
    }

    isPointerActionRef.current = true;
    isHoldingRef.current = true;

    cleanupWindowRef.current?.();
    const onWindowRelease = () => {
      stopHold();
    };
    window.addEventListener('pointerup', onWindowRelease);
    window.addEventListener('pointercancel', onWindowRelease);
    window.addEventListener('blur', onWindowRelease);
    cleanupWindowRef.current = () => {
      window.removeEventListener('pointerup', onWindowRelease);
      window.removeEventListener('pointercancel', onWindowRelease);
      window.removeEventListener('blur', onWindowRelease);
    };

    stepOnce(direction);

    // Only start repeat interval if not already stopped (e.g. hit min/max on initial step)
    if (isHoldingRef.current) {
      holdTimeoutRef.current = setTimeout(() => {
        if (!isHoldingRef.current) return;
        holdIntervalRef.current = setInterval(() => {
          stepOnce(direction);
        }, 50);
      }, 350);
    }
  };

  return (
    <div className={`${styles.container} ${containerClassName}`}>
      <input
        ref={inputRef}
        type="number"
        id={id}
        name={name}
        value={value}
        onChange={onChange}
        onBlur={onBlur}
        min={min}
        max={max}
        step={step}
        required={required}
        disabled={disabled}
        placeholder={placeholder}
        className={`${styles.input} ${className}`}
        {...restProps}
      />
      <div className={styles.stepper}>
        <button
          type="button"
          tabIndex={-1}
          className={`${styles.stepBtn} ${styles.stepBtnUp}`}
          onPointerDown={(e) => handlePointerDown('up', e)}
          onPointerUp={stopHold}
          onPointerCancel={stopHold}
          onClick={(e) => {
            e.preventDefault();
            if (isPointerActionRef.current) {
              isPointerActionRef.current = false;
              return;
            }
            stepOnce('up');
          }}
          disabled={isUpDisabled}
          aria-label="Increase value"
        >
          <ChevronUp size={12} />
        </button>
        <button
          type="button"
          tabIndex={-1}
          className={`${styles.stepBtn} ${styles.stepBtnDown}`}
          onPointerDown={(e) => handlePointerDown('down', e)}
          onPointerUp={stopHold}
          onPointerCancel={stopHold}
          onClick={(e) => {
            e.preventDefault();
            if (isPointerActionRef.current) {
              isPointerActionRef.current = false;
              return;
            }
            stepOnce('down');
          }}
          disabled={isDownDisabled}
          aria-label="Decrease value"
        >
          <ChevronDown size={12} />
        </button>
      </div>
    </div>
  );
}
