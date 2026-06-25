import React from 'react';

function App() {
  return (
    <div className="container" style={{ padding: 'var(--spacing-8) var(--spacing-4)' }}>
      <header style={{ marginBottom: 'var(--spacing-8)', textAlign: 'center' }}>
        <h1 style={{ color: 'var(--color-primary-600)', fontSize: '2.5rem', marginBottom: 'var(--spacing-2)' }}>
          Dynamic Rent Adjustment System
        </h1>
        <p style={{ color: 'var(--color-neutral-800)', fontSize: '1.125rem' }}>
          Frontend Architecture Skeleton Initialized
        </p>
      </header>
      
      <main>
        <section style={{ 
          backgroundColor: '#fff', 
          padding: 'var(--spacing-8)', 
          borderRadius: 'var(--radius-lg)', 
          boxShadow: 'var(--shadow-md)' 
        }}>
          <h2 style={{ marginBottom: 'var(--spacing-4)' }}>Domain-Driven Features Skeleton</h2>
          <ul style={{ listStyle: 'none', display: 'grid', gap: 'var(--spacing-4)', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))' }}>
            {['Auth', 'Users', 'Owners', 'Tenants', 'Listings'].map((feature) => (
              <li key={feature} style={{ 
                padding: 'var(--spacing-4)', 
                backgroundColor: 'var(--color-neutral-50)', 
                border: '1px solid var(--color-neutral-100)',
                borderRadius: 'var(--radius-md)',
                display: 'flex',
                alignItems: 'center',
                gap: 'var(--spacing-2)'
              }}>
                <div style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: 'var(--color-primary-500)' }} />
                <strong>{feature} Domain</strong>
              </li>
            ))}
          </ul>
        </section>
      </main>
    </div>
  );
}

export default App;
