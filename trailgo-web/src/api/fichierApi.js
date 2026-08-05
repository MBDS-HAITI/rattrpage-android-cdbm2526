// src/api/fichierApi.js
import client from './client';

/**
 * Televerse une image et renvoie son URL relative (/uploads/xxx.jpg),
 * a utiliser telle quelle dans imageCouverture ou photo.
 */
export async function televerserImage(fichier) {
  const formData = new FormData();
  formData.append('fichier', fichier);

  const reponse = await client.post('/api/fichiers/images', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return reponse.data;
}
