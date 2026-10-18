# GambitDrame — Cahier des charges 2.0 (proposition)

Statut : conception, à valider avant modification du code. Branche isolée ; ne pas fusionner sans validation.

## Objectif
Créer un entraîneur d'échecs crédible : pas de sacrifices gratuits provoqués par une réduction artificielle de force.

## Interface
- Échiquier et panneau d'analyse Stockfish sur la gauche (adapter sur petit écran).
- Barre d'avantage blanc/noir ; afficher séparément l'évaluation et WDL (victoire/nulle/défaite), sans assimiler évaluation en centipions et probabilité.
- Dernier coup, meilleurs coups candidats, historique des évaluations ; choix d'affichage et accessibilité.

## Politique de recherche — décision de conception
- Commencer avec une seule instance Stockfish et l'option UCI MultiPV (3 à 5 candidats réglables).
- Le caractère déterministe dépend des conditions : même position, binaire, NNUE, options, un thread, profondeur/nœuds fixes et état de recherche contrôlé donnent généralement un résultat reproductible. Le multithreading et les limites temporelles peuvent changer l'ordre et le résultat.
- Ne PAS paralléliser des instances identiques par défaut. Prévoir un banc de test facultatif pour quantifier la divergence avant toute activation.
- Comparer 30 positions variées × 10 répétitions : coups de tête, évaluations, dispersion, temps CPU, mémoire ; séparer reproductibilité et diversité utile.
- Si expérimentation : plusieurs moteurs avec budgets/options/amorçages explicitement distincts ; ne pas présenter la moyenne des scores comme un coup jouable. Une divergence n'implique pas un comportement humain.

## Adversaire humain — approche prioritaire
- MultiPV est réservé à l'analyse, pas à la génération principale des coups de l'adversaire : les meilleurs candidats ne reproduisent pas la distribution des coups humains.
- Construire une distribution empirique des coups effectivement joués à partir de parties humaines Lichess, stratifiées par tranche de classement, cadence, phase de jeu et contexte de position ; tenir compte de la taille de l'échantillon et des biais de sélection.
- Échantillonner un coup selon cette distribution, avec un garde-fou tactique Stockfish calibré sur des erreurs réellement observées au niveau ciblé, et non un filtre absolu qui supprimerait toutes les erreurs humaines.
- Pour les positions rares ou inédites, prévoir un modèle de politique de coups entraîné sur parties humaines ou un mécanisme de repli explicitement évalué ; un simple livre d'ouvertures ne suffit pas.
- Distinguer variance intrinsèque des choix humains et incertitude statistique des fréquences estimées : davantage de données réduit la seconde, pas la première.
- MultiPV et éventuelles recherches parallèles restent un laboratoire d'analyse et de reproductibilité, pas une garantie de niveau Elo.
- Prévoir provenance, Elo, nombre d'occurrences, respect des conditions de l'API et cache local pour hors-ligne.
- Évaluer les profils humains par tests aveugles, erreurs en centipions, blunders, sacrifices, diversité, crédibilité, consommation.

## Cohorte humaine et prévention de la « sagesse des foules »
- Sélectionner des joueurs actifs, avec au moins 1 000 parties classées (seuil configurable), classement non provisoire et suffisamment stable sur une période récente ; contrôler la cadence, les variations de classement et l'inactivité.
- Pour chaque joueur retenu, prendre jusqu'à ses 100 dernières parties pertinentes, en conservant son classement au moment de chaque partie si disponible ; éviter de considérer l'Elo actuel comme celui de toutes ses anciennes parties.
- Échantillonner plusieurs joueurs d'une tranche de classement autour du niveau cible, sans surpondérer ceux qui jouent le plus ; tenir compte de la couleur, de la cadence, de l'ouverture et de la phase de jeu.
- Ne PAS choisir le coup majoritaire, le meilleur coup selon Stockfish ou la moyenne des coups : ces agrégations peuvent augmenter la force et supprimer des erreurs humaines.
- Tirer au hasard dans la distribution des coups observés pour une position donnée ; envisager un tirage hiérarchique (joueur puis coup) pour préserver des styles individuels et des cohérences de partie.
- Problème de couverture : 100 parties par joueur ne garantissent pas que la position courante soit observée ; prévoir une politique de généralisation ou de repli, validée expérimentalement.
- Contrôler le niveau effectif par matchs contre des adversaires de référence et intervalles de confiance, plutôt que supposer qu'une cohorte de 1 200 produit exactement 1 200 Elo.
- Stockfish reste une jauge et un analyseur indépendant : MultiPV et analyse de variations/reprise de coups pour expliquer pourquoi l'évaluation change ; pas le moteur principal de choix des coups humains.

## Navigation
- Gambit de dame existant déplacé dans « Entraînement > Ouvertures > Gambit de dame » ; architecture extensible.
- Modes : jouer, analyser, laboratoire comparatif ; réglages MultiPV, force, temps, profil.

## Technique et licences
- Favoriser moteur local, analyses enregistrées, ressources plafonnées sur Android.
- Code original MIT ; Stockfish GPLv3 conservé et obligations de redistribution à examiner si assemblage diffusé.
- Pas de modification de code ou merge main sans validation. Auditer les fichiers du projet avant implémentation.

## Phases et critères d'acceptation
1. Auditer architecture et comportement actuel ; préserver le gambit de dame.
2. Intégrer Stockfish UCI et panneau d'évaluation, WDL et MultiPV ; tester exactitude.
3. Réaliser banc de reproductibilité et coûts ; n'activer la parallélisation que si bénéfice mesuré.
4. Construire sélection de coups crédibles et comparer à échantillon de parties Lichess.
5. Tests Android, ergonomie, performance et publication APK de test sur branche dédiée.

## À trancher
- Plateforme actuelle exacte et moteur déjà présent ; niveau Elo et cadence ciblés ; mode hors-ligne strict ou hybride ; règles de sélection des coups ; limites RAM/CPU.
