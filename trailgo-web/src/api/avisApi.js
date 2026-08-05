// src/api/avisApi.js
import client from './client';

export async function statistiquesAvis(parcoursId) {
  const reponse = await client.get(`/api/parcours/${parcoursId}/avis/statistiques`);
  return reponse.data;
}

/** Avis signales, pour la moderation. Reserve aux ADMIN. */
export async function listerAvisSignales(page = 0) {
  const reponse = await client.get('/api/avis/signales', { params: { page, size: 20 } });
  return reponse.data;
}

export async function supprimerAvis(avisId) {
  await client.delete(`/api/avis/${avisId}`);
}

export async function leverSignalement(avisId) {
  const reponse = await client.delete(`/api/avis/${avisId}/signalement`);
  return reponse.data;
}
