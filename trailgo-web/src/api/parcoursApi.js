// src/api/parcoursApi.js
//
// Fonctions d'appel a l'API des parcours.
// Chacune correspond a un endpoint du ParcoursController Spring.

import client from './client';

/**
 * Liste paginee avec filtres optionnels.
 * @param {object} parametres { page, taille, theme, difficulte, statut, recherche }
 */
export async function listerParcours(parametres = {}) {
  const reponse = await client.get('/api/parcours', {
    params: {
      page: parametres.page ?? 0,
      size: parametres.taille ?? 20,
      theme: parametres.theme || undefined,
      difficulte: parametres.difficulte || undefined,
      statut: parametres.statut || undefined,
      recherche: parametres.recherche || undefined,
      sort: parametres.tri || 'dateCreation,desc',
    },
  });
  return reponse.data;
}

export async function consulterParcours(id) {
  const reponse = await client.get(`/api/parcours/${id}`);
  return reponse.data;
}

export async function creerParcours(donnees) {
  const reponse = await client.post('/api/parcours', donnees);
  return reponse.data;
}

export async function modifierParcours(id, donnees) {
  const reponse = await client.put(`/api/parcours/${id}`, donnees);
  return reponse.data;
}

export async function supprimerParcours(id) {
  await client.delete(`/api/parcours/${id}`);
}

export async function publierParcours(id) {
  const reponse = await client.post(`/api/parcours/${id}/publication`);
  return reponse.data;
}

export async function depublierParcours(id) {
  const reponse = await client.delete(`/api/parcours/${id}/publication`);
  return reponse.data;
}
