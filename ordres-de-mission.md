# IMMUTABLE / À PRÉSERVER
- Identité Android du squelette : `fr.gambitdrame` (aucune identité Play Store connue ; ne pas changer sans accord).
- Code original MIT ; intégration/distribution Stockfish sous GPLv3 à examiner avant ajout binaire.
- Branche de travail : `proposal/stockfish-multipv-human-training-v1`. Aucune fusion `main` sans approbation explicite de Fab.
- APK debug directement téléchargeable depuis GitHub Release de test ; ne jamais annoncer build ou asset sans preuve.
- Ne jamais qualifier de profils humains calibrés les bots de démonstration.

# Missions explicites de Fab — 19/10/2026
- **GD-001 — Jeu d'échecs Android** : règles légales, touch, roque, en passant, promotions, mats/pats/nulles, historique, annulation, reprise et sauvegarde ; **première version testée par CI, à valider sur téléphone ; les règles FIDE complètes restent à auditer**.
- **GD-002 — Stockfish analytique indépendant** : panneau gauche/adaptatif, centipions, jauge, WDL distinctes, MultiPV, navigation, reproductibilité et bilan GPL. **Non produit**.
- **GD-003 — Adversaires humains Lichess 400/500/600/700/800/900** : joueurs actifs, cote non provisoire, >=1000 parties par cadence, jusqu'à 100 parties récentes/joueur, distribution probabiliste, styles stables, repli documenté, calibration complète. **Non produit** ; bot de démonstration non calibré.
- **GD-004 — Ouvertures extensibles** : Entraînement > Ouvertures > Gambit de dame, exercices, variantes, répétition et feedback. **Point d'entrée initial, cursus non produit**.
- **GD-005 — Livraison Android** : CI réparable, build validé, APK direct, installation humaine sur Samsung SM-A576B Android 16, logs et mémoire synchronisée. **CI et APK v0.2.1 confirmées, installation Samsung à valider**.

## Livraison intermédiaire vérifiée
- Build run 3 (19/10/2026) : **succès**, moteur testé par javac, APK v0.2.0 publiée sur `gd-test-3`, SHA256 `cdec6b6362f1ab2a1354ad3b43e812c1a564830d625ee53c007b4e8e622f6686`.
- Correctifs 0.2.1 : import FEN, icône et insets, perft : **en cours de revalidation CI** ; cette ligne ne constitue pas une preuve de build 0.2.1.

- Run 4 : https://github.com/greenpower2669/GambitDrame/actions/runs/39175569139 ; 18 contrôles Java, build et publication réussis. APK `GambitDrame-v0.2.1-debug.apk` dans `gd-test-4`, SHA256 `e83a3f364118d2129d7ff0488bbeeb5ba14429059fe261c630850fe3ecad6474`.

- Dernière livraison test vérifiée : CI run 5 réussie (commit `fd44e601`), pré-release `gd-test-5`, APK directe v0.2.1, SHA256 `dff95559977bf63bd1166d78b19322e27177fe54e342df4ffcac0d46a7db8ee7`. Statut **Livré, à valider sur Android 16** ; GD-002 et GD-003 non livrées.

## GD-006 — Ordre supplémentaire de Fab (19 octobre 2026)
- Fab a constaté : « Il est vide pas de joueur humain ni stockfish ^^ ... push ça ». Mission : intégrer les deux éléments et une APK directe.
- Intervention v0.3.0 sur la branche autorisée : modèle de coups probabilistes Lichess (<1000 non filtré) + politique heuristique locale ; Stockfish 18 ARM64 pour analyse UCI seulement (panneau évaluations/WDL/MultiPV).
- **Livraison provisoire non validée** tant que compilation/résultat réel du workflow et installation Samsung ne sont pas confirmés. Le calibrage strict 400–900 et les critères >=1000 parties/joueur restent ouverts (GD-003). Pas de merge main.
