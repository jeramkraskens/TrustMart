// Website startup: mount React, enable URL navigation and provide shared login state.
// Read App.jsx next to see which screen is displayed for each address.
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import './index.css';
import App from './App.jsx';
import { AuthProvider } from './context/AuthContext.jsx';
import { NotificationProvider } from './context/NotificationContext.jsx';
import { authSession } from './api/client';
import { shouldOpenGuestHome } from './api/session';

// A restored dashboard tab must not reopen a previous role on a fresh run.
if (shouldOpenGuestHome(window.location.pathname, authSession.isFreshSession)) {
  window.history.replaceState(null, '', '/');
}

// Section: App startup — mount routing and shared account state.
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <NotificationProvider><App /></NotificationProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
);
