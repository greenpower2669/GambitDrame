# GambitDrame — état fonctionnel et contrat (19/10/2026)
Objectif : application d'échecs accessible Android jouable, à terme entraîneur contre des joueurs humains crédibles.
L'adversaire humain doit échantillonner des coups humains réels, avec continuité de style, et ne doit jamais être assimilé aux meilleurs coups de Stockfish. Les tranches sont des classements Lichess par cadence, non FIDE ; cohorte 400/500 non vérifiée.

## Code compilé et tests Java vérifiés sur branche (CI run 4)
- `ChessGame.java` : état FEN, coups légaux, attaque/échecs, roque, prise en passant, quatre promotions, rois protégés, mat/pat, détection simplifiée 50 coups et triple répétition au niveau activité, matériel insuffisant.
- `MainActivity.java` : plateau tactile, saisie UCI alternative, historique, annulation, reprise et sauvegarde locale, import/copie FEN, chargement de l'ouverture Gambit de dame, bot de démonstration aléatoire **NON calibré**.
- Profils Lichess 400–900 affichés mais désactivés ; Stockfish non intégré ; pas d'import PGN, ni navigation des variantes.
- CI 0.2.1 validée : 18 assertions Java, perft profondeur 2=400 et profondeur 3=8902, assembleDebug réussi et APK Release directe publiée. Aucun test d'installation / gameplay physique réalisé. Profil humain réel, Stockfish et notation PGN toujours absents.

## Principes à conserver
- Accessibilité : fort contraste, plateau responsive, taille raisonnable, entrée de coups au clavier en alternative, icône chevalier vectorielle et marges système Android 16 ; accessibilité TalkBack virtuelle des 64 cases restant à développer.
- Hors ligne par défaut ; pas d'INTERNET dans le manifeste.
- Licence MIT du code original ; ne pas embarquer Stockfish avant clarification licence/source de distribution.
- Mémoires du projet à conserver distinctes des mémoires Djipie et FabCopilot.

## Source Lichess vérifiée (19/10/2026)
- Source officielle `lichess-org/lila/modules/rating/src/main/Glicko.scala` sur master : `minRating = IntRating(400)`, `maxRating = IntRating(4000)`, `standardRankableDeviation = 75`. La notation 400 est donc atteignable comme plancher logiciel, mais un joueur au plancher peut être encore moins fort, ce qui biaise une calibration empirique si ignoré.
- La présence de cohortes **actives, non provisoires, avec ≥ 1000 parties/cadence** pour 400 et 500 n'est toujours pas quantifiée. Ne pas confondre plage admise par le moteur de classement et disponibilité de données.
- Source : https://github.com/lichess-org/lila/blob/master/modules/rating/src/main/Glicko.scala

## Lot v0.3.0 — implémentation à valider par CI et test matériel
- Profil de coups "humain" : tirage stochastique sans vote majoritaire parmi les coups observés par l'explorateur Lichess <1000 en blitz/rapid quand Internet répond ; sinon politique heuristique locale avec bruit, capture/développement/risque, six cibles 400–900 exploratoires et style agressif paramétrable.
- Limites STRICTES : les statistiques de l'explorateur ne vérifient pas individuellement les comptes actifs à 1000 parties, la stabilité de rating, ni une calibration séparée 400–900. Les six étiquettes sont **cibles expérimentales, PAS des scores Elo prouvés**.
- Analyse Stockfish 18 : processus UCI binaire natif ARM64 Android NDK r27c, 1 thread, 32 Mo Hash, MultiPV 3, profondeur 11, centipions côté Blancs et WDL côté Blancs. Ne sélectionne jamais les coups de HumanPolicy.
- Application hybride : Internet facultatif uniquement pour statistiques publiques, repli hors ligne ; build inclus binaire Stockfish et licence GPLv3/source exacte.
- Matrice distribution : téléphone Android 16 ARM64 ; aucun matériel installé par CI, aucun test WDL réel jusqu'au lancement.

## Vérifications CI v0.3.0 réussies
- Runner GitHub du 19/10/2026 : `CHESS_TESTS_OK 18`; `HUMAN_POLICY_TESTS_OK 130 choices e4=61 d4=39` prouve l'échantillonnage non majoritaire dans ce scénario.
- Stockfish 18 officiel `sf_18` commit cb3d4ee9... compilé avec NDK r27c, binaire ARM64 de 113 750 704 octets inclus dans l'APK avec GPLv3 et notice source ; Gradle `BUILD SUCCESSFUL`.
- APK directe 0.3.0 de 79 814 857 octets vérifiée par métadonnées GitHub Release gd-test-6. SHA256 82103cdb09eb578ac6c7644270e9bbd55a1ac809e5437da8fd6e6da87d9af39b.
- **Limite** : ni uciok/readyok, ni panneau affiché, ni coups réels Lichess sous Android, ni parties complètes ne sont confirmés par CI ; appareil Samsung requis.

## Lot v0.3.1 — jauge Stockfish et relecture
- L'échiquier dessine dans sa propre surface une colonne gauche de 44dp alignée à sa hauteur : noir en haut, blanc en bas, séparateur doré ; case 0cp → moitié, transition logistique symétrique centipions, presque pleine pour un mat. Accessible via description « Évaluation Stockfish côté blancs ».
- `ReviewCursor` sépare position courante affichée et dernière position de la partie en cours : changement de position sans muter `positions`, `moves`, FEN de départ ni sauvegarde. Cinq boutons début/avant/play-pause/après/fin, plus reprise explicite.
- 900ms par demi-coup en lecture automatique, interruption par pause/navigation/Android onPause. Lorsqu'une relecture est active, aucun coup humain ni bot n'est appliqué ; tâches bot et réseau obsolètes ignorées par révision.
- `StockfishBridge` étiquette ses évaluations par FEN, associe score CP/mat blanc à MultiPV 1, et utilise `stop`+`isready` comme barrière pour limiter l'affichage de lignes périmées lors d'une navigation rapide. Les positions passées restent analysables.
- Les fonctions sont définies dans le code; certification de compilation/test matériel et lien d'APK attendus avant validation.

## État réellement confirmé v0.3.1
- GitHub Actions #7 : succès, compilation Gradle `BUILD SUCCESSFUL in 46s`. `CHESS_TESTS_OK 18`, `HUMAN_POLICY_TESTS_OK 130`, `REVIEW_AND_GAUGE_TESTS_OK 21`.
- APK prérelease `gd-test-7` publiée : `https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-7/GambitDrame-v0.3.1-debug-arm64.apk`, taille 79 825 609 octets, SHA256 `d5b1062633a218914088fa1d5db70d03a74d7697ba300f755265675c32c5c8f7`. Pas de zip.
- Contrôle zip CI : fichier binaire `lib/arm64-v8a/libstockfish.so` de 113 750 704 octets et `assets/licenses/STOCKFISH-GPLv3.txt` de 35 149 octets.
- Réserve : le rendu graphique de l'échiquier et la réactivité tactile ne sont pas testés sur téléphone par les tests JVM/Gradle.

## Lot 0.3.2 — guidage non visuel
- `ChessNarration.java` transforme position et coups légaux en phrases courtes françaises, annonce matériel/prise en passant/roque/promotion, échec/mat/pat et avantage évalué en CP.
- `AccessibleAudio.java` combine sons Android ToneGenerator, choix Piper Jessica/Pierre, synthèse TTS locale de secours, interruptions/reprise et préférences distinctes sons/voix/vitesse ; aucun flux réseau pour les annonces.
- `PiperSpeechEngine.kt` : Piper VITS `fr_FR-upmc-medium` via Sherpa-ONNX 1.13.8 CPU, sid 0 Jessica et sid 1 Pierre, warmup voix muette, exécuteur arrière-plan, AudioTrack et annulations par génération. Audio de secours via Android si Piper non prêt.
- `MainActivity.java` : sélection tactile parlante, annonce de chaque coup et événements, menus « Décrire la position », « Lire l'échiquier », « Coups possibles », « Répéter », « Écouter l'avantage Stockfish », réglages persistants et voix Piper féminine/masculine.
- CI télécharge et vérifie SHA256 Sherpa AAR et modèle Piper, ajoute notices de sources/licence, vérifie le .onnx packagé et compile les tests Java de phrases.
- LIMITES : pas de reconnaissance vocale pour entrer des coups ; voix Piper installée dans APK mais usage réel Samsung requiert test. Aucun mélange du moteur Stockfish à l'adversaire.

## Intégration v0.3.2 vérifiée en CI
- Release directe `gd-test-12` : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-12/GambitDrame-v0.3.2-debug-arm64.apk; 176 621 445 octets, SHA256 `641ea821ecf57b696b5c3b1179a597745d4d8eb8543ea2c153dfd528cd306ff0`.
- Build GitHub Actions #12 : `BUILD SUCCESSFUL in 1m 8s` ; 18 tests de règles, 130 sélections humain, 21 jauge/navigation et **15 narrations** verts.
- Archives AAR Sherpa et archive modèle vocal vérifiées par SHA-256 dans CI. L'APK contient `assets/tts/piper/pierre/fr_FR-upmc-medium.onnx` (76 682 529 octets), Sherpa libs `libonnxruntime.so`, `libsherpa-onnx-*.so`, GPL Stockfish et Apache licence Sherpa.
- Du fait du modèle local hors ligne et de Stockfish, la taille de l'APK augmente à ~176,6 Mo : prévenir Fab de l'espace nécessaire pour téléchargement et installation.
- L'expérience réelle Android AudioTrack + warmup + synthèse de Jessica/Pierre et latences n'est PAS encore confirmée par test sur le téléphone.

## 0.4.0 : développement en attente de confirmation CI
- StockfishPrecision.java : candidats évalués en centipions depuis le trait, qualité min0/max100, distribution gaussienne autour de moyenne 75 sigma 10 par défaut; candidats égaux tous 100; pas une métrique Elo/Chess.com.
- StockfishOpponent.java : moteur secondaire UCI Stockfish 18, MultiPV=N coups légaux, profondeur 7, timeout 25 secondes, refus complet si moins de N scores, aucune substitution de mauvais coups au hasard. Risques performance et mémoire à tester.
- MainActivity : choix joueur humain Lichess / Stockfish / bot désactivé (deux humains), paramètres cible/sigma, essais non persistants et flèches vectorielles, thèmes pièces contrastés accessibles.
- PiperSpeechEngine.kt remplace annulation immédiate de chaque phrase par une file sérialisée; conserve AudioTrack, warmup muet, attend progression du playbackHeadPosition et pause configurable APRES audio, max12 messages. AccessibleAudio.java réserve Android TTS au cas d'échec de Piper.
- Ne pas confondre code poussé, CI et test téléphone SM-A576B; audit évaluation correcte des coups et coupures vocales en condition réelle.

## GD-009 — Livraison build v0.4.0 vérifiée 20/10/2026
- GitHub Actions run #13 : https://github.com/greenpower2669/GambitDrame/actions/runs/39385921496 => completed success, source 4069713b1d96aaaea77566650d88da8b8d22eebe.
- APK directe NON ZIP : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-13/GambitDrame-v0.4.0-debug-arm64.apk, taille 176 645 865 octets, SHA256 c9d35ce9d65a8004ed3c0224f44c5d55a20afaa3416bd472e2feeaa65932f838, release gd-test-13, asset uploaded.
- Tests logs : CHESS_TESTS_OK 18 ; HUMAN_POLICY_TESTS_OK 130 choices e4=61 d4=39 ; REVIEW_AND_GAUGE_TESTS_OK 21 ; CHESS_NARRATION_TESTS_OK 15 ; STOCKFISH_PRECISION_TESTS_OK 10 ; BOARD_ARROWS_TESTS_OK 4 ; BUILD SUCCESSFUL in 1m18s.
- Contenus natifs vérifiés ZIP APK : libstockfish.so ARM64 113 750 704 octets + GPL3, Piper fr_FR-upmc-medium.onnx 76 682 529 octets + Sherpa licences.
- ATTENTION : CI n'a pas testé l'exécution réelle sur Samsung, ni la sélection all-MultiPV à profondeur7 sur téléphone, ni audio Piper avec première phrase / enchaînement, ni glissés/couleurs en contexte réel. Ne pas prétendre « testé téléphone ».

## GD-010 — Nouveau protocole de coups rapides (20/10/2026)
- Fab préfère le classement des **positions après chacun des coups légaux**, en exploitant les centipions qui sous-tendent la jauge à gauche. Ce n'est pas une lecture des pixels de la jauge.
- Stockfish adversaire Noirs par défaut, cible 80 % et sigma 5 (gaussienne), qualité relative entre pire 0 et meilleur 100; Lichess expérimental conservé en option. Profondeur 1 sur chaque FEN fille avec un seul moteur réutilisé, orientation de score renversée pour le parent. En cas d'échec, aucune substitution aléatoire.
- Mode MultiPV complet existant conservé désactivé par défaut; profondeur réglable, testé plus lent.
- Stylistique : demi-pièces coupées longitudinalement en bois à plat, vue du dessus; second rendu 3D perspective; thème classique conservé; pas de cercle autour des pièces bois.
- Plein écran dialogue immersif, contrôle de flèches, prévisualisation live, annotation translucide et demande de désactiver l'interception tactile parent ScrollView.
- Vérifier sur SM-A576B Android 16 le temps de réponse de l'engine, précision des coups, lecture et lisibilité des silhouettes, toucher plein écran, coût mémoire. CI ne teste pas physiquement le téléphone.

## GD-010 — Validation CI finale, 20/10/2026
- GitHub Actions #22 (39394368345) sur d8aac2e3d21d47b3ac35340f34cf9d9c94b76b63 : completed success ; BUILD SUCCESSFUL in 58s, règles 18 tests, politique humaine 130, précision/orientation Stockfish 15, flèches 4.
- APK directe .apk (pas ZIP), v0.4.1 code 8, dans gd-test-22 : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-22/GambitDrame-v0.4.1-debug-arm64.apk ; taille 176659809 octets.
- Livrée pour test réel Samsung SM-A576B Android 16. CI ne vérifie pas la qualité de l'affichage 3D, les gestes physiques ni le temps de réponse de Stockfish sur ce téléphone. Pas de merge main.

## GD-011 — Instrumentation Stockfish (20/10/2026)
- Retour matériel Fab sur 0.4.1 : le niveau perçu du moteur ne reflète pas le curseur, parfois 100 % malgré réglage faible ou 80 %. Capture 20 demi-coups, pas de preuve sur la cause de chaque sélection.
- Cause potentielle établie dans le code : normalisation linéaire entre min et max, puis choix du voisin le plus proche de la cible; grands écarts CP => choix 100 même si 80 demandé; scores tous identiques => tous 100. Profondeur 1 n'est pas un indicateur de précision humaine.
- 0.4.2 candidate : EngineDiagnostics journal SharedPreferences borné (~65k chars), logcat GambitEngine, persistant; StockfishOpponent enregistre par coup FEN, score CP enfant/racine, candidates qualifiés, score souhaité et réel, cas spéciaux, temps et erreurs; MainActivity affiche bouton GET ERR (copier/partager).
- Ne pas modifier l'algorithme de sélection sur cette livraison : priorité à comprendre les données réelles. Branche proposal uniquement; test smartphone nécessaire.
