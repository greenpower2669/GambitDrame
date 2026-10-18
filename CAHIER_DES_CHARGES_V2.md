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

## Adversaire humain
- Sélectionner parmi les candidats MultiPV en fonction de leur plausibilité, du niveau visé et du risque tactique ; filtrer les pertes catastrophiques non plausibles.
- Alternative : utiliser des parties humaines Lichess, avec provenance, cadence, Elo, nombre d'occurrences et respect des conditions de l'API ; cache local pour hors-ligne.
- Évaluer les profils humains par tests aveugles, erreurs en centipions, blunders, sacrifices, diversité, crédibilité, consommation.

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
