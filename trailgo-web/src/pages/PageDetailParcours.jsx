// src/pages/PageDetailParcours.jsx
import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  consulterParcours,
  publierParcours,
  depublierParcours,
  supprimerParcours,
} from '../api/parcoursApi';
import { useAuth } from '../context/AuthContext';
import './PageDetailParcours.css';

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

function PageDetailParcours() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { estAdmin } = useAuth();

  const [parcours, setParcours] = useState(null);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState(null);
  const [actionEnCours, setActionEnCours] = useState(false);

  useEffect(() => {
    chargerParcours();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function chargerParcours() {
    setChargement(true);
    setErreur(null);
    try {
      const resultat = await consulterParcours(id);
      setParcours(resultat);
    } catch (erreurAppel) {
      if (erreurAppel.response?.status === 404) {
        setErreur('Ce parcours n\'existe pas ou a ete supprime.');
      } else {
        setErreur('Impossible de charger ce parcours.');
      }
    } finally {
      setChargement(false);
    }
  }

  async function gererPublication() {
    setActionEnCours(true);
    try {
      const misAJour = parcours.statut === 'PUBLIE'
        ? await depublierParcours(id)
        : await publierParcours(id);
      setParcours(misAJour);
    } catch (erreurAppel) {
      // Le service refuse de publier un parcours sans etape (409) :
      // on affiche ce message precis plutot qu'une erreur generique.
      const message = erreurAppel.response?.data?.detail
        ?? 'Impossible de changer le statut de ce parcours.';
      alert(message);
    } finally {
      setActionEnCours(false);
    }
  }

  async function gererSuppression() {
    const confirmation = window.confirm(
      `Supprimer definitivement "${parcours.titre}" ? Cette action est irreversible.`
    );
    if (!confirmation) return;

    setActionEnCours(true);
    try {
      await supprimerParcours(id);
      navigate('/parcours');
    } catch {
      alert('Impossible de supprimer ce parcours.');
      setActionEnCours(false);
    }
  }

  if (chargement) {
    return <p className="message-info">Chargement...</p>;
  }

  if (erreur) {
    return (
      <div className="page-detail-parcours">
        <p className="message-erreur">{erreur}</p>
        <Link to="/parcours">Retour a la liste</Link>
      </div>
    );
  }

  const estPublie = parcours.statut === 'PUBLIE';

  return (
    <div className="page-detail-parcours">
      <Link to="/parcours" className="lien-retour">← Retour a la liste</Link>

      <header className="entete-detail">
        <div>
          <h1>{parcours.titre}</h1>
          <div className="tags-detail">
            <span className="tag">{LIBELLES_THEME[parcours.theme] ?? parcours.theme}</span>
            <span className="tag">{LIBELLES_DIFFICULTE[parcours.difficulte] ?? parcours.difficulte}</span>
            <span className={`badge-statut ${estPublie ? 'badge-publie' : 'badge-brouillon'}`}>
              {estPublie ? 'Publie' : 'Brouillon'}
            </span>
          </div>
        </div>

        {estAdmin && (
          <div className="actions-admin">
            <Link to={`/parcours/${id}/modifier`} className="bouton-secondaire">
              Modifier
            </Link>
            <button
              onClick={gererPublication}
              disabled={actionEnCours}
              className={estPublie ? 'bouton-secondaire' : 'bouton-primaire'}
            >
              {estPublie ? 'Depublier' : 'Publier'}
            </button>
            <button
              onClick={gererSuppression}
              disabled={actionEnCours}
              className="bouton-danger"
            >
              Supprimer
            </button>
          </div>
        )}
      </header>

      {parcours.description && (
        <p className="description-parcours">{parcours.description}</p>
      )}

      <div className="stats-parcours">
        {parcours.dureeEstimeeMin != null && (
          <div className="stat">
            <span className="stat-valeur">{Math.round(parcours.dureeEstimeeMin / 60 * 10) / 10} h</span>
            <span className="stat-label">Duree estimee</span>
          </div>
        )}
        {parcours.distanceTotaleKm != null && (
          <div className="stat">
            <span className="stat-valeur">{parcours.distanceTotaleKm} km</span>
            <span className="stat-label">Distance</span>
          </div>
        )}
        <div className="stat">
          <span className="stat-valeur">{parcours.etapes.length}</span>
          <span className="stat-label">Etapes</span>
        </div>
        <div className="stat">
          <span className="stat-valeur">{parcours.nbConsultations}</span>
          <span className="stat-label">Consultations</span>
        </div>
        {parcours.zoneNom && (
          <div className="stat">
            <span className="stat-valeur">{parcours.zoneNom}</span>
            <span className="stat-label">Zone</span>
          </div>
        )}
      </div>

      <section className="section-etapes">
        <h2>Etapes du parcours</h2>

        {parcours.etapes.length === 0 ? (
          <p className="message-info">
            Ce parcours n'a pas encore d'etape.
            {estAdmin && ' C\'est pourquoi il ne peut pas etre publie.'}
          </p>
        ) : (
          <ol className="liste-etapes">
            {parcours.etapes.map((etape) => (
              <li key={etape.id} className="etape">
                <span className="numero-etape">{etape.ordre}</span>
                <div className="contenu-etape">
                  <h3>{etape.nom}</h3>
                  {etape.description && <p>{etape.description}</p>}
                  <div className="meta-etape">
                    <span>{etape.latitude.toFixed(4)}, {etape.longitude.toFixed(4)}</span>
                    {etape.dureeVisiteMin != null && (
                      <span>{etape.dureeVisiteMin} min de visite</span>
                    )}
                  </div>
                </div>
              </li>
            ))}
          </ol>
        )}
      </section>
    </div>
  );
}

export default PageDetailParcours;
