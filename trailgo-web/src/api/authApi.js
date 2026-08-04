// src/api/authApi.js
//
// Fonctions d'appel a l'API d'authentification.
// Chaque fonction correspond a un endpoint du AuthController Spring.

import client from './client';

export async function connecter(email, motDePasse) {
  const reponse = await client.post('/api/auth/connexion', { email, motDePasse });
  return reponse.data;
}

export async function inscrire(email, motDePasse, nom) {
  const reponse = await client.post('/api/auth/inscription', { email, motDePasse, nom });
  return reponse.data;
}

export async function recupererProfil() {
  const reponse = await client.get('/api/auth/moi');
  return reponse.data;
}
