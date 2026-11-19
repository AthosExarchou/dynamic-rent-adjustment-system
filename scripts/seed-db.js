#!/usr/bin/env node

/**
 * Database Seeder for DRAS
 *
 * Sets up the core accounts and seeds listings that cover the main product
 * scenarios: approved, pending, rejected, disabled, rented, and unassigned
 * listings; two owners; multiple tenants; one pending application; one canceled
 * rental; favourites; uploaded images; and an externally imported listing.
 *
 * The script is re-runnable: listings are matched by title, accounts by email,
 * and applications by tenant and listing. Existing data is not duplicated, and
 * listing states are only applied to listings created by the current run.
 *
 * Usage:
 *   node scripts/seed-db.js [base-url]
 *
 * Environment:
 *   ADMIN_PASSWORD    current admin password (defaults to adminPassword1!)
 *   SCRAPER_API_KEY   required to seed the external listing, skipped if unset
 */

const fs = require('node:fs');
const path = require('node:path');

const BASE_URL = process.env.APP_URL || process.argv[2] || 'http://localhost';
const API_URL = `${BASE_URL.replace(/\/+$/, '')}/api`;
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'adminPassword1!';
const SCRAPER_API_KEY = process.env.SCRAPER_API_KEY || '';
const IMAGE_DIR = path.resolve(__dirname, '../frontend/public/images/demo');

/**
 * Seed listings. `owner` selects the account that creates the listing,
 * `state` is the moderation state applied after creation, and `imageFiles`
 * lists demo images from frontend/public/images/demo to upload.
 */
const LISTINGS = [
  {
    title: 'Athens Central Apartment',
    subtitle: 'Modern 2-bedroom home in central Athens',
    description: 'Renovated apartment with balcony, A/C, and modern appliances.',
    price: 750,
    pricePerM2: 10,
    address: 'Metropoleos 20, Athens',
    sizeM2: 75,
    propertyType: 'APARTMENT',
    rentalDuration: 'LONG_TERM',
    floor: 2,
    yearBuilt: 2019,
    bedrooms: 2,
    bathrooms: 1,
    owner: 'owner',
    state: 'RENTED'
  },
  {
    title: 'Kolonaki Garden Flat',
    subtitle: 'Quiet flat with a private courtyard in Kolonaki',
    description: 'Two-bedroom flat with hardwood floors, a renovated kitchen, and a shaded courtyard.',
    price: 1200,
    pricePerM2: 13,
    address: 'Skoufa 45, Kolonaki, Athens',
    sizeM2: 95,
    propertyType: 'APARTMENT',
    rentalDuration: 'LONG_TERM',
    floor: 3,
    yearBuilt: 2005,
    bedrooms: 2,
    bathrooms: 1,
    owner: 'owner',
    state: 'APPROVED'
  },
  {
    title: 'Plaka Loft with Acropolis View',
    subtitle: 'Top-floor loft under the Acropolis',
    description: 'Open-plan loft with exposed beams, a large terrace, and a direct view of the Acropolis.',
    price: 900,
    pricePerM2: 15,
    address: 'Adrianou 110, Plaka, Athens',
    sizeM2: 60,
    propertyType: 'LOFT',
    rentalDuration: 'LONG_TERM',
    floor: 4,
    yearBuilt: 1998,
    bedrooms: 1,
    bathrooms: 1,
    owner: 'owner',
    state: 'APPROVED'
  },
  {
    title: 'Pangrati Family Maisonette',
    subtitle: 'Three-bedroom maisonette near Pangrati Square',
    description: 'Two-level home with a private garden, storage room, and parking space.',
    price: 1400,
    pricePerM2: 10,
    address: 'Eratosthenous 32, Pangrati, Athens',
    sizeM2: 140,
    propertyType: 'MAISONETTE',
    rentalDuration: 'LONG_TERM',
    floor: 1,
    yearBuilt: 2012,
    bedrooms: 3,
    bathrooms: 2,
    owner: 'owner',
    state: 'APPROVED'
  },
  {
    title: 'Koukaki Studio',
    subtitle: 'Compact studio a short walk from Syngrou Fix',
    description: 'Furnished studio with a kitchenette and a small balcony, close to the metro.',
    price: 520,
    pricePerM2: 14,
    address: 'Veikou 18, Koukaki, Athens',
    sizeM2: 38,
    propertyType: 'STUDIO',
    rentalDuration: 'LONG_TERM',
    floor: 2,
    yearBuilt: 1985,
    bedrooms: 1,
    bathrooms: 1,
    owner: 'owner',
    state: 'APPROVED'
  },
  {
    title: 'Exarchia Two-Bedroom',
    subtitle: 'Bright apartment on a quiet Exarchia street',
    description: 'Two bedrooms, a separate living room, and a long balcony facing the street.',
    price: 700,
    pricePerM2: 9,
    address: 'Kallidromiou 54, Exarchia, Athens',
    sizeM2: 78,
    propertyType: 'APARTMENT',
    rentalDuration: 'FIXED_TERM',
    floor: 1,
    yearBuilt: 1978,
    bedrooms: 2,
    bathrooms: 1,
    owner: 'owner',
    state: 'APPROVED'
  },
  {
    title: 'Glyfada Seaside House',
    subtitle: 'Four-bedroom house near the Glyfada marina',
    description: 'Detached house with a garden, covered parking, and sea views from the upper floor.',
    price: 1800,
    pricePerM2: 11,
    address: 'Poseidonos 88, Glyfada, Athens',
    sizeM2: 165,
    propertyType: 'HOUSE',
    rentalDuration: 'LONG_TERM',
    floor: 0,
    yearBuilt: 2015,
    bedrooms: 4,
    bathrooms: 2,
    owner: 'owner',
    state: 'APPROVED'
  },
  {
    title: 'Ampelokipi Modern Apartment',
    subtitle: 'Renovated apartment near Ampelokipi metro',
    description: 'Two bedrooms, a large living room, and a balcony with a city view.',
    price: 880,
    pricePerM2: 11,
    address: 'Panormou 72, Ampelokipi, Athens',
    sizeM2: 80,
    propertyType: 'APARTMENT',
    rentalDuration: 'LONG_TERM',
    floor: 4,
    yearBuilt: 2010,
    bedrooms: 2,
    bathrooms: 1,
    owner: 'owner',
    state: 'APPROVED',
    imageFiles: ['image2.jpg', 'image3.jpg', 'image4.jpg']
  },
  {
    title: 'Petralona Vintage Flat',
    subtitle: 'Character flat in a quiet Petralona street',
    description: 'High ceilings, wooden floors, and a small back yard close to the train station.',
    price: 720,
    pricePerM2: 10,
    address: 'Tritonos 19, Petralona, Athens',
    sizeM2: 72,
    propertyType: 'APARTMENT',
    rentalDuration: 'FIXED_TERM',
    floor: 1,
    yearBuilt: 1968,
    bedrooms: 2,
    bathrooms: 1,
    owner: 'owner',
    state: 'APPROVED',
    imageFiles: ['image5.jpg', 'image6.jpg']
  },
  {
    title: 'Ano Patisia Value Apartment',
    subtitle: 'Practical apartment below the market average',
    description: 'Two bedrooms, a closed balcony, and direct access to the Ano Patisia metro station.',
    price: 480,
    pricePerM2: 6,
    address: 'Acharnon 310, Ano Patisia, Athens',
    sizeM2: 80,
    propertyType: 'APARTMENT',
    rentalDuration: 'INDEFINITE',
    floor: 3,
    yearBuilt: 1975,
    bedrooms: 2,
    bathrooms: 1,
    owner: 'owner',
    state: 'APPROVED'
  },
  {
    title: 'Marousi Business Studio',
    subtitle: 'Compact studio near the Marousi business district',
    description: 'Furnished studio with a kitchenette, ideal for a single tenant working nearby.',
    price: 550,
    pricePerM2: 12,
    address: 'Kifisias 44, Marousi, Athens',
    sizeM2: 45,
    propertyType: 'STUDIO',
    rentalDuration: 'OTHER',
    floor: 5,
    yearBuilt: 2003,
    bedrooms: 1,
    bathrooms: 1,
    owner: 'owner2',
    state: 'APPROVED'
  },
  {
    title: 'Zografou Student Studio',
    subtitle: 'Studio close to the university campus',
    description: 'Small studio with a kitchenette and a balcony, a short walk from the campus gates.',
    price: 480,
    pricePerM2: 14,
    address: 'Olof Palme 15, Zografou, Athens',
    sizeM2: 35,
    propertyType: 'STUDIO',
    rentalDuration: 'SHORT_TERM',
    floor: 2,
    yearBuilt: 1995,
    bedrooms: 1,
    bathrooms: 1,
    owner: 'owner2',
    state: 'UNASSIGNED'
  },
  {
    title: 'Nea Smyrni Bright Apartment',
    subtitle: 'Third-floor apartment near Nea Smyrni square',
    description: 'Two bedrooms, a renovated bathroom, and a balcony with afternoon sun.',
    price: 800,
    pricePerM2: 9,
    address: 'Omirou 24, Nea Smyrni, Athens',
    sizeM2: 85,
    propertyType: 'APARTMENT',
    rentalDuration: 'LONG_TERM',
    floor: 3,
    yearBuilt: 2001,
    bedrooms: 2,
    bathrooms: 1,
    owner: 'owner',
    state: 'PENDING'
  },
  {
    title: 'Chalandri Family Apartment',
    subtitle: 'Spacious apartment close to Chalandri metro',
    description: 'Three bedrooms, two bathrooms, a storage room, and a covered parking space.',
    price: 950,
    pricePerM2: 9,
    address: 'Pentelis 61, Chalandri, Athens',
    sizeM2: 110,
    propertyType: 'APARTMENT',
    rentalDuration: 'LONG_TERM',
    floor: 2,
    yearBuilt: 2008,
    bedrooms: 3,
    bathrooms: 2,
    owner: 'owner',
    state: 'PENDING'
  },
  {
    title: 'Thisio Sunny Studio',
    subtitle: 'Furnished studio with a view of the Acropolis',
    description: 'Compact studio with a kitchenette and a shared roof terrace near Thisio station.',
    price: 560,
    pricePerM2: 13,
    address: 'Apostolou Pavlou 37, Thisio, Athens',
    sizeM2: 42,
    propertyType: 'STUDIO',
    rentalDuration: 'SHORT_TERM',
    floor: 3,
    yearBuilt: 1990,
    bedrooms: 1,
    bathrooms: 1,
    owner: 'owner',
    state: 'PENDING'
  },
  {
    title: 'Metaxourgeio Renovation Project',
    subtitle: 'One-bedroom flat in need of renovation',
    description: 'Ground-floor flat with original tiles and a small courtyard, offered as-is.',
    price: 450,
    pricePerM2: 6,
    address: 'Leonidou 12, Metaxourgeio, Athens',
    sizeM2: 70,
    propertyType: 'APARTMENT',
    rentalDuration: 'LONG_TERM',
    floor: 0,
    yearBuilt: 1965,
    bedrooms: 1,
    bathrooms: 1,
    owner: 'owner',
    state: 'REJECTED',
    rejectionReason: 'Photos and a floor plan are required before this listing can be published.'
  },
  {
    title: 'Kypseli Classic Apartment',
    subtitle: 'High-ceiling apartment in a 1970s building',
    description: 'Two bedrooms, a separate dining room, and a long balcony facing the street.',
    price: 650,
    pricePerM2: 7,
    address: 'Fokionos Negri 48, Kypseli, Athens',
    sizeM2: 90,
    propertyType: 'APARTMENT',
    rentalDuration: 'LONG_TERM',
    floor: 2,
    yearBuilt: 1972,
    bedrooms: 2,
    bathrooms: 1,
    owner: 'owner',
    state: 'DISABLED'
  },
  {
    title: 'Ilisia Quiet Apartment',
    subtitle: 'Quiet two-bedroom flat near Ilisia',
    description: 'Second-floor apartment with a closed balcony, updated wiring, and a lift.',
    price: 620,
    pricePerM2: 9,
    address: 'Michalakopoulou 96, Ilisia, Athens',
    sizeM2: 68,
    propertyType: 'APARTMENT',
    rentalDuration: 'LONG_TERM',
    floor: 2,
    yearBuilt: 1988,
    bedrooms: 2,
    bathrooms: 1,
    owner: 'owner',
    state: 'DISABLED',
    imageFiles: ['image9.jpg', 'image10.jpg']
  }
];

/**
 * External listing pushed through the scraper import endpoint. It is assigned
 * to the system owner, marked external, and auto-approved by the backend.
 */
const EXTERNAL_LISTING = {
  title: 'Syntagma Scraped Apartment',
  subtitle: 'Imported from an external listing site',
  description: 'Two-bedroom apartment near Syntagma Square, imported by the analytics pipeline.',
  price: 850,
  pricePerM2: 12,
  address: 'Filellinon 9, Syntagma, Athens',
  sizeM2: 70,
  floor: 2,
  yearBuilt: 2004,
  bedrooms: 2,
  bathrooms: 1,
  propertyType: 'APARTMENT',
  rentalDuration: 'LONG_TERM',
  sourceUrl: 'https://example.com/dras-seed/syntagma-scraped-apartment'
};

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

async function ensureOwnerProfile(client) {
  if (client.user.roles.includes('OWNER')) return;

  const res = await client.req('/owner/new', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      userId: client.user.id,
      firstName: 'Alexander',
      lastName: 'Owner',
      phoneNumber: `+30 690 ${Date.now().toString().slice(-7)}`
    })
  });

  if (!res.ok) {
    throw new Error(`Failed to create owner profile (${res.status})`);
  }
}

function toFormData(listing) {
  const fd = new FormData();
  const skipped = new Set(['state', 'rejectionReason', 'owner', 'imageFiles']);

  for (const [key, value] of Object.entries(listing)) {
    if (skipped.has(key)) continue;
    if (value === undefined || value === null || value === '') continue;
    fd.append(key, String(value));
  }

  for (const name of listing.imageFiles || []) {
    const filePath = path.join(IMAGE_DIR, name);
    if (!fs.existsSync(filePath)) {
      console.warn(`Skipping missing image file: ${filePath}`);
      continue;
    }
    const blob = new Blob([fs.readFileSync(filePath)], { type: 'image/jpeg' });
    fd.append('imageFiles', blob, name);
  }

  return fd;
}

async function fetchAllListings() {
  const res = await fetch(`${API_URL}/listings`);
  if (!res.ok) {
    throw new Error(`Failed to load listings (${res.status})`);
  }
  const listings = await res.json();
  return new Map(listings.map((listing) => [listing.title, listing]));
}

async function fetchTenantInfo(client) {
  const res = await client.req('/tenant/applications');
  if (!res.ok) {
    throw new Error(`Failed to load tenant applications (${res.status})`);
  }
  return res.json().catch(() => ({}));
}

async function applyForListing(client, listingId, profile) {
  return client.req(`/tenant/rent/${listingId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(profile)
  });
}

async function markOneNotificationRead(client, label) {
  const res = await client.req('/notifications');
  if (!res.ok) return;

  const notifications = await res.json().catch(() => []);
  if (!Array.isArray(notifications)) return;

  const unread = notifications.filter((notification) => notification.read === false);
  const hasRead = notifications.some((notification) => notification.read === true);

  // Keep the mix stable on reruns: only mark one when none are read yet
  if (hasRead || unread.length <= 1) return;

  const oldest = unread[unread.length - 1];
  const markRes = await client.req(`/notifications/${oldest.id}/read`, { method: 'PUT' });
  if (markRes.ok) {
    console.log(`Marked one notification read for ${label}`);
  }
}

async function importExternalListing(images) {
  if (!SCRAPER_API_KEY) {
    console.log('Skipping external listing: SCRAPER_API_KEY is not set');
    return;
  }

  const payload = {
    ...EXTERNAL_LISTING,
    images: images && images.length > 0 ? images.slice(0, 2) : []
  };

  const res = await fetch(`${API_URL}/external-import/listings`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Scraper-Key': SCRAPER_API_KEY
    },
    body: JSON.stringify([payload])
  });

  if (!res.ok) {
    throw new Error(`External listing import failed (${res.status}): ${await res.text().catch(() => '')}`);
  }

  const summary = await res.json();
  console.log(`External listing import: received ${summary.received}, imported ${summary.imported}, skipped ${summary.skipped}`);
}

async function seed() {
  console.log(`Seeding database at ${API_URL}...`);

  // Authenticate core accounts
  const admin = await createClient('admin@gmail.com', ADMIN_PASSWORD);
  const owner = await createClient('owner@gmail.com', 'ownerPassword1!');
  const owner2 = await createClient('owner2@gmail.com', 'owner2Password1!');
  const tenant = await createClient('tenant@gmail.com', 'tenantPassword1!');
  const tenant2 = await createClient('tenant2@gmail.com', 'tenant2Password1!');
  const tenant3 = await createClient('tenant3@gmail.com', 'tenant3Password1!');
  const user = await createClient('user@gmail.com', 'userPassword1!');

  const owners = { owner, owner2 };
  console.log('Accounts ready: admin, owner, owner2, tenant, tenant2, tenant3, user');

  // Ensure owner profiles
  await ensureOwnerProfile(owner);
  await ensureOwnerProfile(owner2);

  // Ensure the primary tenant profile (tenants 2 and 3 get one on application)
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

  // Create any missing listings, matched by title
  let listings = await fetchAllListings();
  const createdTitles = new Set();

  for (const listing of LISTINGS) {
    if (listings.has(listing.title)) continue;

    const client = owners[listing.owner || 'owner'];
    const res = await client.req('/listings/new', {
      method: 'POST',
      body: toFormData(listing)
    });

    if (!res.ok) {
      const body = await res.text().catch(() => '');
      throw new Error(`Failed to create "${listing.title}" (${res.status}): ${body}`);
    }

    createdTitles.add(listing.title);
    console.log(`Created listing "${listing.title}"`);
  }

  listings = await fetchAllListings();
  console.log(`Listings ready: ${listings.size} (${createdTitles.size} created this run)`);

  // Apply the fixture's moderation state to listings created in this run.
  // Existing listings are left alone.
  for (const spec of LISTINGS) {
    if (!createdTitles.has(spec.title)) continue;

    const current = listings.get(spec.title);
    if (!current) continue;

    const state = spec.state || 'APPROVED';
    const client = owners[spec.owner || 'owner'];

    if (state === 'PENDING') {
      console.log(`Left listing #${current.id} ("${spec.title}") pending review`);
      continue;
    }

    if (state === 'REJECTED') {
      const res = await admin.req(`/listings/reject/${current.id}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          reason: spec.rejectionReason || 'Listing does not meet publication criteria.'
        })
      });
      if (!res.ok) {
        throw new Error(`Failed to reject "${spec.title}" (${res.status})`);
      }
      console.log(`Rejected listing #${current.id} ("${spec.title}")`);
      continue;
    }

    const approveRes = await admin.req(`/listings/approve/${current.id}`, { method: 'POST' });
    if (!approveRes.ok) {
      throw new Error(`Failed to approve "${spec.title}" (${approveRes.status})`);
    }
    console.log(`Approved listing #${current.id} ("${spec.title}")`);

    if (state === 'DISABLED') {
      const disableRes = await client.req(`/listings/disable/${current.id}`, { method: 'POST' });
      if (!disableRes.ok) {
        throw new Error(`Failed to disable "${spec.title}" (${disableRes.status})`);
      }
      console.log(`Disabled listing #${current.id} ("${spec.title}")`);
    }

    if (state === 'UNASSIGNED') {
      const unassignRes = await admin.req(`/listings/unassign/owner/${current.id}`, { method: 'POST' });
      if (!unassignRes.ok) {
        throw new Error(`Failed to unassign owner from "${spec.title}" (${unassignRes.status})`);
      }
      console.log(`Unassigned owner from listing #${current.id} ("${spec.title}")`);
    }
  }

  listings = await fetchAllListings();

  // Lease the designated listing unless the tenant already rents one
  const leaseSpec = LISTINGS.find((listing) => listing.state === 'RENTED');
  const leaseTarget = leaseSpec ? listings.get(leaseSpec.title) : null;

  if (leaseTarget) {
    const tenantInfo = await fetchTenantInfo(tenant);
    const alreadyRenting = Boolean(tenantInfo.rentingListing) || tenantInfo.rentalStatus === 'RENTING';

    if (!alreadyRenting && !leaseTarget.rented && leaseTarget.status === 'APPROVED') {
      const applyRes = await applyForListing(tenant, leaseTarget.id, {});
      if (!applyRes.ok) {
        throw new Error(`Failed to apply for listing #${leaseTarget.id} (${applyRes.status})`);
      }

      const ownerAppsRes = await owner.req(`/listings/${leaseTarget.id}/applications`);
      const ownerApps = await ownerAppsRes.json().catch(() => ({}));
      const applicant = ownerApps.applicants?.[0];

      if (applicant) {
        await owner.req(`/owner/listings/${leaseTarget.id}/approveApplicant/${applicant.id}`, { method: 'POST' });
        console.log(`Leased listing #${leaseTarget.id} to tenant@gmail.com`);
      }
    }
  }

  // Second tenant: leave one application pending owner review
  const pendingTarget = listings.get('Kolonaki Garden Flat');
  if (pendingTarget) {
    const tenantInfo = await fetchTenantInfo(tenant2);
    const applied = Array.isArray(tenantInfo.appliedListingIds)
      && tenantInfo.appliedListingIds.includes(pendingTarget.id);

    if (!tenantInfo.rentingListing && !applied) {
      const res = await applyForListing(tenant2, pendingTarget.id, {
        firstName: 'Nikos',
        lastName: 'Applicant',
        phoneNumber: '+30 691 0000002'
      });

      if (!res.ok) {
        throw new Error(`Failed to create pending application (${res.status})`);
      }
      console.log(`Left tenant2 application pending on listing #${pendingTarget.id} ("${pendingTarget.title}")`);
    }
  }

  // Third tenant: lease a listing, then unassign the tenant to produce CANCELED
  const cancelTarget = listings.get('Petralona Vintage Flat');
  if (cancelTarget) {
    const tenantInfo = await fetchTenantInfo(tenant3);

    if (tenantInfo.rentalStatus === 'CANCELED' && !tenantInfo.rentingListing) {
      console.log('Tenant3 already has a canceled rental');
    } else if (tenantInfo.rentingListing) {
      if (tenantInfo.rentingListing.listingId === cancelTarget.id) {
        const res = await owner.req(`/listings/unassign/tenant/${cancelTarget.id}`, { method: 'POST' });
        if (!res.ok) {
          throw new Error(`Failed to cancel tenant3 rental (${res.status})`);
        }
        console.log(`Canceled tenant3 rental on listing #${cancelTarget.id}`);
      }
    } else {
      const applied = Array.isArray(tenantInfo.appliedListingIds)
        && tenantInfo.appliedListingIds.includes(cancelTarget.id);

      if (!applied) {
        const applyRes = await applyForListing(tenant3, cancelTarget.id, {
          firstName: 'Maria',
          lastName: 'Canceled',
          phoneNumber: '+30 691 0000003'
        });
        if (!applyRes.ok) {
          throw new Error(`Failed to apply as tenant3 (${applyRes.status})`);
        }
      }

      const ownerAppsRes = await owner.req(`/listings/${cancelTarget.id}/applications`);
      const ownerApps = await ownerAppsRes.json().catch(() => ({}));
      const applicant = (ownerApps.applicants || []).find((a) => a.email === 'tenant3@gmail.com');

      if (!applicant) {
        throw new Error('Tenant3 application not found after applying');
      }

      const approveRes = await owner.req(
        `/owner/listings/${cancelTarget.id}/approveApplicant/${applicant.id}`,
        { method: 'POST' }
      );
      if (!approveRes.ok) {
        throw new Error(`Failed to lease listing #${cancelTarget.id} to tenant3 (${approveRes.status})`);
      }

      const unassignRes = await owner.req(`/listings/unassign/tenant/${cancelTarget.id}`, { method: 'POST' });
      if (!unassignRes.ok) {
        throw new Error(`Failed to cancel tenant3 rental (${unassignRes.status})`);
      }
      console.log(`Canceled tenant3 rental on listing #${cancelTarget.id} ("${cancelTarget.title}")`);
    }
  }

  // External listing through the scraper import path, reusing an uploaded image
  const imageSource = listings.get('Ampelokipi Modern Apartment');
  await importExternalListing(imageSource?.images || []);

  listings = await fetchAllListings();

  // Favourite every approved, available seed listing
  const favIdsRes = await user.req('/favourites/ids');
  const favIds = await favIdsRes.json().catch(() => []);
  const favourites = Array.isArray(favIds) ? favIds : [];

  for (const spec of LISTINGS) {
    const current = listings.get(spec.title);
    if (!current || current.rented || current.status !== 'APPROVED') continue;
    if (favourites.includes(current.id)) continue;

    const res = await user.req(`/favourites/${current.id}`, { method: 'POST' });
    if (res.ok) {
      favourites.push(current.id);
      console.log(`User favourited listing #${current.id} ("${spec.title}")`);
    }
  }

  // Leave a mix of read and unread in-app notifications
  await markOneNotificationRead(owner, 'owner@gmail.com');
  await markOneNotificationRead(tenant, 'tenant@gmail.com');
  await markOneNotificationRead(owner2, 'owner2@gmail.com');
  await markOneNotificationRead(tenant2, 'tenant2@gmail.com');
  await markOneNotificationRead(tenant3, 'tenant3@gmail.com');

  console.log('Database seeded successfully.');
}

seed().catch((err) => {
  console.error('Seeding failed:', err.message);
  process.exit(1);
});
