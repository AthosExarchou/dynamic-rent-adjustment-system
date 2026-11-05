import React, { createContext, useContext, useState, useCallback, useMemo, useEffect } from 'react';
import ToastContainer from './ToastContainer';

const ToastContext = createContext(null);

let nextId = 0;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const removeToast = useCallback((id) => {
    if (id === undefined) {
      setToasts([]);
    } else {
      setToasts(prev => prev.filter(t => t.id !== id));
    }
  }, []);

  const addToast = useCallback((message, type = 'info', duration = 4000) => {
    const id = ++nextId;
    setToasts(prev => [...prev, { id, message, type, duration }]);
    return id;
  }, []);

  const toast = useMemo(() => {
    const fn = (message, duration) => addToast(message, 'info', duration);
    fn.success = (message, duration) => addToast(message, 'success', duration);
    fn.error = (message, duration) => addToast(message, 'error', duration);
    fn.warning = (message, duration) => addToast(message, 'warning', duration);
    fn.info = (message, duration) => addToast(message, 'info', duration);
    fn.dismiss = removeToast;
    return fn;
  }, [addToast, removeToast]);

  useEffect(() => {
    window.__drasToast = toast;
  }, [toast]);

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <ToastContainer toasts={toasts} onRemove={removeToast} />
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within a ToastProvider');
  return ctx;
}
