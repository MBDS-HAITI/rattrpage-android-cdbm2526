// src/pages/PageListeParcours.jsx
import { useState, useEffect } from 'react';
import { listerParcours } from '../api/parcoursApi';
import { useAuth } from '../context/AuthContext';
import CarteParcours from '../components/CarteParcours';
import './PageListeParcours.css';

function PageListeParcours() {
  const [donnees, setDonnees] = useState(null);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState(null);

  const [filtreTheme, setFiltreTheme] = useState('');
  const [filtreDifficulte, setFiltreDifficulte] = useState('');
  const [filtreStatut, setFiltreStatut] = useState('');
  const [recherche, setRecherche] = useState('');
  const [page, setPage] = useState(0);

  const { utilisateur, deconnecter } = useAuth();

  // Se redeclenche a chaque changement de filtre ou de page : React
  // recharge automatiquement la liste sans qu'on ait a le demander
  // explicitement ailleurs dans le code.
  useEffect(() => {
    let annule = false;

    async function charger() {
      setChargement(true);
      setErreur(null);
      try {
        const resultat = await listerParcours({
          page,
          theme: filtreTheme,
          difficulte: filtreDifficulte,
          statut: filtreStatut,
          recherche,
        });
        if (!annule) {
          setDonnees(resultat);
        }
      } catch {
        if (!annule) {
          setErreur('Impossible de charger les parcours. Verifiez que l\'API est demarree.');
        }
      } finally {
        if (!annule) {
          setChargement(false);
        }
      }
    }

    charger();

    // Nettoyage : si le composant est demonte ou si les filtres
    // changent avant la fin de l'appel precedent, on ignore son
    // resultat pour eviter d'afficher une reponse perimee.
    return () => {
      annule = true;
    };
  }, [page, filtreTheme, filtreDifficulte, filtreStatut, recherche]);

  function gererChangementFiltre(setter) {
    return (evenement) => {
      setter(evenement.target.value);
      setPage(0);   // tout changement de filtre revient a la premiere page
    };
  }

  return (
    <div className="page-liste-parcours">
      <header className="entete">
        <h1>Parcours touristiques</h1>
        <div className="info-utilisateur">
          <span>{utilisateur?.nom} ({utilisateur?.role})</span>
          <button onClick={deconnecter}>Deconnexion</button>
        </div>
      </header>

      <div className="barre-filtres">
        <input
          type="search"
          placeholder="Rechercher un parcours..."
          value={recherche}
          onChange={gererChangementFiltre(setRecherche)}
        />

        <select value={filtreTheme} onChange={gererChangementFiltre(setFiltreTheme)}>
          <option value="">Tous les themes</option>
          <option value="CULTUREL">Culturel</option>
          <option value="GASTRONOMIQUE">Gastronomique</option>
          <option value="NATUREL">Naturel</option>
          <option value="HISTORIQUE">Historique</option>
        </select>

        <select value={filtreDifficulte} onChange={gererChangementFiltre(setFiltreDifficulte)}>
          <option value="">Toutes difficultes</option>
          <option value="FACILE">Facile</option>
          <option value="MOYEN">Moyen</option>
          <option value="DIFFICILE">Difficile</option>
        </select>

        <select value={filtreStatut} onChange={gererChangementFiltre(setFiltreStatut)}>
          <option value="">Tous statuts</option>
          <option value="PUBLIE">Publie</option>
          <option value="BROUILLON">Brouillon</option>
        </select>
      </div>

      {chargement && <p className="message-info">Chargement des parcours...</p>}
      {erreur && <p className="message-erreur">{erreur}</p>}

      {!chargement && !erreur && donnees && (
        <>
          {donnees.contenu.length === 0 ? (
            <p className="message-info">Aucun parcours ne correspond a ces criteres.</p>
          ) : (
            <div className="grille-parcours">
              {donnees.contenu.map((parcours) => (
                <CarteParcours key={parcours.id} parcours={parcours} />
              ))}
            </div>
          )}

          {donnees.totalPages > 1 && (
            <div className="pagination">
              <button disabled={donnees.premiere} onClick={() => setPage(page - 1)}>
                Precedent
              </button>
              <span>Page {donnees.page + 1} sur {donnees.totalPages}</span>
              <button disabled={donnees.derniere} onClick={() => setPage(page + 1)}>
                Suivant
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default PageListeParcours;
