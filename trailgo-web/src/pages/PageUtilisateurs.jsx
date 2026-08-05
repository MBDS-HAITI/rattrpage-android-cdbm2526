// src/pages/PageUtilisateurs.jsx
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { listerUtilisateurs, modifierUtilisateur } from '../api/utilisateurApi';
import { useAuth } from '../context/AuthContext';
import './PageUtilisateurs.css';

function PageUtilisateurs() {
  const [donnees, setDonnees] = useState(null);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState(null);
  const [actionEnCoursId, setActionEnCoursId] = useState(null);

  const { utilisateur: utilisateurConnecte } = useAuth();

  useEffect(() => {
    charger();
  }, []);

  async function charger() {
    setChargement(true);
    setErreur(null);
    try {
      const resultat = await listerUtilisateurs();
      setDonnees(resultat);
    } catch {
      setErreur('Impossible de charger la liste des utilisateurs.');
    } finally {
      setChargement(false);
    }
  }

  async function gererChangementRole(compte, nouveauRole) {
    await appliquerModification(compte, nouveauRole, compte.actif);
  }

  async function gererBasculeActif(compte) {
    await appliquerModification(compte, compte.role, !compte.actif);
  }

  async function appliquerModification(compte, role, actif) {
    setActionEnCoursId(compte.id);
    try {
      const misAJour = await modifierUtilisateur(compte.id, role, actif);
      setDonnees((precedent) => ({
        ...precedent,
        contenu: precedent.contenu.map((c) => (c.id === compte.id ? misAJour : c)),
      }));
    } catch (erreurAppel) {
      // Le service refuse l'auto-modification et la suppression du
      // dernier administrateur actif (409) : on affiche ce message
      // precis plutot qu'une erreur generique.
      const message = erreurAppel.response?.data?.detail
        ?? 'Impossible de modifier ce compte.';
      alert(message);
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
    <div className="page-utilisateurs">
      <header className="entete-utilisateurs">
        <div>
          <h1>Comptes utilisateurs</h1>
          <p className="sous-titre">{donnees.totalElements} compte(s)</p>
        </div>
        <Link to="/parcours" className="bouton-secondaire">← Retour a la liste</Link>
      </header>

      <table className="tableau-utilisateurs">
        <thead>
          <tr>
            <th>Nom</th>
            <th>Email</th>
            <th>Role</th>
            <th>Statut</th>
            <th>Inscrit le</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {donnees.contenu.map((compte) => {
            const estSoiMeme = compte.email === utilisateurConnecte?.email;
            const enCours = actionEnCoursId === compte.id;

            return (
              <tr key={compte.id} className={!compte.actif ? 'ligne-inactive' : ''}>
                <td>{compte.nom}</td>
                <td>{compte.email}</td>
                <td>
                  <select
                    value={compte.role}
                    onChange={(e) => gererChangementRole(compte, e.target.value)}
                    disabled={estSoiMeme || enCours}
                  >
                    <option value="TOURISTE">Touriste</option>
                    <option value="ADMIN">Administrateur</option>
                  </select>
                </td>
                <td>
                  <span className={`badge-actif ${compte.actif ? 'badge-actif-oui' : 'badge-actif-non'}`}>
                    {compte.actif ? 'Actif' : 'Desactive'}
                  </span>
                </td>
                <td className="cellule-date">
                  {new Date(compte.dateCreation).toLocaleDateString('fr-FR')}
                </td>
                <td>
                  {estSoiMeme ? (
                    <span className="etiquette-vous">Vous</span>
                  ) : (
                    <button
                      onClick={() => gererBasculeActif(compte)}
                      disabled={enCours}
                      className={compte.actif ? 'bouton-danger' : 'bouton-secondaire'}
                    >
                      {compte.actif ? 'Desactiver' : 'Reactiver'}
                    </button>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

export default PageUtilisateurs;
