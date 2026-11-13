#!/usr/bin/env node

/**
 * Database Seeder for DRAS
 *
 * Sets up the core accounts (admin, owner, tenant, user)
 * and seeds 1 active rental workflow (listing, approval, tenant application, lease, favourite).
 *
 * Usage: node scripts/seed-db.js
 */

const BASE_URL = process.env.APP_URL || process.argv[2] || 'http://localhost';
const API_URL = `${BASE_URL.replace(/\/+$/, '')}/api`;
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'adminPassword1!';

async function createClient(email, password) {
  const cookies = {};

  const saveCookies = (res) => {
    for (const sc of (res.headers.getSetCookie?.() || [])) {
      const [pair] = sc.split(';');
      const [k, v] = pair.split('=');
      if (k && v) cookies[k.trim()] = v.trim();
    }
  };

  const req = async (endpoint, options = {}) => {
    if (!cookies['XSRF-TOKEN-DRAS']) {
      const init = await fetch(`${API_URL}/listings`);
      saveCookies(init);
    }
    const headers = { ...options.headers };
    headers['Cookie'] = Object.entries(cookies).map(([k, v]) => `${k}=${v}`).join('; ');
    if (['POST', 'PUT', 'DELETE'].includes((options.method || 'GET').toUpperCase())) {
      headers['X-XSRF-TOKEN'] = cookies['XSRF-TOKEN-DRAS'] || '';
    }
    const res = await fetch(`${API_URL}${endpoint}`, { ...options, headers });
    saveCookies(res);
    return res;
  };

  // Try to register if account doesn't exist
  const username = email.split('@')[0];
  const params = new URLSearchParams({ username, email, password });
  await req('/saveUser', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: params.toString()
  });

  // Login
  const loginRes = await req('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });

  if (!loginRes.ok) {
    throw new Error(`Failed to log in as ${email}`);
  }

  const user = await loginRes.json();
  return { user, req };
}

async function seed() {
  console.log(`Seeding database at ${API_URL}...`);

  // Authenticate core accounts
  const admin = await createClient('admin@gmail.com', ADMIN_PASSWORD);
  const owner = await createClient('owner@gmail.com', 'ownerPassword1!');
  const tenant = await createClient('tenant@gmail.com', 'tenantPassword1!');
  const user = await createClient('user@gmail.com', 'userPassword1!');
  console.log('Accounts ready: admin, owner, tenant, user');

  // Ensure owner & tenant profiles
  if (!owner.user.roles.includes('OWNER')) {
    await owner.req('/owner/new', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: owner.user.id,
        firstName: 'Alexander',
        lastName: 'Owner',
        phoneNumber: `+30 690 ${Date.now().toString().slice(-7)}`
      })
    });
  }

  if (!tenant.user.roles.includes('TENANT')) {
    await admin.req('/tenant/new', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: tenant.user.id,
        firstName: 'Thalia',
        lastName: 'Tenant',
        phoneNumber: `+30 690 ${Date.now().toString().slice(-7)}`
      })
    });
  }

  // Create 1 listing if owner has none
  const myListingsRes = await owner.req('/listings/mylisting');
  const myListings = await myListingsRes.json().catch(() => []);
  let listingId = myListings[0]?.id;

  if (!listingId) {
    const fd = new FormData();
    fd.append('title', 'Athens Central Apartment');
    fd.append('subtitle', 'Modern 2-bedroom home in central Athens');
    fd.append('description', 'Renovated apartment with balcony, A/C, and modern appliances.');
    fd.append('price', '750');
    fd.append('pricePerM2', '10');
    fd.append('address', 'Metropoleos 20, Athens');
    fd.append('sizeM2', '75');
    fd.append('propertyType', 'APARTMENT');
    fd.append('rentalDuration', 'LONG_TERM');
    fd.append('floor', '2');
    fd.append('yearBuilt', '2019');
    fd.append('bedrooms', '2');
    fd.append('bathrooms', '1');

    await owner.req('/listings/new', { method: 'POST', body: fd });
    const updated = await (await owner.req('/listings/mylisting')).json();
    listingId = updated[0]?.id;
    console.log(`Created listing #${listingId} ("Athens Central Apartment")`);
  }

  // Admin approves listing if pending
  await admin.req(`/listings/approve/${listingId}`, { method: 'POST' });

  // Tenant applies & Owner approves lease (only if not already leased)
  const isAlreadyLeased = myListings[0]?.rentalStatus === 'RENTED' || myListings[0]?.status === 'RENTED';
  if (!isAlreadyLeased) {
    await tenant.req(`/tenant/rent/${listingId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({})
    });

    const appsRes = await owner.req(`/listings/${listingId}/applications`);
    const appsData = await appsRes.json().catch(() => ({}));
    const applicant = appsData.applicants?.[0];
    if (applicant) {
      await owner.req(`/owner/listings/${listingId}/approveApplicant/${applicant.id}`, { method: 'POST' });
      console.log(`Leased listing #${listingId} to tenant@gmail.com`);
    }
  }

  // User favourites the listing (only if not already favourited)
  const favIdsRes = await user.req('/favourites/ids');
  const favIds = await favIdsRes.json().catch(() => []);
  if (Array.isArray(favIds) && !favIds.includes(listingId)) {
    await user.req(`/favourites/${listingId}`, { method: 'POST' });
    console.log(`User favourited listing #${listingId}`);
  }

  console.log('Database seeded successfully.');
}

seed().catch((err) => {
  console.error('Seeding failed:', err.message);
  process.exit(1);
});
