// src/api/client.js
//
// Point d'entree UNIQUE pour tous les appels HTTP vers l'API Spring Boot.
// Equivalent du GestionnaireExceptions cote back : centraliser la
// logique commune (URL de base, jeton, gestion des erreurs) au lieu de
// la repeter dans chaque composant.

import axios from 'axios';

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Intercepteur de requete : ajoute automatiquement le jeton JWT
 * a chaque appel, si l'utilisateur est connecte.
 *
 * Sans cela, il faudrait ajouter l'en-tete Authorization a la main
 * dans chaque fonction d'appel API — source d'oublis garantie.
 */
client.interceptors.request.use((config) => {
  const jeton = localStorage.getItem('trailgo_jeton');
  if (jeton) {
    config.headers.Authorization = `Bearer ${jeton}`;
  }
  return config;
});

/**
 * Intercepteur de reponse : detecte un jeton expire ou invalide (401/403)
 * et deconnecte automatiquement l'utilisateur.
 *
 * Sans cela, un jeton perime provoquerait des erreurs silencieuses sur
 * chaque appel, sans que l'utilisateur comprenne pourquoi rien ne
 * fonctionne plus.
 */
client.interceptors.response.use(
  (reponse) => reponse,
  (erreur) => {
    const statut = erreur.response?.status;
    if (statut === 401 || statut === 403) {
      const cheminActuel = window.location.pathname;
      const estEndpointAuth = erreur.config?.url?.includes('/api/auth/');

      // On ne deconnecte que si l'utilisateur etait cense etre
      // authentifie. Un 403 sur un endpoint public (consultation)
      // ne doit pas provoquer de deconnexion intempestive.
      if (!estEndpointAuth && cheminActuel !== '/connexion') {
        localStorage.removeItem('trailgo_jeton');
        localStorage.removeItem('trailgo_utilisateur');
        window.location.href = '/connexion';
      }
    }
    return Promise.reject(erreur);
  }
);

export default client;