# IMMUTABLE / À PRÉSERVER
- Identité Android du squelette : `fr.gambitdrame` (aucune identité Play Store connue ; ne pas changer sans accord).
- Code original MIT ; intégration/distribution Stockfish sous GPLv3 à examiner avant ajout binaire.
- Branche de travail : `proposal/stockfish-multipv-human-training-v1`. Aucune fusion `main` sans approbation explicite de Fab.
- APK debug directement téléchargeable depuis GitHub Release de test ; ne jamais annoncer build ou asset sans preuve.
- Ne jamais qualifier de profils humains calibrés les bots de démonstration.

# Missions explicites de Fab — 19/10/2026
- **GD-001 — Jeu d'échecs Android** : règles légales, touch, roque, en passant, promotions, mats/pats/nulles, historique, annulation, reprise et sauvegarde ; **implémentation initiale soumise aux tests**.
- **GD-002 — Stockfish analytique indépendant** : panneau gauche/adaptatif, centipions, jauge, WDL distinctes, MultiPV, navigation, reproductibilité et bilan GPL. **Non produit**.
- **GD-003 — Adversaires humains Lichess 400/500/600/700/800/900** : joueurs actifs, cote non provisoire, >=1000 parties par cadence, jusqu'à 100 parties récentes/joueur, distribution probabiliste, styles stables, repli documenté, calibration complète. **Non produit** ; bot de démonstration non calibré.
- **GD-004 — Ouvertures extensibles** : Entraînement > Ouvertures > Gambit de dame, exercices, variantes, répétition et feedback. **Point d'entrée initial, cursus non produit**.
- **GD-005 — Livraison Android** : CI réparable, build validé, APK direct, installation humaine sur Samsung SM-A576B Android 16, logs et mémoire synchronisée. **En cours ; contrôle GitHub après commit requis**.
