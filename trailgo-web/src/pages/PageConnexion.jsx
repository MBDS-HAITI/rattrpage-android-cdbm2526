// src/pages/PageConnexion.jsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ArrierePlanTraces from '../components/ArrierePlanTraces';
import './PageConnexion.css';

function PageConnexion() {
  const [email, setEmail] = useState('');
  const [motDePasse, setMotDePasse] = useState('');
  const [erreur, setErreur] = useState(null);
  const [enCours, setEnCours] = useState(false);

  const { connecter } = useAuth();
  const navigate = useNavigate();

  async function gererSoumission(evenement) {
    evenement.preventDefault();
    setErreur(null);
    setEnCours(true);

    try {
      await connecter(email, motDePasse);
      navigate('/parcours');
    } catch (erreurAppel) {
      // L'API renvoie un ProblemDetail (RFC 7807) avec un champ "detail".
      // Le service Spring renvoie le meme message que l'email soit
      // inconnu ou le mot de passe faux : on l'affiche tel quel.
      const message = erreurAppel.response?.data?.detail
        ?? 'Impossible de se connecter. Verifiez votre connexion reseau.';
      setErreur(message);
    } finally {
      setEnCours(false);
    }
  }

  return (
    <div className="page-connexion">
      <ArrierePlanTraces />

      <div className="carte-connexion">
        <h1>TrailGo</h1>
        <p className="sous-titre">Administration des parcours touristiques</p>

        <form onSubmit={gererSoumission}>
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoFocus
          />

          <label htmlFor="motDePasse">Mot de passe</label>
          <input
            id="motDePasse"
            type="password"
            value={motDePasse}
            onChange={(e) => setMotDePasse(e.target.value)}
            required
          />

          {erreur && <p className="message-erreur">{erreur}</p>}

          <button type="submit" disabled={enCours}>
            {enCours ? 'Connexion...' : 'Se connecter'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default PageConnexion;
