// src/pages/PageModerationAvis.jsx
//
// Ecran de moderation : les avis signales par les utilisateurs
// attendent ici une decision de l'administrateur.

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { listerAvisSignales, supprimerAvis, leverSignalement } from '../api/avisApi';
import './PageModerationAvis.css';

function PageModerationAvis() {
  const [donnees, setDonnees] = useState(null);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState(null);
  const [actionEnCoursId, setActionEnCoursId] = useState(null);

  useEffect(() => {
    charger();
  }, []);

  async function charger() {
    setChargement(true);
    setErreur(null);
    try {
      const resultat = await listerAvisSignales();
      setDonnees(resultat);
    } catch {
      setErreur('Impossible de charger les avis signales.');
    } finally {
      setChargement(false);
    }
  }

  async function gererSuppression(avis) {
    const confirmation = window.confirm(
      `Supprimer definitivement l'avis de ${avis.auteurNom} sur "${avis.parcoursTitre}" ?`
    );
    if (!confirmation) return;

    setActionEnCoursId(avis.id);
    try {
      await supprimerAvis(avis.id);
      // Retire l'avis de la liste locale plutot que de recharger
      // tout depuis le serveur : l'interface reste reactive.
      setDonnees((precedent) => ({
        ...precedent,
        contenu: precedent.contenu.filter((a) => a.id !== avis.id),
        totalElements: precedent.totalElements - 1,
      }));
    } catch {
      alert('Impossible de supprimer cet avis.');
    } finally {
      setActionEnCoursId(null);
    }
  }

  async function gererLeveeSignalement(avis) {
    setActionEnCoursId(avis.id);
    try {
      await leverSignalement(avis.id);
      // L'avis n'est plus signale : il sort de la file de moderation.
      setDonnees((precedent) => ({
        ...precedent,
        contenu: precedent.contenu.filter((a) => a.id !== avis.id),
        totalElements: precedent.totalElements - 1,
      }));
    } catch {
      alert('Impossible de lever le signalement.');
    } finally {
      setActionEnCoursId(null);
    }
  }

  if (chargement) {
    return <p className="message-info">Chargement...</p>;
  }

  if (erreur) {
    return <p className="message-erreur">{erreur}</p>;
  }

  return (
    <div className="page-moderation">
      <header className="entete-moderation">
        <div>
          <h1>Moderation des avis</h1>
          <p className="sous-titre">
            {donnees.totalElements} avis signale{donnees.totalElements > 1 ? 's' : ''} en attente
          </p>
        </div>
        <Link to="/parcours" className="bouton-secondaire">← Retour a la liste</Link>
      </header>

      {donnees.contenu.length === 0 ? (
        <p className="message-info">Aucun avis signale pour le moment. Tout est en ordre.</p>
      ) : (
        <div className="liste-avis-signales">
          {donnees.contenu.map((avis) => (
            <div key={avis.id} className="carte-avis-signale">
              <div className="entete-avis">
                <div>
                  <Link to={`/parcours/${avis.parcoursId}`} className="lien-parcours-avis">
                    {avis.parcoursTitre}
                  </Link>
                  <span className="meta-avis">
                    par {avis.auteurNom} · {new Date(avis.dateCreation).toLocaleDateString('fr-FR')}
                  </span>
                </div>
                <span className="note-avis">{'★'.repeat(avis.note)}{'☆'.repeat(5 - avis.note)}</span>
              </div>

              {avis.commentaire && <p className="commentaire-avis">{avis.commentaire}</p>}

              <div className="actions-avis">
                <button
                  onClick={() => gererLeveeSignalement(avis)}
                  disabled={actionEnCoursId === avis.id}
                  className="bouton-secondaire"
                >
                  Approuver (lever le signalement)
                </button>
                <button
                  onClick={() => gererSuppression(avis)}
                  disabled={actionEnCoursId === avis.id}
                  className="bouton-danger"
                >
                  Supprimer l'avis
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default PageModerationAvis;
