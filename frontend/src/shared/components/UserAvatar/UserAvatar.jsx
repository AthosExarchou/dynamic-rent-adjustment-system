import React from 'react';
import styles from './UserAvatar.module.css';

const PALETTE = [
  '#4f46e5',
  '#0369a1',
  '#047857',
  '#b45309',
  '#be123c',
  '#7e22ce',
  '#0f766e',
  '#1d4ed8',
  '#3f6212',
  '#713f12',
  '#831843',
  '#334155',
];

function getInitials(user) {
  const firstName = (user?.firstName || '').trim();
  const lastName = (user?.lastName || '').trim();

  if (firstName && lastName) {
    if (firstName.toLowerCase() === lastName.toLowerCase()) {
      return Array.from(firstName)[0].toUpperCase();
    }
    return (Array.from(firstName)[0] + Array.from(lastName)[0]).toUpperCase();
  }

  if (firstName || lastName) {
    return Array.from(firstName || lastName).slice(0, 2).join('').toUpperCase();
  }

  const username = (user?.username || '').trim();
  if (!username) return '?';

  const parts = username.split(/\s+/).filter(Boolean);
  if (parts.length > 1) {
    return (Array.from(parts[0])[0] + Array.from(parts[1])[0]).toUpperCase();
  }

  return Array.from(username).slice(0, 2).join('').toUpperCase();
}

const SMALL_AVATAR_MAX_SIZE = 24;
const ROUND_SHIFT_MAX_SIZE = 40;

const ROUND_INITIALS = new Set(['C', 'G', 'O', 'Q', 'S', 'U']);

function hasOnlyRoundInitials(user) {
  return Array.from(getInitials(user)).every(letter => ROUND_INITIALS.has(letter));
}

function getPaletteColor(user) {
  const key = String(user?.id ?? user?.email ?? user?.username ?? '');
  let hash = 0;
  for (let i = 0; i < key.length; i += 1) {
    hash = (hash * 31 + key.charCodeAt(i)) >>> 0;
  }
  return PALETTE[hash % PALETTE.length];
}

export default function UserAvatar({ user, size = 32, className = '', title }) {
  const fontSize = Math.max(10, Math.round(size * 0.4));

  let shiftClass = '';
  if (size <= SMALL_AVATAR_MAX_SIZE) {
    shiftClass = styles.smallShift;
  } else if (size <= ROUND_SHIFT_MAX_SIZE && hasOnlyRoundInitials(user)) {
    shiftClass = styles.roundShift;
  }

  return (
    <span
      className={`${styles.avatar} ${shiftClass} ${className}`}
      style={{
        width: size,
        height: size,
        fontSize,
        backgroundColor: getPaletteColor(user),
      }}
      title={title || user?.username || undefined}
      aria-hidden="true"
    >
      {getInitials(user)}
    </span>
  );
}
