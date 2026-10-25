import React from 'react';
import { Heart } from 'lucide-react';
import { useFavourites } from '../context/FavouritesContext';
import styles from './FavouriteButton.module.css';

export default function FavouriteButton({
  listing,
  variant = 'card',
  size = 18,
  showText = false,
  className = '',
}) {
  const { isFavourite, toggleFavourite } = useFavourites();

  if (!listing || !listing.id) return null;

  const active = isFavourite(listing.id);

  const handleClick = (e) => {
    e.preventDefault();
    e.stopPropagation();
    toggleFavourite(listing);
  };

  const label = active ? 'Remove from favourites' : 'Add to favourites';
  const text = active ? 'Saved in Favourites' : 'Save to Favourites';

  if (variant === 'button') {
    return (
      <button
        type="button"
        onClick={handleClick}
        className={`${styles.actionBtn} ${active ? styles.active : ''} ${className}`}
        aria-label={label}
        aria-pressed={active}
        title={label}
      >
        <Heart
          size={size}
          className={`${styles.heartIcon} ${active ? styles.heartFilled : ''}`}
        />
        {showText && <span>{text}</span>}
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      className={`${styles.cardBtn} ${active ? styles.active : ''} ${className}`}
      aria-label={label}
      aria-pressed={active}
      title={label}
    >
      <Heart
        size={size}
        className={`${styles.heartIcon} ${active ? styles.heartFilled : ''}`}
      />
    </button>
  );
}
