// src/components/CarteParcours.jsx
//
// Une "carte" visuelle representant un parcours dans la liste.
// Composant purement presentationnel : il ne fait aucun appel API,
// il recoit ses donnees par props et affiche.

import { Link } from 'react-router-dom';
import './CarteParcours.css';

const LIBELLES_THEME = {
  CULTUREL: 'Culturel',
  GASTRONOMIQUE: 'Gastronomique',
  NATUREL: 'Naturel',
  HISTORIQUE: 'Historique',
};

const LIBELLES_DIFFICULTE = {
  FACILE: 'Facile',
  MOYEN: 'Moyen',
  DIFFICILE: 'Difficile',
};

function CarteParcours({ parcours }) {
  const estPublie = parcours.statut === 'PUBLIE';

  return (
    <Link to={`/parcours/${parcours.id}`} className="carte-parcours">
      <div className="carte-parcours-image">
        {parcours.imageCouverture ? (
          <img src={parcours.imageCouverture} alt={parcours.titre} />
        ) : (
          <div className="carte-parcours-image-vide">Aucune image</div>
        )}
        <span className={`badge-statut ${estPublie ? 'badge-publie' : 'badge-brouillon'}`}>
          {estPublie ? 'Publie' : 'Brouillon'}
        </span>
      </div>

      <div className="carte-parcours-contenu">
        <h3>{parcours.titre}</h3>

        <div className="carte-parcours-tags">
          <span className="tag">{LIBELLES_THEME[parcours.theme] ?? parcours.theme}</span>
          <span className="tag">{LIBELLES_DIFFICULTE[parcours.difficulte] ?? parcours.difficulte}</span>
        </div>

        <div className="carte-parcours-infos">
          {parcours.dureeEstimeeMin != null && (
            <span>{Math.round(parcours.dureeEstimeeMin / 60 * 10) / 10} h</span>
          )}
          {parcours.distanceTotaleKm != null && (
            <span>{parcours.distanceTotaleKm} km</span>
          )}
          <span>{parcours.nbEtapes} etape{parcours.nbEtapes > 1 ? 's' : ''}</span>
        </div>
      </div>
    </Link>
  );
}

export default CarteParcours;
