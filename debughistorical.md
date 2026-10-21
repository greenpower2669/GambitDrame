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

## 19/10/2026 — Résolution des échecs CI 0.3.2
- Run #8 `39301541395` : échec Gradle `Could not find method kotlinOptions() on project ':app'` à la ligne 19. Correction commit `fe3cb6f` : déplacer bloc kotlinOptions dans android.
- Run #9 `39302112548` et #10 `39302153935` : JVM cibles incompatibles Java 1.8 / Kotlin 17. Correction commit `b18ac1c` : `compileOptions` Java VERSION_17 des deux côtés.
- Run #11 `39302452741` : erreur syntaxe Kotlin PiperSpeechEngine.kt ligne 132 `Expecting an element`, parenthèse superflue dans builder AudioTrack. Correction commit `a3a6471`.
- Run #12 `39302856790` **succès** : `CHESS_TESTS_OK 18`, `HUMAN_POLICY_TESTS_OK 130`, `REVIEW_AND_GAUGE_TESTS_OK 21`, `CHESS_NARRATION_TESTS_OK 15`, `BUILD SUCCESSFUL in 1m 8s`.
- Contrôles archives SHA256 Sherpa AAR et Piper tar.bz2 **OK** ; APK contient `lib/arm64-v8a/libstockfish.so` 113 750 704 octets, `assets/tts/piper/pierre/fr_FR-upmc-medium.onnx` 76 682 529 octets, `SHERPA-ONNX-LICENSE.txt` 11 358 octets, source Piper. Avertissement Gradle non fatal : unable to strip libraries natives (inclut Stockfish/ONNX), présentes telles quelles.
- Pre-release `gd-test-12` : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-12/GambitDrame-v0.3.2-debug-arm64.apk ; taille 176 621 445 ; SHA256 `641ea821ecf57b696b5c3b1179a597745d4d8eb8543ea2c153dfd528cd306ff0`.
- Les tests CI prouvent compilation et présence ressources, **pas l'audition réelle des voix/hardware**, à qualifier ainsi dans passation.

## 20/10/2026 — Diagnostic et correctifs 0.4.0 à compiler
- Cause principale des coupures : ancienne méthode Piper.speak() incrémentait generation et appelait track.pause/flush dès une nouvelle annonce, même pendant une phrase. Ancien AudioTrack recréé à chaque phrase. Modifié en file d'attente sérialisée et lecture persistance de la piste, warmup muet avant prêt, délai après progression complète du pointeur audio.
- Stockfish humain : ancien adversaire expérimental Lichess a été jugé peu crédible. Nouveau choix Stockfish évalue exhaustivement N coups légaux MultiPV, normalise 0..100, tire cible gaussienne, refuse score manquant. À vérifier sur Android; aucun label Elo certain.
- Autres interventions : essais sur pile non sauvegardée, flèches vectorielles, pièces ivoire/ébène et deux thèmes alternatifs. Compilation/prêt téléphone NON ENCORE VALIDÉS.

## 20/10/2026 — Vérification build 0.4.0

## GD-009 — Livraison build v0.4.0 vérifiée 20/10/2026
- GitHub Actions run #13 : https://github.com/greenpower2669/GambitDrame/actions/runs/39385921496 => completed success, source 4069713b1d96aaaea77566650d88da8b8d22eebe.
- APK directe NON ZIP : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-13/GambitDrame-v0.4.0-debug-arm64.apk, taille 176 645 865 octets, SHA256 c9d35ce9d65a8004ed3c0224f44c5d55a20afaa3416bd472e2feeaa65932f838, release gd-test-13, asset uploaded.
- Tests logs : CHESS_TESTS_OK 18 ; HUMAN_POLICY_TESTS_OK 130 choices e4=61 d4=39 ; REVIEW_AND_GAUGE_TESTS_OK 21 ; CHESS_NARRATION_TESTS_OK 15 ; STOCKFISH_PRECISION_TESTS_OK 10 ; BOARD_ARROWS_TESTS_OK 4 ; BUILD SUCCESSFUL in 1m18s.
- Contenus natifs vérifiés ZIP APK : libstockfish.so ARM64 113 750 704 octets + GPL3, Piper fr_FR-upmc-medium.onnx 76 682 529 octets + Sherpa licences.
- ATTENTION : CI n'a pas testé l'exécution réelle sur Samsung, ni la sélection all-MultiPV à profondeur7 sur téléphone, ni audio Piper avec première phrase / enchaînement, ni glissés/couleurs en contexte réel. Ne pas prétendre « testé téléphone ».

- Les classes sont compilées sur Android et tests Java ont passé. Avertissement Gradle « Unable to strip libonnxruntime.so, libsherpa-onnx..., libstockfish.so » n'a pas empêché le packaging natif, tous binaires présents.

## 20/10/2026 — GD-010 v0.4.1, état CI intermédiaire
- Build #16 `39393967417`: tests JVM, compilation Stockfish ARM64, Gradle, vérification APK **tous réussis**, mais publication GitHub Release refusée HTTP 403 «Resource not accessible by integration». Pas de Release #16 confirmée.
- Build #18 `39394049308`: `success` complet jusqu'à l'étape «Publish direct test APK in GitHub pre-release», commit `2c0cfc82d4e225ce3c1db6a8cc259629a0759dcf`. Les mises à jour ultérieures d'essais d'orientation de score et flèches continuent sur des runs distincts.
- Dernier code opérationnel candidat commit `d8aac2e3d21d47b3ac35340f34cf9d9c94b76b63`, workflow #22 `39394368345` en cours à la première observation. NE PAS revendiquer une livraison finale sans résultat du run courant et présence de l'asset direct .apk.
- Tests sur matériel Samsung toujours non réalisés par CI. Le score d'un coup est l'évaluation après son application à depth=1 depuis l'autre trait, renversée; ce choix peut donner une force réelle bien inférieure à celle du mode approfondi.

## GD-010 — Validation CI finale, 20/10/2026
- GitHub Actions #22 (39394368345) sur d8aac2e3d21d47b3ac35340f34cf9d9c94b76b63 : completed success ; BUILD SUCCESSFUL in 58s, règles 18 tests, politique humaine 130, précision/orientation Stockfish 15, flèches 4.
- APK directe .apk (pas ZIP), v0.4.1 code 8, dans gd-test-22 : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-22/GambitDrame-v0.4.1-debug-arm64.apk ; taille 176659809 octets.
- Livrée pour test réel Samsung SM-A576B Android 16. CI ne vérifie pas la qualité de l'affichage 3D, les gestes physiques ni le temps de réponse de Stockfish sur ce téléphone. Pas de merge main.

## GD-011 — Audit 0.4.1 sur Samsung et instrumentation 0.4.2, 20/10/2026
- Fab signale : moteur Stockfish semble jouer 100 %, même après curseur de précision modifié et partie relancée ; comportement jugé incohérent. Capture utilisateur avec 20 demi-coups.
- Lecture code : `precisionTarget` est transmis au sélectionneur, mais seule la distance au pourcentage relatif le plus proche compte. Ex: coups relatifs 100,45,0 => cible 80 sélectionne 100. `normalize` en cas de CP tous égaux définit tous les coups à 100. Cela peut expliquer l'effet, sans prouver ce qui a eu lieu sur son appareil.
- Avant 0.4.2 : aucune liste des coups évalués ni trace du choix accessible à l'utilisateur, d'où impossibilité d'expliquer une partie précise a posteriori.
- Correctif d'observabilité : `EngineDiagnostics`, logs par score, paramètres et résultat, GET ERR copiable/partageable, dernier message UCI si profondeur1 manquante; CI à vérifier.

## GD-011 — Livrable v0.4.2 vérifié en CI, 20/10/2026
- GitHub Actions #27 `39415650189` sur commit `4b5b68dc23f4c6c2938ec77c4db4509b8ea5906c`: completed success, BUILD SUCCESSFUL (1m20s). Tests précision 18, règles 18, revue/jauge 21, narrations 15, flèches 4.
- Prérelease `gd-test-27` : APK directe `GambitDrame-v0.4.2-debug-arm64.apk` (176 673 417 octets), lien https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-27/GambitDrame-v0.4.2-debug-arm64.apk ; pas de ZIP à décompresser.
- GET ERR : journal Stockfish local copiable et partageable, scores CP et rangs de tous les candidats, curseurs/modes, score réellement sélectionné, cas `BEST_FORCED_BY_NEAREST_SCORE` et `ALL_SCORES_EQUAL`, traces de l'analyse gauche en profondeur 11 vs choix profondeur 1, erreurs moteur.
- Tests manuels Samsung Android 16 restent requis pour vérifier le moteur UCI et l'ergonomie réelle. Rien fusionné sur main.


## 20/10/2026 — GD-012 décision et vérifications à réaliser
- Incident témoin v0.4.2 : 6…Qf5 choisi avec score profondeur1 −343cp / qualité 55,45 % vs cible 75,06 % malgré 6…Qe7 −98cp; la jauge profondeur11 signalait +6,31 après coup. Une perte de dame peut aussi faire partie d'un sacrifice pertinent, à juger sur l'évaluation d'ensemble.
- Protocole v0.4.3 : garder le processus UCI de la jauge profondeur11 et demander MultiPV complet profondeur12 *après* bestmove depth11 ; moteur et hash persistants. Score du joueur noir signé correctement ; plafond de perte relative 60 cp à 80 %.
- Risques à tester : latence Stockfish MultiPV n lignes à depth12, lignes UCI manquantes, Android lifecycle/pause, barre prédictive après coup, checkbox avancé et annulation du calcul; aucune garantie de vérification matérielle par seule CI.
- Test Java TacticalGuard + code Android ; consulter Actions avant d'affirmer APK prête.

## GD-012 — Livraison CI confirmée
- GitHub Actions #37, https://github.com/greenpower2669/GambitDrame/actions/runs/39426144750 : success, commit `faa696ba9e86c52808087e8f3f04a640b4a36c59`.
- Tests confirmés dans le log : `TACTICAL_GUARD_TESTS_OK 8`, `STOCKFISH_PRECISION_TESTS_OK 18`, `CHESS_TESTS_OK 18`, `BOARD_ARROWS_TESTS_OK 4`; `BUILD SUCCESSFUL in 1m 19s`.
- Prérelease `gd-test-37`, APK **directe non ZIP** `GambitDrame-v0.4.3-debug-arm64.apk`, 176684097 octets : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-37/GambitDrame-v0.4.3-debug-arm64.apk.
- ⚠️ Validation Android 16 physique à faire : UCI pause/reprise, latence MultiPV12, score et bande prédictive, voix, journal GET ERR. CI ≠ preuve de performance réelle sur Samsung. Pas de fusion main.


## GD-013 — Journal de développement analyseur (20/10/2026)
- Mise en place moteur étude Stockfish local profondeur9 (N+1 positions, une seule session UCI), qualité par perte cp plafonnée, statistiques Blancs/Noirs, répertoire d'ouverture local, archive JSON, graphe historique et réussite par ouverture.
- Vérifications CI : StudyMetricsTest ajouté au workflow. Tentatives préliminaires #42/#43 échec compilation Java StudyTrendView.drawText(String,float,float) sans argument Paint ; correction apportée au commit 7dcb94c, à confirmer par run ultérieur. Pas de test Samsung physique.
- Limite métrique : évaluation séparée de deux positions à même profondeur peut subir bruit; valeurs expérimentales non assimilables à la précision Lichess. Import accepte JSON archive et valide séquence de coups.

- CI #48 (39431587270) au commit `71d07bcdd6109cc1b81cfbbc6f489461686f49cf` : SUCCESS, build Android et tests locaux, APK v0.4.4 176715029 octets, release gd-test-48. Les échecs intermédiaires #42/#43 sont corrigés. Pas de certification de fonctionnement réel Samsung.


## GD-015 — Diagnostic / risques et tests en cours (20/10/2026)
- Incident d'origine : sélection artificielle de pourcentage relatif parmi les coups, provoquant parfois dégradation massive et absence de stratégie. Le code v0.4.5 a un garde-fou CP mais conserve une sélection de coups isolés, sans plan durable. Ce constat vient de l'audit du code, et non d'un test exhaustif de parties.
- Modification candidate : softmax entre coups proches de la meilleure évaluation, plafond de perte CP, profondeur plus élevée quand jauge indique que Stockfish est en déficit, plan positionnel simple étiqueté et légèrement favorisé ; tous coups légaux peuvent entrer dans MultiPV. Nécessité d'une calibration de parties complètes ouverte.
- Voix : le code antérieur ne possédait pas de signal explicite de fin AudioTrack pour bloquer l'application du coup IA. `PiperSpeechEngine` fournira un callback après réellement joué, Android TTS via onDone ; un délai de secours 45 secondes évite le blocage en cas de pilote dysfonctionnel. Ne pas présenter ce délai comme une preuve de lecture terminée ; tester matériel.
- Visuel et assistances ajoutés au code, mais rendu/voix/explications exactes non certifiés sur SM-A576B par les seules Actions.
- CI code v0.4.6 en cours : premier run #67 échoué lors de compilation Java car appel selectGuarded ancien nombre d'arguments (version transitoire). Appel corrigé avant dernier build ; attendre CI réussie. Historiser ensuite SHA/run/tag direct APK.

## GD-015 — Résultat final de CI 20/10/2026
- GitHub Actions run #70 : https://github.com/greenpower2669/GambitDrame/actions/runs/39497491157 ; `success`, `BUILD SUCCESSFUL in 1m 17s`, code testé 294c4e0f60e6374cc12db7ca7c8c8b6f3557f0a4.
- Tests confirmés : ADAPTIVE_OPPONENT_TESTS_OK 504, OPENING_COACH_TESTS_OK 10, BOARD_ARROWS_TESTS_OK 46, CHESS_TESTS_OK 18, HUMAN_POLICY_TESTS_OK 130, STOCKFISH_PRECISION_TESTS_OK 18, TACTICAL_GUARD_TESTS_OK 8, STUDY_METRICS_TESTS_OK 9.
- APK ARM64 directe, sans ZIP : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-70/GambitDrame-v0.4.6-debug-arm64.apk ; taille 176749305 octets, MIME application/vnd.android.package-archive ; Release https://github.com/greenpower2669/GambitDrame/releases/tag/gd-test-70.
- Statut : livré à valider sur Samsung SM-A576B Android16. Pas de test physique, pas de mesure sur parties complètes, pas de calibration Elo ni de certification AudioTrack sur téléphone. Ancien mode conservé, main inchangé.


## GD-016 — Retour Samsung niveau 30/40 joue comme 90 (21/10/2026)
- Capture v0.4.6 : cible 40%, sigma10 ; dernier coup g8g7 classé 100% / perte 0/132cp, en mode adaptatif, jauge noire défavorable. Un seul meilleur coup n'est pas une preuve de force sur une partie complète.
- Audit code : `effectiveTarget` ajoutait jusqu'à 95% de l'écart vers 100 selon déficit de CP; exemple niveau30 / −300cp => 97, ce qui écrasait le niveau nominal; `softmax` et budget de pertes très serrés faisaient par ailleurs privilégier des coups quasi parfaits.
- Correctif GD-016 : cible positionnelle séparée, jamais de mutation de niveau, hystérésis jusqu'au seuil, température nominale plus étendue à bas niveau, contre-jeu modéré, CP plafonnés et logs séparés. La recherche n'assure pas de remonter la jauge et la précision 30/90 n'est pas Elo mesuré.
- Risques : profondeur MultiPV 12–14 selon retard, délai téléphone, choix de défense/sacrifice, curseur seuil si l'utilisateur change en pleine partie ; tests CI & Samsung à consigner sans inventer succès.

## GD-016 — Vérification CI finale et APK 0.4.7 (21/10/2026)
- GitHub Actions run #79 https://github.com/greenpower2669/GambitDrame/actions/runs/39669050938 : **completed success**, BUILD SUCCESSFUL (1m17s) ; commit code `525f333139f957be6b3e8c18b9bb9defd977f856`. Couverture : ADAPTIVE_OPPONENT_TESTS_OK **1113**, CHESS_TESTS_OK 18, STOCKFISH_PRECISION_TESTS_OK 18, TACTICAL_GUARD_TESTS_OK 8, BOARD_ARROWS_TESTS_OK 46, STUDY_METRICS_TESTS_OK 9, OPENING_COACH_TESTS_OK 10.
- Prérelease gd-test-79 https://github.com/greenpower2669/GambitDrame/releases/tag/gd-test-79 ; APK directe non ZIP, MIME application/vnd.android.package-archive, **176754465 octets** : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-79/GambitDrame-v0.4.7-debug-arm64.apk.
- Jauge turquoise signale l'objectif de récupération (score du côté Noir dans le mode actuel), sans modifier la valeur nominale 30/40/90. Test CI de logique oui ; **test de jeu réel Samsung non réalisé**. Conserver ancien mode et toutes les données ; main non fusionnée.
