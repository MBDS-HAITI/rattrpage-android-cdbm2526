// src/api/utilisateurApi.js
import client from './client';

export async function listerUtilisateurs(page = 0) {
  const reponse = await client.get('/api/utilisateurs', { params: { page, size: 50 } });
  return reponse.data;
}

export async function modifierUtilisateur(id, role, actif) {
  const reponse = await client.put(`/api/utilisateurs/${id}`, { role, actif });
  return reponse.data;
}
