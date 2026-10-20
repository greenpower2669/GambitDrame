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

## GD-008 — Demande de Fab : son et voix IA pour déficience visuelle (19/10/2026)
- Fab demande des effets sonores et de la voix pour compenser la difficulté à voir les pièces et la jauge. Livraison à **vérifier** : lecture des pièces touchées, destinations légales, déplacements, prises, échec/mat/promotion/roque, historique, lecture d'ensemble du plateau et avantage Stockfish.
- Sons courts différenciés : sélection, pose, prise, échec, erreur, navigation. Options ON/OFF indépendantes, volume Android respecté.
- Reprendre véritable **Piper français UPMC Medium Sherpa-ONNX 1.13.8** déjà validé par GeckoDoku : Jessica sid=0 voix féminine par défaut, Pierre sid=1 option, warmup audio muet, synthèse locale via CPU et lecture AudioTrack. Voix Android TTS française hors ligne comme secours en cas de chargement/échec Piper.
- Ne jamais prétendre que Piper est intégré avant succès de la CI et vérification du modèle dans l'APK. Se conformer aux licences des modèles/ONNX, publier .apk directe de test, garder `main` intacte.
- Modes voix et sons indépendants et persistants, vitesse réglable, annonces d'évaluation uniquement sur demande sauf option automatique, éviter bavardage permanent.

## GD-008 — Livraison build #12 vérifiée, version 0.3.2 (19/10/2026)
- GitHub Actions https://github.com/greenpower2669/GambitDrame/actions/runs/39302856790 — **succès** sur commit `a3a6471de25fa1579e13318969b1aaac198a211d`.
- APK Android ARM64 directe https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-12/GambitDrame-v0.3.2-debug-arm64.apk (176 621 445 octets ; SHA-256 `641ea821ecf57b696b5c3b1179a597745d4d8eb8543ea2c153dfd528cd306ff0`), publiée GitHub prerelease `gd-test-12`. Pas d'archive ZIP à décompresser pour Fab.
- Vérifiés par CI dans APK : Sherpa-ONNX 1.13.8, Piper UPMC VITS 76 682 529 octets, échantillons espeak-ng-data, sources/licences, binaire Stockfish 18 ARM64 113 750 704 octets. Tests 18+130+21+15 réussis.
- **Non acquis** jusqu'au test SM-A576B : lancement/latence/sid Jessica-Pierrre d'Android AudioTrack, coexistence TalkBack, volumes, chauffe et fonctionnement Stockfish réel. Pas de merge main.

## GD-009 : mission du 20/10/2026 — voix, Stockfish relatif, essais et flèches
- Voix : warmup Piper réellement joué muet par AudioTrack persistant, lecture intégrale avant prochain message, file bornée et délai après lecture de 100 à 2000 ms. Interruption uniquement sur arrêt explicite.
- Adversaire : conserver le profil humain Lichess expérimental comme option; nouveau vrai Stockfish calculant tous coups légaux par MultiPV=N et profondeur 7, puis sélectionner un coup selon score relatif minimum 0, maximum 100 avec cible réglable 0..100 et gaussienne sigma 0..30. Score relatif de POSITION, jamais prétendre précision officielle d'une partie ou Elo calibré. Si évaluation incomplète, signaler échec et ne jouer aucun coup.
- Essayer des coups : position copiée en pile fictive, annuler/quitter, aucune mutation de l'historique sauvegardé. Réanalyser via Stockfish lors des essais, et laisser bot arrêté.
- Flèches vectorielles tactiles par glissé d'une case à une autre, toggle du segment par répétition, effacement manuel, max 12, couleur orange.
- Pièces : style ivoire/ébène, minimal et contraste élevé, fonds lisibles, sélecteur persistant.
- Identité fr.gambitdrame, branche proposal/stockfish-multipv-human-training-v1 seulement, pas de merge main, APK directe non ZIP et CI obligatoirement vérifiée avant annonce.

## GD-009 — Livraison build v0.4.0 vérifiée 20/10/2026
- GitHub Actions run #13 : https://github.com/greenpower2669/GambitDrame/actions/runs/39385921496 => completed success, source 4069713b1d96aaaea77566650d88da8b8d22eebe.
- APK directe NON ZIP : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-13/GambitDrame-v0.4.0-debug-arm64.apk, taille 176 645 865 octets, SHA256 c9d35ce9d65a8004ed3c0224f44c5d55a20afaa3416bd472e2feeaa65932f838, release gd-test-13, asset uploaded.
- Tests logs : CHESS_TESTS_OK 18 ; HUMAN_POLICY_TESTS_OK 130 choices e4=61 d4=39 ; REVIEW_AND_GAUGE_TESTS_OK 21 ; CHESS_NARRATION_TESTS_OK 15 ; STOCKFISH_PRECISION_TESTS_OK 10 ; BOARD_ARROWS_TESTS_OK 4 ; BUILD SUCCESSFUL in 1m18s.
- Contenus natifs vérifiés ZIP APK : libstockfish.so ARM64 113 750 704 octets + GPL3, Piper fr_FR-upmc-medium.onnx 76 682 529 octets + Sherpa licences.
- ATTENTION : CI n'a pas testé l'exécution réelle sur Samsung, ni la sélection all-MultiPV à profondeur7 sur téléphone, ni audio Piper avec première phrase / enchaînement, ni glissés/couleurs en contexte réel. Ne pas prétendre « testé téléphone ».

## GD-010 — Validation Fab du 20/10/2026 : essai nouveau moteur, bois, flèches
- Fab autorise le développement et le test sur la branche de travail, sans merge main.
- Adversaire par défaut Stockfish Noir, cible 80 %, sigma 5 ; mode rapide par défaut: chaque coup légal appliqué sur copie, résultat analysé par Stockfish depth 1, conversion score depuis le trait de l'enfant vers le joueur ayant joué (négation), classement pire 0 / meilleur 100, sélection au plus proche d'une cible gaussienne. Refuser un choix si les évaluations sont incomplètes.
- Conserver en option (désactivée par défaut) l'exploration MultiPV à profondeur réglable.
- Deux styles bois: demi-pièces longitudinales à plat (défaut), perspective 3D commutable ; anciens thèmes préservés. Retirer les médaillons circulaires sur les deux modes bois.
- Flèches semi-transparentes avec aperçu pendant déplacement, empêcher ScrollView d'intercepter le glisser; plateau en vrai plein écran avec commandes flèches/effacer/retour.
- v0.4.1 code 8, tests JVM étendus conversion orientation score enfant, CI GitHub Actions. Test physique Samsung non automatisable ici.
- Attention: comparer score centipions des positions résultantes, pas des pourcentages affichés par la jauge ; profondeur 1 n'est pas une recherche exhaustive de toutes les suites.
