// src/pages/PageDashboard.jsx
//
// Tableau de bord : statistiques globales calculees a partir des
// endpoints existants, sans qu'aucun endpoint dedie n'ait ete
// necessaire cote API.

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { listerParcours, consulterParcours } from '../api/parcoursApi';
import { statistiquesAvis } from '../api/avisApi';
import './PageDashboard.css';

const LIBELLES_THEME = {
  CULTUREL: 'Culturel',
  GASTRONOMIQUE: 'Gastronomique',
  NATUREL: 'Naturel',
  HISTORIQUE: 'Historique',
};

function PageDashboard() {
  const [nbPublies, setNbPublies] = useState(null);
  const [nbBrouillons, setNbBrouillons] = useState(null);
  const [topParcours, setTopParcours] = useState([]);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState(null);

  useEffect(() => {
    chargerStatistiques();
  }, []);

  async function chargerStatistiques() {
    setChargement(true);
    setErreur(null);
    try {
      // Trois appels independants : on ne demande qu'un seul resultat
      // (size: 1) pour les comptages, le total figure dans la
      // pagination sans avoir besoin de charger les lignes elles-memes.
      const [reponsePublies, reponseBrouillons, tousLesParcours] = await Promise.all([
        listerParcours({ statut: 'PUBLIE', taille: 1 }),
        listerParcours({ statut: 'BROUILLON', taille: 1 }),
        listerParcours({ taille: 100 }),
      ]);

      setNbPublies(reponsePublies.totalElements);
      setNbBrouillons(reponseBrouillons.totalElements);

      // ParcoursSummaryResponse (utilise par la liste paginee) ne
      // contient PAS le champ nbConsultations : seul le detail complet
      // l'expose. On charge donc chaque parcours individuellement
      // avant de pouvoir trier par nombre de consultations.
      // Avec un nombre de parcours modeste, le cout reste negligeable ;
      // sur un catalogue de plusieurs milliers de parcours, ce calcul
      // devrait etre deplace cote serveur (endpoint dedie ou champ
      // ajoute au resume).
      const details = await Promise.all(
        tousLesParcours.contenu.map((p) => consulterParcours(p.id))
      );

      const cinqPremiers = [...details]
        .sort((a, b) => b.nbConsultations - a.nbConsultations)
        .slice(0, 5);

      // Note moyenne, chargee seulement pour ces 5 parcours : inutile
      // d'interroger les statistiques d'avis de tous les parcours.
      const avecNotes = await Promise.all(
        cinqPremiers.map(async (parcours) => {
          try {
            const stats = await statistiquesAvis(parcours.id);
            return { ...parcours, noteMoyenne: stats.noteMoyenne, nbAvis: stats.nbAvis };
          } catch {
            return { ...parcours, noteMoyenne: null, nbAvis: 0 };
          }
        })
      );

      setTopParcours(avecNotes);
    } catch {
      setErreur('Impossible de charger les statistiques.');
    } finally {
      setChargement(false);
    }
  }

  if (chargement) {
    return <p className="message-info">Chargement du tableau de bord...</p>;
  }

  if (erreur) {
    return <p className="message-erreur">{erreur}</p>;
  }

  const total = nbPublies + nbBrouillons;

  return (
    <div className="page-dashboard">
      <header className="entete-dashboard">
        <h1>Tableau de bord</h1>
        <Link to="/parcours" className="bouton-secondaire">← Retour a la liste</Link>
      </header>

      <div className="grille-statistiques">
        <div className="carte-statistique">
          <span className="valeur-statistique">{total}</span>
          <span className="label-statistique">Parcours au total</span>
        </div>

        <div className="carte-statistique carte-statistique-publie">
          <span className="valeur-statistique">{nbPublies}</span>
          <span className="label-statistique">Publies</span>
        </div>

        <div className="carte-statistique carte-statistique-brouillon">
          <span className="valeur-statistique">{nbBrouillons}</span>
          <span className="label-statistique">Brouillons</span>
        </div>
      </div>

      <section className="section-top">
        <h2>Top 5 des parcours les plus consultes</h2>

        {topParcours.length === 0 ? (
          <p className="message-info">Aucun parcours pour le moment.</p>
        ) : (
          <table className="tableau-top">
            <thead>
              <tr>
                <th>Rang</th>
                <th>Parcours</th>
                <th>Theme</th>
                <th>Consultations</th>
                <th>Note moyenne</th>
              </tr>
            </thead>
            <tbody>
              {topParcours.map((parcours, index) => (
                <tr key={parcours.id}>
                  <td className="cellule-rang">{index + 1}</td>
                  <td>
                    <Link to={`/parcours/${parcours.id}`}>{parcours.titre}</Link>
                  </td>
                  <td>{LIBELLES_THEME[parcours.theme] ?? parcours.theme}</td>
                  <td>{parcours.nbConsultations}</td>
                  <td>
                    {parcours.noteMoyenne != null
                      ? `${parcours.noteMoyenne} / 5 (${parcours.nbAvis} avis)`
                      : 'Aucun avis'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}

export default PageDashboard;
