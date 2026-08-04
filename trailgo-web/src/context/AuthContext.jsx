// src/context/AuthContext.jsx
//
// Contexte React : rend l'utilisateur connecte disponible dans TOUTE
// l'application, sans avoir a le transmettre manuellement de composant
// en composant ("prop drilling").
//
// C'est l'equivalent cote React de ce que SecurityContextHolder fait
// cote Spring : une source unique de verite sur "qui est connecte".

import { createContext, useContext, useState, useEffect } from 'react';
import { connecter as appelConnexion } from '../api/authApi';

const AuthContext = createContext(null);

const CLE_JETON = 'trailgo_jeton';
const CLE_UTILISATEUR = 'trailgo_utilisateur';

export function AuthProvider({ children }) {
  const [utilisateur, setUtilisateur] = useState(null);
  const [chargement, setChargement] = useState(true);

  // Au premier chargement de l'application, on relit le localStorage :
  // si un jeton valide y est deja stocke (session precedente), on
  // reconnecte l'utilisateur sans qu'il ait a retaper ses identifiants.
  useEffect(() => {
    const jeton = localStorage.getItem(CLE_JETON);
    const utilisateurStocke = localStorage.getItem(CLE_UTILISATEUR);

    if (jeton && utilisateurStocke) {
      try {
        setUtilisateur(JSON.parse(utilisateurStocke));
      } catch {
        // Donnees corrompues : on nettoie plutot que de planter.
        localStorage.removeItem(CLE_JETON);
        localStorage.removeItem(CLE_UTILISATEUR);
      }
    }
    setChargement(false);
  }, []);

  async function connecter(email, motDePasse) {
    const reponse = await appelConnexion(email, motDePasse);

    const donneesUtilisateur = {
      id: reponse.id,
      email: reponse.email,
      nom: reponse.nom,
      role: reponse.role,
    };

    localStorage.setItem(CLE_JETON, reponse.jeton);
    localStorage.setItem(CLE_UTILISATEUR, JSON.stringify(donneesUtilisateur));
    setUtilisateur(donneesUtilisateur);

    return donneesUtilisateur;
  }

  function deconnecter() {
    localStorage.removeItem(CLE_JETON);
    localStorage.removeItem(CLE_UTILISATEUR);
    setUtilisateur(null);
  }

  const estConnecte = utilisateur !== null;
  const estAdmin = utilisateur?.role === 'ADMIN';

  const valeur = { utilisateur, estConnecte, estAdmin, chargement, connecter, deconnecter };

  return <AuthContext.Provider value={valeur}>{children}</AuthContext.Provider>;
}

/**
 * Hook d'acces au contexte. Toujours utiliser ce hook plutot que
 * useContext(AuthContext) directement : il verifie que le composant
 * appelant est bien a l'interieur d'un AuthProvider, et donne un
 * message d'erreur clair sinon.
 */
export function useAuth() {
  const contexte = useContext(AuthContext);
  if (contexte === null) {
    throw new Error('useAuth doit etre utilise a l\'interieur d\'un AuthProvider');
  }
  return contexte;
}
