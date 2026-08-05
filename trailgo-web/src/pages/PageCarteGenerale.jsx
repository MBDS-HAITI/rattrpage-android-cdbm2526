// src/pages/PageCarteGenerale.jsx
//
// Vue d'ensemble : toutes les etapes de tous les parcours publies,
// regroupees par proximite (clustering) pour rester lisible meme avec
// beaucoup de marqueurs proches les uns des autres.

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import MarkerClusterGroup from 'react-leaflet-cluster';
import 'leaflet/dist/leaflet.css';
import 'leaflet.markercluster/dist/MarkerCluster.css';
import 'leaflet.markercluster/dist/MarkerCluster.Default.css';
import '../components/config-icones-leaflet';
import { listerParcours, consulterParcours } from '../api/parcoursApi';
import './PageCarteGenerale.css';

// Centre approximatif de Port-au-Prince, utilise tant qu'aucune
// donnee n'est encore chargee.
const CENTRE_PAR_DEFAUT = [18.5392, -72.3364];

function PageCarteGenerale() {
  const [marqueurs, setMarqueurs] = useState([]);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState(null);

  useEffect(() => {
    chargerToutesLesEtapes();
  }, []);

  async function chargerToutesLesEtapes() {
    setChargement(true);
    setErreur(null);
    try {
      // Etape 1 : la liste resumee des parcours publies.
      const liste = await listerParcours({ statut: 'PUBLIE', taille: 100 });

      // Etape 2 : le detail de chacun, en parallele, pour recuperer
      // les coordonnees de leurs etapes (absentes du resume).
      const details = await Promise.all(
        liste.contenu.map((p) => consulterParcours(p.id))
      );

      const tousLesMarqueurs = details.flatMap((parcours) =>
        parcours.etapes.map((etape) => ({
          id: `${parcours.id}-${etape.id}`,
          parcoursId: parcours.id,
          parcoursTitre: parcours.titre,
          nom: etape.nom,
          latitude: etape.latitude,
          longitude: etape.longitude,
        }))
      );

      setMarqueurs(tousLesMarqueurs);
    } catch {
      setErreur('Impossible de charger les parcours publies.');
    } finally {
      setChargement(false);
    }
  }

  const centre = marqueurs.length > 0
    ? [marqueurs[0].latitude, marqueurs[0].longitude]
    : CENTRE_PAR_DEFAUT;

  return (
    <div className="page-carte-generale">
      <header className="entete-carte-generale">
        <div>
          <h1>Carte des parcours publies</h1>
          <p className="sous-titre">
            {chargement ? 'Chargement...' : `${marqueurs.length} etape(s) sur la carte`}
          </p>
        </div>
        <Link to="/parcours" className="bouton-secondaire">← Retour a la liste</Link>
      </header>

      {erreur && <p className="message-erreur">{erreur}</p>}

      {!chargement && !erreur && (
        <MapContainer
          center={centre}
          zoom={11}
          className="conteneur-carte-generale"
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          {/* MarkerClusterGroup regroupe automatiquement les marqueurs
              proches en un seul cercle numerote, qui se separe au zoom.
              Sans cela, une carte avec beaucoup d'etapes proches devient
              vite illisible. */}
          <MarkerClusterGroup chunkedLoading>
            {marqueurs.map((marqueur) => (
              <Marker key={marqueur.id} position={[marqueur.latitude, marqueur.longitude]}>
                <Popup>
                  <strong>{marqueur.nom}</strong>
                  <p style={{ margin: '0.3rem 0' }}>{marqueur.parcoursTitre}</p>
                  <Link to={`/parcours/${marqueur.parcoursId}`}>Voir le parcours</Link>
                </Popup>
              </Marker>
            ))}
          </MarkerClusterGroup>
        </MapContainer>
      )}
    </div>
  );
}

export default PageCarteGenerale;
