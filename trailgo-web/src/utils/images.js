// src/utils/images.js
//
// L'API renvoie des chemins RELATIFS pour les images ("/uploads/xxx.jpg"),
// valides uniquement sur le serveur qui les sert (port 8080). Le
// navigateur resout un chemin relatif par rapport a la page courante
// (port 5173, celui de Vite) : sans ce prefixe, l'image pointe vers
// une adresse qui n'existe pas et ne s'affiche jamais.

export function urlAbsolueImage(cheminRelatif) {
  if (!cheminRelatif) return null;
  if (cheminRelatif.startsWith('http')) return cheminRelatif;   // deja absolue
  return `${import.meta.env.VITE_API_URL}${cheminRelatif}`;
}
