// src/App.jsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import PageConnexion from './pages/PageConnexion';
import PageListeParcours from './pages/PageListeParcours';
import PageDetailParcours from './pages/PageDetailParcours';
import PageFormulaireParcours from './pages/PageFormulaireParcours';
import PageCarteGenerale from './pages/PageCarteGenerale';
import PageDashboard from './pages/PageDashboard';
import PageModerationAvis from './pages/PageModerationAvis';
import PageUtilisateurs from './pages/PageUtilisateurs';
import ArrierePlanTraces from './components/ArrierePlanTraces';
import './App.css';

/**
 * Protege une route ET l'habille du meme fond decoratif que la page de
 * connexion, pour une identite visuelle coherente sur tout le back
 * office. Le fond est place une seule fois ici plutot que d'etre
 * duplique dans chacune des huit pages.
 */
function RouteProtegee({ children }) {
  const { estConnecte, chargement } = useAuth();

  if (chargement) {
    return <div className="chargement-page">Chargement...</div>;
  }
  if (!estConnecte) {
    return <Navigate to="/connexion" replace />;
  }

  return (
    <div className="mise-en-page-app">
      <ArrierePlanTraces />
      <div className="contenu-mise-en-page">{children}</div>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/connexion" element={<PageConnexion />} />

          <Route path="/parcours" element={<RouteProtegee><PageListeParcours /></RouteProtegee>} />
          <Route path="/dashboard" element={<RouteProtegee><PageDashboard /></RouteProtegee>} />
          <Route path="/moderation" element={<RouteProtegee><PageModerationAvis /></RouteProtegee>} />
          <Route path="/utilisateurs" element={<RouteProtegee><PageUtilisateurs /></RouteProtegee>} />
          <Route path="/carte" element={<RouteProtegee><PageCarteGenerale /></RouteProtegee>} />
          <Route path="/parcours/nouveau" element={<RouteProtegee><PageFormulaireParcours /></RouteProtegee>} />
          <Route path="/parcours/:id" element={<RouteProtegee><PageDetailParcours /></RouteProtegee>} />
          <Route path="/parcours/:id/modifier" element={<RouteProtegee><PageFormulaireParcours /></RouteProtegee>} />

          <Route path="*" element={<Navigate to="/parcours" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
