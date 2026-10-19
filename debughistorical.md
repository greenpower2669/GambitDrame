# Historique de débogage GambitDrame

## 19/10/2026 — CI initiale en échec (confirmé)
- Exécution : https://github.com/greenpower2669/GambitDrame/actions/runs/39173917141
- Étape fautive : `android-actions/setup-android@v3` ; log exact : `Warning: Failed to find package 'tools'` puis `sdkmanager tools` retourne code 1.
- Il s'agit de la préparation SDK, AVANT Gradle et le compilateur Java. Aucun diagnostic de compilabilité de l'ancien squelette ne peut être tiré de cet échec.
- Correction candidate : supprimer l'action qui demande le paquet obsolète, utiliser `sdkmanager` du runner directement. À confirmer avec un nouveau workflow.
- Version témoin : commit `725ab29d428926e369409bbb02a50effb3f93918`; aucune APK Release avant correction.

## Vérifications restant à faire
- Compilation Java/Android réelle ; installation Samsung Android 16 ; moteur de règles sur positions adversariales/perft ; boucle bot ; accessibilité lecteur d'écran.
- Lichess : cote stable et 1000 parties/cadence à vérifier ; cohorts 400 et 500 inconnues. Aucun téléchargement réalisé.
- Stockfish : code/licence GPLv3, absence d'exécutable distribué dans ce prototype.

## 19/10/2026 — Deuxième CI échouée (confirmé)
- Run : https://github.com/greenpower2669/GambitDrame/actions/runs/39175175362
- Après retrait de setup-android@v3, le runner possède bien `ANDROID_HOME=/usr/local/lib/android/sdk` mais la commande `sdkmanager` n'est pas dans le PATH du shell.
- Message exact : `line 2: sdkmanager: command not found`, code 127.
- Correction candidate : localiser explicitement le binaire sous `$ANDROID_HOME/cmdline-tools` avec `find`, exécuter ce chemin ; déplacer les tests Java en amont pour distinguer l'état du moteur de l'installation SDK. Confirmation par CI attendue.

## 19/10/2026 — Troisième CI réussie (confirmé)
- Exécution : https://github.com/greenpower2669/GambitDrame/actions/runs/39175277306
- Tests Java : succès ; SDK 35 : succès ; Gradle assembleDebug : succès ; fichier APK non vide : vérifié ; publication GitHub : succès.
- GitHub Release : https://github.com/greenpower2669/GambitDrame/releases/tag/gd-test-3 ; `GambitDrame-v0.2.0-debug.apk` 22 193 octets, SHA256 `cdec6b6362f1ab2a1354ad3b43e812c1a564830d625ee53c007b4e8e622f6686`.
- Installation et gameplay sur Samsung SM-A576B : non testés.
- Nouvelle étape : v0.2.1 ajoute import FEN, iconographie et tests perft ; nouveau build attendu.

## 19/10/2026 — Quatrième CI réussie, livraison 0.2.1 (confirmé)
- Workflow : https://github.com/greenpower2669/GambitDrame/actions/runs/39175569139, commit `2abc639322106ea9ceac7adc185c42e47545576a`.
- Logs : `CHESS_TESTS_OK 18`, `BUILD SUCCESSFUL in 25s`, APK présente et GitHub Release upload réussie.
- Asset vérifié par métadonnées GitHub : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-4/GambitDrame-v0.2.1-debug.apk ; taille 24 791 octets ; SHA256 `e83a3f364118d2129d7ff0488bbeeb5ba14429059fe261c630850fe3ecad6474`.
- Périmètre non éprouvé : lancement/installation/ergonomie sur Samsung, plusieurs parties complètes, profils humains Lichess, analyse Stockfish.

## 19/10/2026 — Vérification rating Lichess (code officiel)
- `Glicko.scala` sur la branche master de lichess-org/lila : plancher fixé à 400 (contrairement aux anciens témoignages évoquant 600) ; plafond 4000 ; écart-type `standardRankableDeviation=75`.
- L'existence d'au moins 1000 parties/cadence pour une cohorte de comptes actifs à 400–500 reste **non vérifiée**. Ce n'est pas une preuve que la cohorte existe/est absente.
- https://github.com/lichess-org/lila/blob/master/modules/rating/src/main/Glicko.scala

## 19/10/2026 — Cinquième CI réussie (confirmé)
- Après nettoyage mémoires/workflow, run 5 : https://github.com/greenpower2669/GambitDrame/actions/runs/39175802031 — `success`.
- APK directe sur https://github.com/greenpower2669/GambitDrame/releases/tag/gd-test-5, `GambitDrame-v0.2.1-debug.apk` 24 791 octets, SHA256 `dff95559977bf63bd1166d78b19322e27177fe54e342df4ffcac0d46a7db8ee7`.
- Ce build ne prouve toujours pas l'installation/lancement physique sur Samsung ; aucune demande de fusion main.

## 19/10/2026 — Retour utilisateur et mission de correction
- Fab confirme que 0.2.1 est perçue comme "vide" : absence du vrai joueur humain et de Stockfish. Ce n'est pas une régression : les deux fonctions étaient explicitement non produites dans la version précédente, mais le résultat ne répond pas encore à la mission principale.
- Intervention v0.3.0 : modules HumanPolicy / LichessOpeningExplorer / StockfishBridge, composant UI et workflow de fabrication du moteur Stockfish 18 pour ARM64.
- Aucun build de v0.3.0, APK ou lancement moteur n'est confirmé lors de la rédaction de cette entrée. Résultat du CI à examiner avant livraison. Risque de confusion entre coup humain non calibré et vraie cohortes 400–900 : l'UI doit rester explicite.

## 19/10/2026 — CI + APK 0.3.0 réussies (preuve)
- Workflow : https://github.com/greenpower2669/GambitDrame/actions/runs/39218250281, commit `6f2839f147f0a90d2f9c00919bf7b44adcfbacbe`, status `success`.
- Logs : `CHESS_TESTS_OK 18`, `HUMAN_POLICY_TESTS_OK 130 choices e4=61 d4=39`, `BUILD SUCCESSFUL in 39s`.
- Intégration Stockfish : source officielle sf_18 commit cb3d4ee9..., compilation NDK réussie ; 113 750 704 octets dans `lib/arm64-v8a/libstockfish.so`; licence GPLv3 35 149 octets et notice source dans assets.
- `Unable to strip ... libstockfish.so` affiché en avertissement (binaire déjà strip par workflow), n'a pas empêché build, inclusion et publication ; non démontré comme bug.
- Release https://github.com/greenpower2669/GambitDrame/releases/tag/gd-test-6 ; asset `GambitDrame-v0.3.0-debug-arm64.apk`, taille 79 814 857 octets, SHA256 `82103cdb09eb578ac6c7644270e9bbd55a1ac809e5437da8fd6e6da87d9af39b`.
- **Non vérifiés** : Android 16 ProcessBuilder binaire, handshake UCI, rendu WDL, récupération API Lichess, comportements lors de l'extinction de l'écran, véritables Elo 400–900. Ne pas annoncer comme validés.

## 19/10/2026 — Relecture et jauge latérale (0.3.1), avant build
- Demande Fab : barre de force noir/blanc collée à gauche façon Lichess, bouton play/précédent/suivant/début/fin. Cinq boutons plus sortie explicite relecture et annonces d'accessibilité.
- Risque historique corrigé par conception : passage en relecture ne réécrit pas `positions`/`moves` ; `ReviewCursor` pure Java + tests. Après annuler/nouvelle partie/import FEN, sortir de relecture, recalcul Stockfish.
- Stockfish peut publier des résultats obsolètes si navigation rapide. Pont remplacé : tags FEN, barrière `stop`/`isready`, seule principale MultiPV 1 modifie la jauge.
- À confirmer : compilation JVM/Gradle, log de tests, APK directe, largeur barre 44dp et précision des zones tactiles sur Samsung, autoplay et historique.

## 19/10/2026 — CI v0.3.1 réussie
- GitHub Actions #7 : https://github.com/greenpower2669/GambitDrame/actions/runs/39297601721 (success, commit 47aaf2d...).
- Preuves logs : `CHESS_TESTS_OK 18`, `HUMAN_POLICY_TESTS_OK 130 choices e4=61 d4=39`, `REVIEW_AND_GAUGE_TESTS_OK 21`, `BUILD SUCCESSFUL in 46s`.
- Stockfish officiel ARM64 présent : `lib/arm64-v8a/libstockfish.so` 113 750 704 octets ; licence GPL 35 149 octets et notice source vérifiées.
- GitHub Release `gd-test-7` : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-7/GambitDrame-v0.3.1-debug-arm64.apk; 79 825 609 octets ; sha256 `d5b1062633a218914088fa1d5db70d03a74d7697ba300f755265675c32c5c8f7`. Publication `state=uploaded` vérifiée.
- Pas encore d'essai physique sur SM-A576B Android 16 : jauge noire/blanche à gauche, toucher des cases, contrôle cinq boutons/lecture, reprise au direct, UCI après navigation, accessibilité visuelle doivent être confirmés.

## 19/10/2026 — Préparation GD-008 (avant CI)
- Fab demande explicitement sons et synthèse de parole IA pour accessibilité basse vision. Analyse dépôt GeckoDoku : `PierrePiperSpeechEngine.kt`, `VoicePcmPlayer.kt`, `PierreVoiceConfig.kt`, workflows Gecko avec Sherpa 1.13.8 et Piper fr_FR-upmc-medium; sources officielles et SHA256 repris.
- Option retenue : vrai Piper VITS local dans APK, sid 0 Jessica, sid 1 Pierre, système Android TTS comme secours. Son synthétisé ToneGenerator pour les événements. Pas de copie aveugle de l'application Gecko.
- Tests Java attendus de `ChessNarrationTest.java`, pipeline Kotlin/Gradle, binaire Piper natif et modèle ONNX dans .apk, source/licence ; aucun résultat hardware affirmé à ce stade.
- Risques CI : conflit AAR natif, plugin Kotlin, archive voice et extraction. Risques fonctionnels : premières paroles coupées, double parole avec TalkBack, fortes latences CPU lors des longues descriptions, mode silencieux Android et chargement modèle.
