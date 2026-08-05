// src/pages/PageFormulaireParcours.jsx
//
// Formulaire de creation ET de modification d'un parcours.
// Les deux cas partagent le meme composant : seul le mode change selon
// qu'un id est present dans l'URL. C'est une pratique courante en React
// plutot que de dupliquer deux formulaires presque identiques.

import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  consulterParcours,
  creerParcours,
  modifierParcours,
} from '../api/parcoursApi';
import { televerserImage } from '../api/fichierApi';
import './PageFormulaireParcours.css';

const THEMES = ['CULTUREL', 'GASTRONOMIQUE', 'NATUREL', 'HISTORIQUE'];
const DIFFICULTES = ['FACILE', 'MOYEN', 'DIFFICILE'];

/** Genere un identifiant local temporaire pour le drag & drop,
    avant que l'etape n'existe en base (donc sans id serveur). */
let compteurTemporaire = 0;
function idTemporaire() {
  compteurTemporaire += 1;
  return `temp-${compteurTemporaire}`;
}

function etapeVide() {
  return {
    cleLocale: idTemporaire(),
    nom: '',
    description: '',
    latitude: '',
    longitude: '',
    dureeVisiteMin: '',
  };
}

function PageFormulaireParcours() {
  const { id } = useParams();
  const navigate = useNavigate();
  const modeEdition = Boolean(id);

  const [titre, setTitre] = useState('');
  const [description, setDescription] = useState('');
  const [theme, setTheme] = useState('CULTUREL');
  const [difficulte, setDifficulte] = useState('FACILE');
  const [dureeEstimeeMin, setDureeEstimeeMin] = useState('');
  const [imageCouverture, setImageCouverture] = useState('');
  const [etapes, setEtapes] = useState([etapeVide()]);

  const [chargement, setChargement] = useState(modeEdition);
  const [enregistrement, setEnregistrement] = useState(false);
  const [televersementImage, setTeleversementImage] = useState(false);
  const [erreurs, setErreurs] = useState({});
  const [erreurGenerale, setErreurGenerale] = useState(null);

  const indexGlisse = useRef(null);

  // En mode edition, on precharge le parcours existant.
  useEffect(() => {
    if (!modeEdition) return;

    async function charger() {
      try {
        const parcours = await consulterParcours(id);
        setTitre(parcours.titre);
        setDescription(parcours.description ?? '');
        setTheme(parcours.theme);
        setDifficulte(parcours.difficulte);
        setDureeEstimeeMin(parcours.dureeEstimeeMin ?? '');
        setImageCouverture(parcours.imageCouverture ?? '');
        setEtapes(
          parcours.etapes.length > 0
            ? parcours.etapes.map((e) => ({
                cleLocale: idTemporaire(),
                nom: e.nom,
                description: e.description ?? '',
                latitude: String(e.latitude),
                longitude: String(e.longitude),
                dureeVisiteMin: e.dureeVisiteMin ?? '',
              }))
            : [etapeVide()]
        );
      } catch {
        setErreurGenerale('Impossible de charger ce parcours.');
      } finally {
        setChargement(false);
      }
    }
    charger();
  }, [id, modeEdition]);

  // ============ Gestion des etapes ============

  function ajouterEtape() {
    setEtapes([...etapes, etapeVide()]);
  }

  function retirerEtape(cleLocale) {
    if (etapes.length === 1) return;   // au moins une etape doit rester
    setEtapes(etapes.filter((e) => e.cleLocale !== cleLocale));
  }

  function modifierEtape(cleLocale, champ, valeur) {
    setEtapes(etapes.map((e) =>
      e.cleLocale === cleLocale ? { ...e, [champ]: valeur } : e
    ));
  }

  // ---- Drag & drop natif (API HTML5, aucune bibliotheque externe) ----

  function gererDebutGlisse(index) {
    indexGlisse.current = index;
  }

  function gererSurvol(evenement, indexCible) {
    evenement.preventDefault();
    const indexSource = indexGlisse.current;
    if (indexSource === null || indexSource === indexCible) return;

    const copie = [...etapes];
    const [deplacee] = copie.splice(indexSource, 1);
    copie.splice(indexCible, 0, deplacee);

    indexGlisse.current = indexCible;
    setEtapes(copie);
  }

  function gererFinGlisse() {
    indexGlisse.current = null;
  }

  // ============ Image ============

  async function gererChoixImage(evenement) {
    const fichier = evenement.target.files[0];
    if (!fichier) return;

    setTeleversementImage(true);
    try {
      const resultat = await televerserImage(fichier);
      setImageCouverture(resultat.url);
    } catch {
      alert('Impossible de televerser cette image (format ou taille invalide).');
    } finally {
      setTeleversementImage(false);
    }
  }

  // ============ Validation et soumission ============

  function valider() {
    const nouvellesErreurs = {};

    if (!titre.trim()) {
      nouvellesErreurs.titre = 'Le titre est obligatoire';
    }

    etapes.forEach((etape, index) => {
      if (!etape.nom.trim()) {
        nouvellesErreurs[`etape-${index}-nom`] = 'Le nom est obligatoire';
      }
      const lat = Number(etape.latitude);
      const lng = Number(etape.longitude);
      if (etape.latitude === '' || Number.isNaN(lat) || lat < -90 || lat > 90) {
        nouvellesErreurs[`etape-${index}-latitude`] = 'Latitude invalide (-90 a 90)';
      }
      if (etape.longitude === '' || Number.isNaN(lng) || lng < -180 || lng > 180) {
        nouvellesErreurs[`etape-${index}-longitude`] = 'Longitude invalide (-180 a 180)';
      }
    });

    setErreurs(nouvellesErreurs);
    return Object.keys(nouvellesErreurs).length === 0;
  }

  async function gererSoumission(evenement) {
    evenement.preventDefault();
    setErreurGenerale(null);

    if (!valider()) {
      return;
    }

    const donnees = {
      titre: titre.trim(),
      description: description.trim() || null,
      theme,
      difficulte,
      dureeEstimeeMin: dureeEstimeeMin === '' ? null : Number(dureeEstimeeMin),
      imageCouverture: imageCouverture || null,
      etapes: etapes.map((e) => ({
        nom: e.nom.trim(),
        description: e.description.trim() || null,
        latitude: Number(e.latitude),
        longitude: Number(e.longitude),
        dureeVisiteMin: e.dureeVisiteMin === '' ? null : Number(e.dureeVisiteMin),
      })),
    };

    setEnregistrement(true);
    try {
      const resultat = modeEdition
        ? await modifierParcours(id, donnees)
        : await creerParcours(donnees);
      navigate(`/parcours/${resultat.id}`);
    } catch (erreurAppel) {
      const detail = erreurAppel.response?.data;
      if (detail?.erreurs) {
        // Erreurs de validation Bean cote serveur : on les fusionne
        // avec celles deja affichees, au cas ou le client aurait
        // laisse passer un cas que le serveur rejette quand meme.
        setErreurs((precedent) => ({ ...precedent, ...detail.erreurs }));
      }
      setErreurGenerale(detail?.detail ?? 'Impossible d\'enregistrer ce parcours.');
    } finally {
      setEnregistrement(false);
    }
  }

  if (chargement) {
    return <p className="message-info">Chargement...</p>;
  }

  return (
    <div className="page-formulaire-parcours">
      <Link to="/parcours" className="lien-retour">← Retour a la liste</Link>

      <h1>{modeEdition ? 'Modifier le parcours' : 'Nouveau parcours'}</h1>

      {erreurGenerale && <p className="message-erreur">{erreurGenerale}</p>}

      <form onSubmit={gererSoumission}>
        <section className="section-formulaire">
          <h2>Informations generales</h2>

          <div className="champ">
            <label htmlFor="titre">Titre *</label>
            <input
              id="titre"
              value={titre}
              onChange={(e) => setTitre(e.target.value)}
            />
            {erreurs.titre && <span className="erreur-champ">{erreurs.titre}</span>}
          </div>

          <div className="champ">
            <label htmlFor="description">Description</label>
            <textarea
              id="description"
              rows={4}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div className="grille-champs">
            <div className="champ">
              <label htmlFor="theme">Theme *</label>
              <select id="theme" value={theme} onChange={(e) => setTheme(e.target.value)}>
                {THEMES.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>

            <div className="champ">
              <label htmlFor="difficulte">Difficulte *</label>
              <select id="difficulte" value={difficulte} onChange={(e) => setDifficulte(e.target.value)}>
                {DIFFICULTES.map((d) => <option key={d} value={d}>{d}</option>)}
              </select>
            </div>

            <div className="champ">
              <label htmlFor="duree">Duree estimee (min)</label>
              <input
                id="duree"
                type="number"
                min="1"
                value={dureeEstimeeMin}
                onChange={(e) => setDureeEstimeeMin(e.target.value)}
              />
            </div>
          </div>

          <div className="champ">
            <label htmlFor="image">Image de couverture</label>
            <input id="image" type="file" accept="image/*" onChange={gererChoixImage} />
            {televersementImage && <span className="info-televersement">Televersement...</span>}
            {imageCouverture && (
              <img src={imageCouverture} alt="Apercu" className="apercu-image" />
            )}
          </div>
        </section>

        <section className="section-formulaire">
          <div className="entete-section">
            <h2>Etapes</h2>
            <button type="button" onClick={ajouterEtape} className="bouton-secondaire">
              + Ajouter une etape
            </button>
          </div>

          <p className="aide-drag">Glissez une etape par sa poignee (⠿) pour la reordonner.</p>

          <div className="liste-etapes-formulaire">
            {etapes.map((etape, index) => (
              <div
                key={etape.cleLocale}
                className="carte-etape-formulaire"
                draggable
                onDragStart={() => gererDebutGlisse(index)}
                onDragOver={(e) => gererSurvol(e, index)}
                onDragEnd={gererFinGlisse}
              >
                <span className="poignee-glisse" title="Glisser pour reordonner">⠿</span>
                <span className="numero-etape-form">{index + 1}</span>

                <div className="champs-etape">
                  <div className="champ">
                    <input
                      placeholder="Nom de l'etape *"
                      value={etape.nom}
                      onChange={(e) => modifierEtape(etape.cleLocale, 'nom', e.target.value)}
                    />
                    {erreurs[`etape-${index}-nom`] && (
                      <span className="erreur-champ">{erreurs[`etape-${index}-nom`]}</span>
                    )}
                  </div>

                  <textarea
                    placeholder="Description"
                    rows={2}
                    value={etape.description}
                    onChange={(e) => modifierEtape(etape.cleLocale, 'description', e.target.value)}
                  />

                  <div className="grille-champs-etape">
                    <div className="champ">
                      <input
                        placeholder="Latitude *"
                        value={etape.latitude}
                        onChange={(e) => modifierEtape(etape.cleLocale, 'latitude', e.target.value)}
                      />
                      {erreurs[`etape-${index}-latitude`] && (
                        <span className="erreur-champ">{erreurs[`etape-${index}-latitude`]}</span>
                      )}
                    </div>
                    <div className="champ">
                      <input
                        placeholder="Longitude *"
                        value={etape.longitude}
                        onChange={(e) => modifierEtape(etape.cleLocale, 'longitude', e.target.value)}
                      />
                      {erreurs[`etape-${index}-longitude`] && (
                        <span className="erreur-champ">{erreurs[`etape-${index}-longitude`]}</span>
                      )}
                    </div>
                    <input
                      placeholder="Duree (min)"
                      type="number"
                      min="1"
                      value={etape.dureeVisiteMin}
                      onChange={(e) => modifierEtape(etape.cleLocale, 'dureeVisiteMin', e.target.value)}
                    />
                  </div>
                </div>

                <button
                  type="button"
                  className="bouton-retirer-etape"
                  onClick={() => retirerEtape(etape.cleLocale)}
                  disabled={etapes.length === 1}
                  title="Retirer cette etape"
                >
                  ✕
                </button>
              </div>
            ))}
          </div>
        </section>

        <div className="actions-formulaire">
          <Link to="/parcours" className="bouton-secondaire">Annuler</Link>
          <button type="submit" disabled={enregistrement} className="bouton-primaire">
            {enregistrement ? 'Enregistrement...' : (modeEdition ? 'Enregistrer' : 'Creer le parcours')}
          </button>
        </div>
      </form>
    </div>
  );
}

export default PageFormulaireParcours;
