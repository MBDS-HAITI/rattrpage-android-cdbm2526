[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/nFc58pc7)
# TrailGo — Plateforme de gestion de parcours touristiques
### Mini-projet · Spring Boot · React · Android natif

---

## Contexte

Une agence de tourisme régionale souhaite moderniser son offre en proposant une plateforme numérique permettant aux visiteurs de découvrir, planifier et suivre des parcours touristiques thématiques (culturel, gastronomique, naturel, historique…).

La plateforme se compose de trois composants à développer en équipe :

| Composant | Technologie | Rôle |
|---|---|---|
| API REST | Spring Boot | Centrale, consommée par les deux fronts |
| Back office | React | Administration des contenus |
| Application mobile | Android natif (Kotlin) | Usage terrain par les touristes |

---

## Architecture globale

```
┌─────────────────┐     ┌─────────────────┐
│   React Admin   │     │  Android App    │
│   (Back office) │     │  (Touriste)     │
└────────┬────────┘     └────────┬────────┘
         │                       │
         └──────────┬────────────┘
                    │  REST / JSON
          ┌─────────▼──────────┐
          │   Spring Boot API  │
          │   + Spring Security│
          └─────────┬──────────┘
                    │
          ┌─────────▼──────────┐
          │   Base de données  │
          │   PostgreSQL       │
          └────────────────────┘
```

---

## Modèle de données (simplifié)

- **Parcours** : titre, description, thème, durée estimée, difficulté, image de couverture, statut (publié / brouillon), tracé GeoJSON (LineString), bbox (enveloppe géographique), distance totale (km)
- **Étape** : nom, description, coordonnées GPS (lat/lng), géométrie PostGIS (Point), ordre, photo, durée de visite
- **Point d'intérêt** : titre, catégorie, adresse, coordonnées, rayon de proximité (m)
- **Zone géographique** : nom, polygone GeoJSON (Polygon), région administrative
- **Utilisateur** : email, rôle (ADMIN / TOURISTE), préférences
- **Avis** : note (1–5), commentaire, date, parcours, auteur
- **Favoris** : relation utilisateur ↔ parcours

---

## Données cartographiques

### Stack spatiale

| Couche | Technologie |
|---|---|
| Base de données spatiale | PostgreSQL + extension **PostGIS** |
| Format d'échange | **GeoJSON** (RFC 7946) |
| Tuiles vectorielles (optionnel) | **pg_tileserv** ou Martin tile server |
| Bibliothèque Java | **Hibernate Spatial** + **JTS (Java Topology Suite)** |
| Carte web (back office) | **Leaflet.js** ou **MapLibre GL JS** |
| Carte mobile | **Google Maps SDK** (Android) |
| Fond de carte | **OpenStreetMap** via Leaflet / tiles publiques |

---

### Fonctionnalités cartographiques par composant

#### API Spring Boot — endpoints spatiaux

**Gestion des tracés**
- `POST /parcours/{id}/trace` — importer un tracé GPX ou GeoJSON (LineString)
- `GET /parcours/{id}/trace` — exporter le tracé en GeoJSON
- Calcul automatique de la distance totale du tracé (`ST_Length`) et de l'altitude cumulée si données GPX

**Requêtes spatiales (PostGIS)**
- `GET /parcours?lat=&lng=&radius=` — parcours dont une étape est dans un rayon donné (`ST_DWithin`)
- `GET /parcours?bbox=` — parcours intersectant une bounding box (`ST_Intersects`)
- `GET /etapes/proches?lat=&lng=&radius=` — étapes à proximité immédiate (`ST_DWithin`)
- Calcul de distance entre la position du touriste et la prochaine étape (`ST_Distance`)

**Zones géographiques**
- CRUD sur les zones (polygones) avec stockage PostGIS (`ST_GeomFromGeoJSON`)
- Association automatique d'un parcours à une zone (`ST_Within`)
- `GET /zones/{id}/parcours` — tous les parcours inclus dans une zone

**Notions mises en œuvre**
`PostGIS` · `Hibernate Spatial` · `JTS Geometry` · `ST_DWithin / ST_Distance / ST_Intersects / ST_Within` · `Projection SRID 4326` · `parsing GPX (JDOM ou GPX4J)`

---

#### Back office React — éditeur cartographique

**Visualisation**
- Carte Leaflet affichant tous les parcours publiés (clustering de marqueurs)
- Couche de chaleur (heatmap) des étapes les plus visitées
- Affichage des zones géographiques en polygones colorés par région

**Édition des tracés**
- Dessin interactif du tracé d'un parcours (`Leaflet.draw` ou `MapLibre Draw`)
- Ajout / déplacement des étapes par glisser-déposer sur la carte
- Définition des zones géographiques par dessin de polygone
- Import d'un fichier GPX avec prévisualisation du tracé avant validation

**Outils d'analyse (bonus)**
- Visualisation du profil altimétrique d'un tracé (graphique SVG)
- Estimation automatique du temps de parcours selon la distance et la difficulté

**Notions mises en œuvre**
`Leaflet.js` · `React-Leaflet` · `Leaflet.draw` · `GeoJSON parsing` · `import GPX` · `clustering (MarkerCluster)`

---

#### Application Android — navigation terrain

**Affichage cartographique**
- Rendu du tracé complet du parcours sur la carte (Polyline depuis GeoJSON)
- Marqueurs différenciés par type d'étape (icônes personnalisées)
- Affichage des points d'intérêt dans un rayon de 200 m autour de la position courante

**Navigation GPS**
- Localisation en temps réel (FusedLocationProviderClient)
- Détection automatique de l'arrivée à une étape (`ST_DWithin` côté serveur ou calcul local Haversine)
- Recentrage automatique de la carte sur la position de l'utilisateur
- Indication de la direction et distance vers la prochaine étape

**Fonctionnement hors ligne**
- Téléchargement du tracé GeoJSON et des tuiles de carte avant le départ (cache Room + tiles en cache local)
- Navigation sans connexion avec reprise de synchronisation au retour en ligne

**Notions mises en œuvre**
`Google Maps SDK` · `FusedLocationProviderClient` · `Polyline / GroundOverlay` · `formule Haversine` · `Room (cache GeoJSON)` · `WorkManager (sync différée)`

---

## Composant 1 — API Spring Boot

### Fonctionnalités attendues

**Gestion des parcours**
- CRUD complet sur les parcours et leurs étapes
- Filtrage par thème, durée, difficulté
- Recherche full-text sur titre et description
- Upload d'images (stockage local ou S3)

**Sécurité**
- Authentification JWT (Spring Security)
- Rôles ADMIN et TOURISTE
- Endpoints publics (consultation) vs protégés (création, modification)

**Autres**
- Pagination et tri sur tous les endpoints de liste
- Validation des données (`@Valid`, messages d'erreur structurés)
- Documentation Swagger / OpenAPI
- Tests unitaires sur la couche service (JUnit 5 + Mockito)

### Notions mises en œuvre
`Spring Data JPA` · `Spring Security + JWT` · `Bean Validation` · `MapStruct` · `Swagger` · `JUnit / Mockito` · `PostGIS / Hibernate Spatial` · `JTS Geometry`

---

## Composant 2 — Back office React

### Fonctionnalités attendues

**Dashboard**
- Nombre de parcours publiés / brouillons
- Moyenne des notes par parcours
- Top 5 parcours les plus consultés

**Gestion des parcours**
- Liste paginée avec filtres et recherche
- Formulaire de création / édition avec gestion des étapes (drag & drop pour réordonner)
- Upload d'image de couverture
- Bouton publier / dépublier

**Gestion des utilisateurs**
- Liste des comptes inscrits
- Activation / désactivation d'un compte
- Changement de rôle

**Modération des avis**
- Liste des avis avec note et commentaire
- Suppression d'un avis signalé

### Notions mises en œuvre
`React Router` · `Context API ou Redux` · `React Hook Form` · `Axios` · `composants contrôlés` · `gestion des tokens JWT` · `Leaflet.js / React-Leaflet` · `Leaflet.draw` · `GeoJSON`

---

## Composant 3 — Application Android (Kotlin)

### Fonctionnalités attendues

**Authentification**
- Écran de connexion / inscription
- Persistance du token (EncryptedSharedPreferences)

**Exploration**
- Liste des parcours avec filtres par thème et difficulté
- Fiche détail d'un parcours avec ses étapes
- Carte interactive affichant les étapes (Google Maps SDK)
- Ajout / suppression d'un parcours en favori

**Navigation terrain**
- Mode "en cours" : affichage de l'étape courante avec GPS
- Marqueur de position en temps réel sur la carte
- Progression visuelle entre les étapes

**Avis**
- Soumission d'une note et d'un commentaire après un parcours

### Notions mises en œuvre
`MVVM + ViewModel` · `Retrofit + OkHttp` · `Room (cache offline + GeoJSON)` · `Google Maps SDK` · `FusedLocationProviderClient` · `RecyclerView` · `Navigation Component` · `Coroutines / Flow` · `WorkManager`

---

## Organisation suggérée

### Équipe : 3 à 4 étudiants
| Profil | Responsabilité principale |
|---|---|
| Étudiant A | Spring Boot (API + sécurité) |
| Étudiant B | Spring Boot (données + tests) + DevOps |
| Étudiant C | React (back office) |
| Étudiant D | Android |

> La qualité de l'intégration entre les composants est évaluée collectivement.


---

## Critères d'évaluation

### Back-end (Spring Boot) — 30 pts
- Qualité et cohérence de l'API REST (endpoints, codes HTTP, pagination)
- Sécurité (JWT, rôles, validation)
- Tests unitaires (couverture ≥ 70% couche service)
- Documentation Swagger complète

### Front office admin (React) — 25 pts
- Fonctionnalités complètes (CRUD, dashboard)
- Gestion des états et des erreurs
- UX claire et responsive

### Application mobile (Android) — 30 pts
- Navigation fluide et conforme MVVM
- Intégration carte et GPS
- Gestion offline (Room)
- Qualité du code Kotlin (coroutines, flow)

### Intégration & soutenance — 15 pts
- Cohérence de la plateforme end-to-end
- Qualité de la démo
- Réponses aux questions techniques

---

## Extensions optionnelles (bonus)

- Notifications push (Firebase Cloud Messaging) lors de la publication d'un nouveau parcours
- Export PDF d'un parcours depuis le back office
- Mode sombre sur l'application Android
- Internationalisation (FR / EN) sur les deux fronts
- Déploiement Docker Compose (API + base de données)
