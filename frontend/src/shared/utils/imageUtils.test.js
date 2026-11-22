import assert from 'node:assert/strict';
import test from 'node:test';

import {
  DEFAULT_LISTING_IMAGE,
  SECONDARY_LISTING_IMAGE,
  getImageUrl,
  getListingImageFallback,
  handleListingImageError
} from './imageUtils.js';

function fakeImageEvent(src) {
  const target = {
    src,
    dataset: {},
    getAttribute() {
      return target.src;
    }
  };
  return { target };
}

test('demo assets resolve to the frontend route', () => {
  assert.equal(getImageUrl('images/demo/image1.jpg'), '/images/demo/image1.jpg');
});

test('remote URLs resolve to the local placeholder', () => {
  assert.equal(getImageUrl('https://cdn.example.com/photo.jpg'), DEFAULT_LISTING_IMAGE);
  assert.equal(getImageUrl('//cdn.example.com/photo.jpg'), DEFAULT_LISTING_IMAGE);
});

test('fallback chain tries the other demo file once', () => {
  assert.equal(getListingImageFallback('data/images/1/a.jpg', 0), DEFAULT_LISTING_IMAGE);
  assert.equal(getListingImageFallback('data/images/1/a.jpg', 1), SECONDARY_LISTING_IMAGE);
  assert.equal(getListingImageFallback('data/images/1/a.jpg', 2), null);
});

test('fallback chain handles demo files used as real images', () => {
  assert.equal(getListingImageFallback(SECONDARY_LISTING_IMAGE, 0), DEFAULT_LISTING_IMAGE);
  assert.equal(getListingImageFallback(SECONDARY_LISTING_IMAGE, 1), null);
});

test('error handler stops after two attempts', () => {
  const event = fakeImageEvent('/api/images/1/a.jpg');
  handleListingImageError(event);
  assert.equal(event.target.src, DEFAULT_LISTING_IMAGE);
  handleListingImageError(event);
  assert.equal(event.target.src, SECONDARY_LISTING_IMAGE);
  handleListingImageError(event);
  assert.equal(event.target.src, SECONDARY_LISTING_IMAGE);
});
