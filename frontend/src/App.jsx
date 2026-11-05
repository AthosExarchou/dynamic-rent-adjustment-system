import React from 'react';
import { RouterProvider } from 'react-router-dom';
import { AuthProvider } from './features/auth';
import { FavouritesProvider } from './features/favourites';
import { ToastProvider } from './shared/components/Toast';
import router from './app/routes';

function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <FavouritesProvider>
          <RouterProvider router={router} />
        </FavouritesProvider>
      </ToastProvider>
    </AuthProvider>
  );
}

export default App;
