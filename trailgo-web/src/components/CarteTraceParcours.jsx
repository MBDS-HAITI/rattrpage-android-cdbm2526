// src/components/CarteTraceParcours.jsx
//
// Affiche le trace et les etapes d'UN parcours sur une carte Leaflet.
// Utilise sur la fiche detail.

import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet';
import { useEffect } from 'react';
import 'leaflet/dist/leaflet.css';
import '../components/config-icones-leaflet';

/**
 * Recentre et zoome automatiquement la carte pour que tout le contenu
 * (trace + marqueurs) soit visible, sans que l'utilisateur ait a le
 * faire manuellement.
 *
 * Doit etre un composant enfant du MapContainer : useMap() ne
 * fonctionne qu'a l'interieur du contexte Leaflet.
 */
function AjusterVue({ positions }) {
  const carte = useMap();

  useEffect(() => {
    if (positions.length === 0) return;

    if (positions.length === 1) {
      carte.setView(positions[0], 15);
    } else {
      carte.fitBounds(positions, { padding: [30, 30] });
    }
  }, [carte, positions]);

  return null;
}

function CarteTraceParcours({ etapes, trace }) {
  // Positions des marqueurs, au format [latitude, longitude] attendu
  // par Leaflet (a NE PAS confondre avec l'ordre GeoJSON [lng, lat]).
  const positionsEtapes = etapes.map((e) => [e.latitude, e.longitude]);

  // Le trace GeoJSON stocke ses coordonnees en [longitude, latitude] ;
  // Leaflet attend l'inverse. Cette conversion est l'endroit exact ou
  // l'inversion X/Y pourrait se glisser si on n'y prete pas attention.
  const positionsTrace = trace?.geometrie?.coordinates?.map(
    ([longitude, latitude]) => [latitude, longitude]
  ) ?? [];

  const toutesLesPositions = positionsTrace.length > 0 ? positionsTrace : positionsEtapes;

  if (toutesLesPositions.length === 0) {
    return (
      <div className="carte-vide">
        Aucune donnee geographique a afficher pour ce parcours.
      </div>
    );
  }

  return (
    <MapContainer
      center={toutesLesPositions[0]}
      zoom={14}
      scrollWheelZoom={false}
      className="conteneur-carte"
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {positionsTrace.length > 1 && (
        <Polyline positions={positionsTrace} color="#2563eb" weight={4} />
      )}

      {etapes.map((etape) => (
        <Marker key={etape.id} position={[etape.latitude, etape.longitude]}>
          <Popup>
            <strong>{etape.ordre}. {etape.nom}</strong>
            {etape.description && <p style={{ margin: '0.3rem 0 0' }}>{etape.description}</p>}
          </Popup>
        </Marker>
      ))}

      <AjusterVue positions={toutesLesPositions} />
    </MapContainer>
  );
}

export default CarteTraceParcours;
