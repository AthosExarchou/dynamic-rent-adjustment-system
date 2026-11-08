import React from 'react';
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';
import styles from './Pagination.module.css';

/**
 * Pagination controls for list and grid views.
 *
 * @param {Object} props
 * @param {number} [props.currentPage=1]
 * @param {number} [props.totalItems=0]
 * @param {number} [props.pageSize=10]
 * @param {Function} props.onPageChange
 * @param {number[]} [props.pageSizeOptions]
 * @param {Function} [props.onPageSizeChange]
 * @param {string} [props.className='']
 */
export default function Pagination({
  currentPage = 1,
  totalItems = 0,
  pageSize = 10,
  onPageChange,
  pageSizeOptions = null,
  onPageSizeChange = null,
  className = '',
}) {
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));

  if (totalItems === 0 || totalPages <= 1) return null;

  const startItem = (currentPage - 1) * pageSize + 1;
  const endItem   = Math.min(totalItems, currentPage * pageSize);

  // Generate visible page numbers with ellipsis gaps
  const getPageNumbers = () => {
    const delta = 1;
    const range = [];
    const result = [];
    let prev;

    for (let i = 1; i <= totalPages; i++) {
      if (
        i === 1 ||
        i === totalPages ||
        (i >= currentPage - delta && i <= currentPage + delta)
      ) {
        range.push(i);
      }
    }

    for (const page of range) {
      if (prev !== undefined) {
        if (page - prev === 2) {
          result.push(prev + 1); // fill single-gap without ellipsis
        } else if (page - prev > 2) {
          result.push('...');
        }
      }
      result.push(page);
      prev = page;
    }
    return result;
  };

  return (
    <nav className={`${styles.pagination} ${className}`} aria-label="Pagination navigation">
      <span className={styles.info}>
        Showing <strong>{startItem}</strong>-<strong>{endItem}</strong> of <strong>{totalItems}</strong>
      </span>

      <div className={styles.controls}>
        <button
          type="button"
          className={styles.navBtn}
          onClick={() => onPageChange(1)}
          disabled={currentPage === 1}
          aria-label="First page"
          title="First page"
        >
          <ChevronsLeft size={15} />
        </button>

        <button
          type="button"
          className={styles.navBtn}
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 1}
          aria-label="Previous page"
          title="Previous page"
        >
          <ChevronLeft size={15} />
        </button>

        <div className={styles.pages}>
          {getPageNumbers().map((page, idx) =>
            page === '...' ? (
              <span key={`ellipsis-${idx}`} className={styles.ellipsis}>...</span>
            ) : (
              <button
                key={page}
                type="button"
                className={`${styles.pageBtn} ${currentPage === page ? styles.active : ''}`}
                onClick={() => onPageChange(page)}
                aria-current={currentPage === page ? 'page' : undefined}
                aria-label={`Page ${page}`}
              >
                {page}
              </button>
            )
          )}
        </div>

        <button
          type="button"
          className={styles.navBtn}
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage === totalPages}
          aria-label="Next page"
          title="Next page"
        >
          <ChevronRight size={15} />
        </button>

        <button
          type="button"
          className={styles.navBtn}
          onClick={() => onPageChange(totalPages)}
          disabled={currentPage === totalPages}
          aria-label="Last page"
          title="Last page"
        >
          <ChevronsRight size={15} />
        </button>
      </div>

      {pageSizeOptions && onPageSizeChange && (
        <div className={styles.pageSizeWrapper}>
          <label htmlFor="pageSizeSelect" className={styles.pageSizeLabel}>Per page:</label>
          <select
            id="pageSizeSelect"
            className={styles.pageSizeSelect}
            value={pageSize}
            onChange={e => onPageSizeChange(Number(e.target.value))}
            aria-label="Items per page"
          >
            {pageSizeOptions.map(opt => (
              <option key={opt} value={opt}>{opt}</option>
            ))}
          </select>
        </div>
      )}
    </nav>
  );
}
