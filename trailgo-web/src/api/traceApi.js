// src/api/traceApi.js
import client from './client';

/**
 * Recupere le trace d'un parcours. Renvoie null si aucun trace n'a
 * ete importe (l'API repond 404 dans ce cas, ce qui n'est pas une
 * vraie erreur pour l'affichage : on montre juste les marqueurs seuls).
 */
export async function consulterTrace(parcoursId) {
  try {
    const reponse = await client.get(`/api/parcours/${parcoursId}/trace`);
    return reponse.data;
  } catch (erreur) {
    if (erreur.response?.status === 404) {
      return null;
    }
    throw erreur;
  }
}
