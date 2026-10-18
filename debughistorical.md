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
