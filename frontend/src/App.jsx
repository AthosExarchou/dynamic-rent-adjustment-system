import React from 'react';
import { RouterProvider } from 'react-router-dom';
import { AuthProvider } from './features/auth';
import router from './app/routes';

function App() {
  return (
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  );
}

export default App;
