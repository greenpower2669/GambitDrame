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

## GD-006 — livraison intermédiaire vérifiée (19/10/2026)
- Workflow #6 : https://github.com/greenpower2669/GambitDrame/actions/runs/39218250281 — succès sur commit `6f2839f147f0a90d2f9c00919bf7b44adcfbacbe`.
- Stockfish 18 compilé ARM64, moteur `lib/arm64-v8a/libstockfish.so` et licence GPLv3 vérifiés dans l'APK ; application Android compilée.
- APK directe publiée : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-6/GambitDrame-v0.3.0-debug-arm64.apk (79 814 857 octets, SHA256 `82103cdb09eb578ac6c7644270e9bbd55a1ac809e5437da8fd6e6da87d9af39b`).
- **Livré à valider sur Samsung** : présence binaire prouvée, lancement effectif et qualité des coups encore non testés physiquement. Cohortes 400–900 et engagements de performance non validés.

## GD-007 — Ordre explicite de Fab : jauge et télécommande de relecture (19/10/2026)
- Jauge Stockfish façon Lichess **collée verticalement à gauche du plateau même en mode portrait**, noirs en haut, blancs en bas, rapport 50/50 à zéro, progression contraste élevé selon score centipions ou mat, lue vocalement par libellé d'accessibilité.
- Navigation de partie : 5 boutons visibles **début / précédent / lecture automatique-pause / suivant / fin**, plus « Reprendre la partie » pour revenir au jeu. Les positions parcourues **ne mutent pas** la partie sauvegardée.
- Chaque position affichée déclenche une nouvelle recherche Stockfish ; le moteur humain reste indépendant de Stockfish et ne joue pas pendant une relecture. Contrôles TalkBack en français.
- Version 0.3.1, travail sur branche `proposal/stockfish-multipv-human-training-v1`. APK directe GitHub. Pas de merge main. Vérifier CI et matériel avant d'annoncer les fonctions comme éprouvées.

## GD-007 — Livraison vérifiée, CI GitHub #7
- Build Android 0.3.1 : succès `https://github.com/greenpower2669/GambitDrame/actions/runs/39297601721`, code source commit `47aaf2d35968ce2299de7e52c8c223ed6621459b`.
- APK directe (sans zip) publiée : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-7/GambitDrame-v0.3.1-debug-arm64.apk; taille 79 825 609 octets ; SHA256 `d5b1062633a218914088fa1d5db70d03a74d7697ba300f755265675c32c5c8f7`.
- Tests : règles 18 ; choix d'adversaire probabiliste 130 ; navigation/jauge 21. Stockfish natif ARM64 et GPLv3 vérifiés à l'intérieur de l'APK.
- Validation matérielle Android 16/visibilité de la jauge/exécution UCI reste à effectuer sur le Samsung ; ne pas confondre build réussi et essai matériel.
