// src/components/config-icones-leaflet.js
//
// PIEGE CLASSIQUE : avec un outil de build moderne (Vite, Webpack),
// les icones de marqueur par defaut de Leaflet ne s'affichent pas.
// Leaflet cherche ses images via des chemins relatifs qui ne
// correspondent plus une fois le code regroupe ("bundle").
//
// Solution : reconstruire l'icone par defaut en pointant explicitement
// vers les fichiers images, importes comme des modules pour que Vite
// leur attribue une URL correcte.
//
// A importer UNE SEULE FOIS, au demarrage de l'application (main.jsx).

import L from 'leaflet';
import iconeUrl from 'leaflet/dist/images/marker-icon.png';
import iconeRetineUrl from 'leaflet/dist/images/marker-icon-2x.png';
import ombreUrl from 'leaflet/dist/images/marker-shadow.png';

delete L.Icon.Default.prototype._getIconUrl;

L.Icon.Default.mergeOptions({
  iconUrl: iconeUrl,
  iconRetinaUrl: iconeRetineUrl,
  shadowUrl: ombreUrl,
});
