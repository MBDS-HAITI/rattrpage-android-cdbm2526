// src/App.jsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import PageConnexion from './pages/PageConnexion';
import PageListeParcours from './pages/PageListeParcours';
import PageDetailParcours from './pages/PageDetailParcours';
import PageFormulaireParcours from './pages/PageFormulaireParcours';

function RouteProtegee({ children }) {
  const { estConnecte, chargement } = useAuth();

  if (chargement) {
    return <div className="chargement-page">Chargement...</div>;
  }
  if (!estConnecte) {
    return <Navigate to="/connexion" replace />;
  }
  return children;
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/connexion" element={<PageConnexion />} />

          <Route
            path="/parcours"
            element={
              <RouteProtegee>
                <PageListeParcours />
              </RouteProtegee>
            }
          />

          {/* Route specifique AVANT /parcours/:id, sinon "nouveau"
              serait interprete comme un identifiant de parcours. */}
          <Route
            path="/parcours/nouveau"
            element={
              <RouteProtegee>
                <PageFormulaireParcours />
              </RouteProtegee>
            }
          />

          <Route
            path="/parcours/:id"
            element={
              <RouteProtegee>
                <PageDetailParcours />
              </RouteProtegee>
            }
          />

          <Route
            path="/parcours/:id/modifier"
            element={
              <RouteProtegee>
                <PageFormulaireParcours />
              </RouteProtegee>
            }
          />

          <Route path="*" element={<Navigate to="/parcours" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
