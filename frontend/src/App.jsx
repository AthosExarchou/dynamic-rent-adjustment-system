import React from 'react';
import { RouterProvider } from 'react-router-dom';
import { AuthProvider } from './features/auth';
import { FavouritesProvider } from './features/favourites';
import router from './app/routes';

function App() {
  return (
    <AuthProvider>
      <FavouritesProvider>
        <RouterProvider router={router} />
      </FavouritesProvider>
    </AuthProvider>
  );
}

export default App;
