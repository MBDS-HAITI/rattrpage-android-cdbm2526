-- =============================================================
-- TrailGo - Donnees de demonstration
-- src/main/resources/db/migration/V2__donnees_demonstration.sql
--
-- Jouees automatiquement au premier demarrage sur une base vide.
-- Le prof n'a pas besoin d'inserer quoi que ce soit a la main.
-- =============================================================

-- =============================================================
-- 1. UTILISATEURS
-- =============================================================
-- Mot de passe admin    : MotDePasse123
-- Mot de passe touriste : Touriste123
INSERT INTO utilisateur (email, mot_de_passe, nom, role, actif) VALUES
                                                                    ('admin@trailgo.ht',
                                                                     '$2b$10$Q0lA87LTv5Poai/SM0xEtOA/.qNB525zEGbE4obxt9JLk1arM5Vxi',
                                                                     'Admin TrailGo', 'ADMIN', true),
                                                                    ('touriste@trailgo.ht',
                                                                     '$2b$10$2cRpCSr/t/ojvpEvYfBBS.phf9RiFbBNEfBjrd4oFnviXNNO049IC',
                                                                     'Jean-Pierre Beaumont', 'TOURISTE', true);


-- =============================================================
-- 2. ZONE GEOGRAPHIQUE
-- =============================================================
-- Polygone couvrant le centre historique de Port-au-Prince.
-- Les parcours inclus y seront rattaches automatiquement
-- via ST_Within au premier appel de /api/zones/rattachement.
INSERT INTO zone_geographique (nom, region_administrative, polygone) VALUES
                                                                         ('Centre historique de Port-au-Prince', 'Ouest',
                                                                          ST_SetSRID(
                                                                                  ST_GeomFromText('POLYGON((-72.36 18.52, -72.30 18.52, -72.30 18.57,
                               -72.36 18.57, -72.36 18.52))'),
                                                                                  4326)),
                                                                         ('Petionville', 'Ouest',
                                                                          ST_SetSRID(
                                                                                  ST_GeomFromText('POLYGON((-72.31 18.50, -72.27 18.50, -72.27 18.53,
                               -72.31 18.53, -72.31 18.50))'),
                                                                                  4326));


-- =============================================================
-- 3. PARCOURS
-- =============================================================
-- Parcours 1 : PUBLIE avec trace
INSERT INTO parcours (titre, description, theme, duree_estimee_min, difficulte,
                      statut, trace, bbox, distance_totale_km,
                      zone_id, nb_consultations)
VALUES (
           'Le vieux Port-au-Prince historique',
           'Une promenade dans le coeur historique de la capitale : la cathedrale,
            le Champ de Mars et le Marche en Fer, temoins de l''architecture du
            XIXe siecle et de la resilience haitienne apres le seisme de 2010.',
           'HISTORIQUE', 180, 'FACILE', 'PUBLIE',
           ST_SetSRID(
                   ST_GeomFromText('LINESTRING(-72.3395 18.5479, -72.3383 18.5462,
                                -72.3378 18.5426, -72.3401 18.5432,
                                -72.3419 18.5461)'),
                   4326),
           ST_SetSRID(
                   ST_GeomFromText('POLYGON((-72.3419 18.5426, -72.3378 18.5426,
                               -72.3378 18.5479, -72.3419 18.5479,
                               -72.3419 18.5426))'),
                   4326),
           -- Distance pre-calculee ; sera recalculee par ST_Length si on
           -- re-importe le trace depuis l'interface.
           1.194,
           1,   -- zone Centre historique
           42
       );

-- Parcours 2 : PUBLIE avec trace - theme gastronomique
INSERT INTO parcours (titre, description, theme, duree_estimee_min, difficulte,
                      statut, trace, bbox, distance_totale_km,
                      zone_id, nb_consultations)
VALUES (
           'Route du rhum et du cafe',
           'Decouvrez les saveurs d''Haiti a travers ses distilleries artisanales
            et ses bruleries de cafe. Un parcours pour les amateurs de gastronomie
            qui souhaitent comprendre le savoir-faire local.',
           'GASTRONOMIQUE', 240, 'MOYEN', 'PUBLIE',
           ST_SetSRID(
                   ST_GeomFromText('LINESTRING(-72.2852 18.5731, -72.2890 18.5698,
                                -72.2921 18.5720, -72.2950 18.5695)'),
                   4326),
           ST_SetSRID(
                   ST_GeomFromText('POLYGON((-72.2950 18.5695, -72.2852 18.5695,
                               -72.2852 18.5731, -72.2950 18.5731,
                               -72.2950 18.5695))'),
                   4326),
           3.472,
           NULL,  -- hors zone Centre historique
           28
       );

-- Parcours 3 : PUBLIE - theme naturel, difficulte difficile
INSERT INTO parcours (titre, description, theme, duree_estimee_min, difficulte,
                      statut, trace, bbox, distance_totale_km,
                      zone_id, nb_consultations)
VALUES (
           'Randonnee du Morne Calebasse',
           'Ascension du Morne Calebasse au-dessus de Petionville, avec une vue
            panoramique sur la baie de Port-au-Prince et l''ile de la Gonave par
            temps clair. Sentier boise, depart a l''aube recommande.',
           'NATUREL', 300, 'DIFFICILE', 'PUBLIE',
           ST_SetSRID(
                   ST_GeomFromText('LINESTRING(-72.2850 18.5120, -72.2870 18.5180,
                                -72.2890 18.5250, -72.2910 18.5310,
                                -72.2930 18.5380)'),
                   4326),
           ST_SetSRID(
                   ST_GeomFromText('POLYGON((-72.2930 18.5120, -72.2850 18.5120,
                               -72.2850 18.5380, -72.2930 18.5380,
                               -72.2930 18.5120))'),
                   4326),
           7.821,
           2,   -- zone Petionville
           15
       );

-- Parcours 4 : PUBLIE - theme culturel, facile
INSERT INTO parcours (titre, description, theme, duree_estimee_min, difficulte,
                      statut, trace, bbox, distance_totale_km,
                      zone_id, nb_consultations)
VALUES (
           'Art et muralisme du Centre-Ville',
           'Promenade artistique a travers les ruelles ornees de fresques murales
            qui racontent l''histoire et la culture haitienne. Rencontres avec des
            artistes locaux, visite de galeries improvisees.',
           'CULTUREL', 150, 'FACILE', 'PUBLIE',
           ST_SetSRID(
                   ST_GeomFromText('LINESTRING(-72.3350 18.5450, -72.3370 18.5430,
                                -72.3390 18.5410, -72.3410 18.5430,
                                -72.3430 18.5450)'),
                   4326),
           ST_SetSRID(
                   ST_GeomFromText('POLYGON((-72.3430 18.5410, -72.3350 18.5410,
                               -72.3350 18.5450, -72.3430 18.5450,
                               -72.3430 18.5410))'),
                   4326),
           1.856,
           1,   -- zone Centre historique
           67
       );

-- Parcours 5 : BROUILLON - pas encore publie (pour montrer le filtre)
INSERT INTO parcours (titre, description, theme, duree_estimee_min, difficulte,
                      statut, nb_consultations)
VALUES (
           'Bord de mer de Bizoton',
           'Parcours cote en cours de preparation.',
           'NATUREL', 120, 'FACILE', 'BROUILLON', 0
       );


-- =============================================================
-- 4. ETAPES
-- =============================================================
-- Parcours 1 : Le vieux Port-au-Prince historique
INSERT INTO etape (parcours_id, nom, description, position, ordre, duree_visite_min) VALUES
                                                                                         (1, 'Cathedrale Notre-Dame',
                                                                                          'Ruines de la cathedrale du XIXe siecle, symbole du seisme de 2010
                                                                                           et de la memoire collective haitienne.',
                                                                                          ST_SetSRID(ST_MakePoint(-72.3395, 18.5479), 4326), 1, 30),
                                                                                         (1, 'Champ de Mars',
                                                                                          'Vaste place publique entouree de batiments gouvernementaux et de
                                                                                           statues de heroes nationaux : Dessalines, Petion, Christophe.',
                                                                                          ST_SetSRID(ST_MakePoint(-72.3378, 18.5426), 4326), 2, 45),
                                                                                         (1, 'Marche en Fer',
                                                                                          'Marche historique construit en 1891, importe de France en pieces
                                                                                           detachees. Reconverti en marche artisanal et alimentaire.',
                                                                                          ST_SetSRID(ST_MakePoint(-72.3419, 18.5461), 4326), 3, 60);

-- Parcours 2 : Route du rhum et du cafe
INSERT INTO etape (parcours_id, nom, description, position, ordre, duree_visite_min) VALUES
                                                                                         (2, 'Distillerie Barbancourt',
                                                                                          'La plus celebre distillerie de rhum d''Haiti, fondee en 1862.
                                                                                           Visite des chais et degustation des differentes cuvees.',
                                                                                          ST_SetSRID(ST_MakePoint(-72.2852, 18.5731), 4326), 1, 90),
                                                                                         (2, 'Brulerie Rebo',
                                                                                          'L''une des premieres torrefactions d''Haiti, avec des cafes
                                                                                           de la region de Thiotte et du Plateau Central.',
                                                                                          ST_SetSRID(ST_MakePoint(-72.2921, 18.5720), 4326), 2, 60),
                                                                                         (2, 'Marche artisanal de Kenscoff',
                                                                                          'Marche de producteurs locaux : cafes, cacao, mangues Francisque
                                                                                           et epices typiques de la region des Mornes.',
                                                                                          ST_SetSRID(ST_MakePoint(-72.2950, 18.5695), 4326), 3, 45);

-- Parcours 3 : Randonnee du Morne Calebasse
INSERT INTO etape (parcours_id, nom, description, position, ordre, duree_visite_min) VALUES
                                                                                         (3, 'Depart : Bois Verna',
                                                                                          'Point de depart dans le quartier de Bois Verna, a l''ombre des
                                                                                           grands arbres. Preparez eau et collation.',
                                                                                          ST_SetSRID(ST_MakePoint(-72.2850, 18.5120), 4326), 1, 15),
                                                                                         (3, 'Crete intermediaire',
                                                                                          'Premiere vue degagee sur la plaine du Cul-de-Sac et les
                                                                                           bidonvilles qui grimpent les pentes.',
                                                                                          ST_SetSRID(ST_MakePoint(-72.2890, 18.5250), 4326), 2, 20),
                                                                                         (3, 'Sommet Morne Calebasse',
                                                                                          'Vue panoramique a 360 degres : baie de Port-au-Prince, ile de la
                                                                                           Gonave, Petionville et les Mornes de l''interieur.',
                                                                                          ST_SetSRID(ST_MakePoint(-72.2930, 18.5380), 4326), 3, 45);

-- Parcours 4 : Art et muralisme
INSERT INTO etape (parcours_id, nom, description, position, ordre, duree_visite_min) VALUES
                                                                                         (4, 'Quartier Saint-Antoine',
                                                                                          'Premier quartier de muralisme engages : scenes de vie quotidienne
                                                                                           et portraits des heroines de l''independance.',
                                                                                          ST_SetSRID(ST_MakePoint(-72.3350, 18.5450), 4326), 1, 30),
                                                                                         (4, 'Galerie Monnin',
                                                                                          'L''une des plus anciennes galeries d''art d''Haiti, fondee en 1955.
                                                                                           Peintures naives et art contemporain haitien.',
                                                                                          ST_SetSRID(ST_MakePoint(-72.3390, 18.5410), 4326), 2, 40),
                                                                                         (4, 'Place Boyer',
                                                                                          'Place emblematique de Petionville ornee de sculptures en fer
                                                                                           filigranes, specialite des artisans de Croix-des-Bouquets.',
                                                                                          ST_SetSRID(ST_MakePoint(-72.3430, 18.5450), 4326), 3, 25);


-- =============================================================
-- 5. POINTS D'INTERET
-- =============================================================
INSERT INTO point_interet (titre, categorie, adresse, position, rayon_proximite_m) VALUES
                                                                                       ('Hotel Oloffson',
                                                                                        'HEBERGEMENT',
                                                                                        'Avenue Christophe, Port-au-Prince',
                                                                                        ST_SetSRID(ST_MakePoint(-72.3385, 18.5445), 4326), 200),
                                                                                       ('Restaurant Quartier Latin',
                                                                                        'RESTAURANT',
                                                                                        'Petionville, Port-au-Prince',
                                                                                        ST_SetSRID(ST_MakePoint(-72.2890, 18.5128), 4326), 150),
                                                                                       ('Musee du Pantheon National (MUPANAH)',
                                                                                        'MUSEE',
                                                                                        'Champ de Mars, Port-au-Prince',
                                                                                        ST_SetSRID(ST_MakePoint(-72.3372, 18.5431), 4326), 100),
                                                                                       ('Citadelle Laferriere (acces)',
                                                                                        'MONUMENT',
                                                                                        'Cap-Haitien',
                                                                                        ST_SetSRID(ST_MakePoint(-72.3345, 18.5455), 4326), 300),
                                                                                       ('Marche Salomon',
                                                                                        'COMMERCE',
                                                                                        'Avenue Estimee, Port-au-Prince',
                                                                                        ST_SetSRID(ST_MakePoint(-72.3360, 18.5440), 4326), 150);


-- =============================================================
-- 6. AVIS
-- =============================================================
INSERT INTO avis (parcours_id, auteur_id, note, commentaire) VALUES
                                                                 (1, 2, 5, 'Parcours magnifique, tres bien documente. Le guide audio
             disponible sur l''application est un vrai plus.'),
                                                                 (2, 2, 4, 'Excellent moment chez Barbancourt ! La brulerie Rebo est
             moins accessible aux personnes a mobilite reduite.'),
                                                                 (3, 2, 4, 'Randonnee exigeante mais la vue du sommet vaut chaque
             effort. Prevoir de bonnes chaussures et de l''eau.');


-- =============================================================
-- 7. FAVORIS
-- =============================================================
INSERT INTO favori (utilisateur_id, parcours_id) VALUES
                                                     (2, 1),
                                                     (2, 3);


-- =============================================================
-- NOTE POUR LA DEMO
-- =============================================================
-- Comptes disponibles :
--   ADMIN    : admin@trailgo.ht    / MotDePasse123
--   TOURISTE : touriste@trailgo.ht / Touriste123
--
-- Le rattachement automatique des parcours aux zones (ST_Within)
-- est deja effectue ci-dessus via les zone_id en dur.
-- Pour le recalculer : POST /api/zones/rattachement