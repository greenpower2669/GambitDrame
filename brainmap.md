# GambitDrame — architecture et flux (19/10/2026)

## Entrées
- `settings.gradle`, `build.gradle`, `app/build.gradle` : Android Java AGP 8.7.3, compile SDK 35, applicationId fr.gambitdrame, version 0.2.1.
- `app/src/main/AndroidManifest.xml` : activité exportée, thème natif, aucune permission réseau.

## Fonctionnement actuel du prototype compilé par CI
1. `app/src/main/java/fr/gambitdrame/ChessGame.java`
   - `Position` : plateau de 64 chars, trait, droits de roque, en passant, compteurs ; parser/formatter FEN et clef répétition.
   - `pseudo` : génération géométrique, conditions spéciales roque/pion/promotion.
   - `attacked`, `inCheck`, `legal` : filtrage par exposition du roi ; interdit de capturer le roi.
   - `apply`, `play` : mutations sur copies de Position ; roques, en passant, promotion et droits.
   - `outcome`, `insufficient` : mat, pat, règles simplifiées des nulles. Limite : règles avancées FIDE et claims automatiques non implémentés.
2. `app/src/main/java/fr/gambitdrame/MainActivity.java`
   - `positions` / `moves` : états historisés par demi-coup, annulation et rejouabilité.
   - `SharedPreferences` : chemin UCI rejoué depuis START au lancement ; pas de réseau.
   - `ChessView` : Canvas responsive, tap pièce→destination, destinations légales ; promotion par dialog.
   - Champ UCI, nouveau, annuler, import de base FEN + historique et FEN en presse-papiers ; Bot simple aléatoire sur coups légaux (prototype explicitement non humain/non calibré).
   - Bouton Gambit de dame : charge 1.d4 d5 2.c4, mode entraînement extensible à construire.
   - Spinner 400–900 désactivé ; texte Stockfish absent = état véridique.
3. `app/src/test/java/fr/gambitdrame/ChessGameTest.java` : tests Java autonomes : perft(2)=400 et perft(3)=8902, jeu initial, mat, pat, roque, en passant, promotions, pièce clouée, FEN.
4. `.github/workflows/android-debug.yml` : checkout → Java 17 → localisation du `sdkmanager` dans ANDROID_HOME → Gradle 8.9 → tests `javac` → assembleDebug → vérification taille APK → GitHub Release préliminaire direct .apk, sans main.

## Extension planifiée, NON implémentée
- `human/` : pipelines de collecte Lichess, provenance, filtres actifs/stables, légalité, distributions probabilistes et politiques repli.
- `analysis/` : interface moteur UCI Stockfish séparé, 1 processus, WDL et centipions non confondus, MultiPV, cache, contraintes mémoire/CPU/licence.
- `training/` : registre d'ouvertures, exercices, variantes et scores.
- UI accessible TalkBack case par case, configuration avancée des profils, affichage gauche/tablette ou repli sous plateau en portrait.

## Extension architecture v0.3.0
- `HumanPolicy.java` : échantillonnage de coups légal via occurrences issues des parties réelles de la catégorie Lichess <1000 ; sinon distribution softmax de critères échiquéens, cible et style, graine locale. Pas de Stockfish.
- `LichessOpeningExplorer.java` : accès HTTPS optionnel explorer.lichess.ovh, ratings=0, cadences blitz/rapid, timeout 1800 ms, cache SharedPreferences, seulement les 24 premiers demi-coups ; n'affirme pas cohorte 1000 parties.
- `StockfishBridge.java` : ProcessBuilder sur `applicationInfo.nativeLibraryDir/libstockfish.so` ; protocole UCI, UCI_ShowWDL, MultiPV 3, parsing info score cp/mate/wdl et lignes principales, stop sur pause, shutdown. Analyse indépendante.
- `MainActivity.java` : choix niveau 400–900, style, switch Internet, exécuteur mono-thread pour appels Lichess, annulation robuste par révision, panneau Stockfish avant échiquier en portrait et à gauche en paysage large.
- CI `.github/workflows/android-debug.yml` : compilation tests JVM et human, NDK r27c, clone source officiel `sf_18` commit figé, `make net`/`make build ARCH=armv8 COMP=ndk`, inclusion comme lib exécutable native ARM64 + GPL/source sous assets, vérification zip APK puis Release directe.
- `app/build.gradle` 0.3.0, minSdk 29 (SDK natif Stockfish cible API 29), abiFilters arm64-v8a, useLegacyPackaging pour extraction native.
- Risques en cours : ProcessBuilder sur libstockfish.so sur Samsung 16, taille APK, exactitude orientation WDL par blanc et rétroaction UI, disponibilité réelle de l'API, licences.

## CI 0.3.0 — état vérifié
- Exécution #6 a passé : tests JVM, téléchargement NDK, compilateur C++ Stockfish, Gradle Android, `unzip -l` de l'APK, livraison GitHub Release. Source binaire compilé de 113,75 Mo et APK comprimée de 79,8 Mo.
- Message Gradle `Unable to strip ... libstockfish.so` : avertissement non bloquant ; moteur natif déjà passé par llvm-strip avant Gradle, packagé tel quel. L'exécution via ProcessBuilder dans nativeLibraryDir demeure à valider sur Android 16.
- GitHub Release de test `gd-test-6`, pas main. Voir debughistorical.md pour preuve et SHA256.

## Code ajouté au lot 0.3.1
- `EvaluationScale.java` (sans Android) : score centipions/mat du point de vue Blancs → fraction [0.02,0.98] et libellé localisé français ; 0cp=0.5.
- `ReviewCursor.java` (sans Android) : sélection 0..N, index=-1 pour direct, bornes de navigation, adaptation à une histoire tronquée sans modifier l'historique.
- `ReviewTest.java` vérifie bornes de relecture, retour live, scores positifs/négatifs/mats, formatage FR ; ajouté à CI `javac` et `java` après HumanPolicyTest.
- `MainActivity.ChessView` : jauge fait partie du canvas à gauche de l'échiquier, donc même hauteur et interaction de toucher décalée pour garder les coordonnées de cases précises. `displayed()` retourne la position consultée, `current()` reste la partie réelle.
- `StockfishBridge.Listener` : `onAnalysis(fen,text,cpWhite,mateWhite)`; UCI séquentiel stop-isready-go ; n'écrit pas de score dans la politique humaine.
- APK CI version 0.3.1 (code 5) attendue via prérelease nouvelle, build workflow inchangé pour moteur officiel Stockfish 18 et GPL.

## Validation chaîne d'intégration v0.3.1
- Tests Java : 18 légalité, 130 échantillonnages humains, 21 calculs/propriétés de curseur de relecture, tous réussis par workflow #7.
- Build Android passe, Stockfish 18 ARM64/notice GPL vérifiés dans le zip de l'APK, prérelease gd-test-7 directe publiée; détails de SHA256 et lien dans brain.md.
- Conserver la distinction : vérification CI = réussite du packaging ; validation Samsung/TalkBack/animation/jauge en situation = à faire.

## 0.3.2 composants son et voix
- `app/src/main/java/fr/gambitdrame/ChessNarration.java` : texte français pur testable par Java sans Android (positions, coups légaux, évaluation et événements).
- `AccessibleAudio.java` : contrôle paramètres + TTS Android hors ligne secours + ToneGenerator; délègue la synthèse principale à `PiperSpeechEngine.kt`.
- `PiperSpeechEngine.kt` : worker arrière-plan pour load + warmup + inference Piper + AudioTrack ; copie `espeak-ng-data` des assets vers fichiers internes; vitesse et speaker sid ; aucune dépendance réseau runtime.
- Workflow `android-debug.yml` : en plus Stockfish ARM64, prépare Sherpa AAR fixe et modèle VITS fr_FR-upmc-medium d'archive SHA256 fixe (`e9830a...67f89a7f`), sources/licences embarquées, vérifie .onnx dans .apk.
- `app/build.gradle` : version 0.3.2 code 6, plugin Kotlin 2.0.21 (nécessaire à l'interface Sherpa issue GeckoDoku), dépendance AAR locale téléchargée par CI.
- `MainActivity` : sons ponctuels, cinq boutons relecture conservés, aide parlante case/jeu/relecture/évaluation, sélecteurs directs et vitesse, mode silence contrôlable.

## Preuves CI 0.3.2
- Workflow #12 terminé `success` : https://github.com/greenpower2669/GambitDrame/actions/runs/39302856790.
- Gradle Java/Kotlin jvmTarget 17 harmonisés, Kotlin builder corrigé ; voie intégration modèle + AAR + paquet native ARM64 réussie ; `BUILD SUCCESSFUL` et asset uploadé.
- APK prérelease `gd-test-12`, taille 176 621 445 octets, SHA dans brain.md. Relever que le pack native libs reste extrait sur Samsung en install, avec espace libre supplémentaire à prévoir.

## Architecture 0.4.0
- StockfishPrecision.java + StockfishPrecisionTest.java : normalisation relative 0/100 et variance, comportement pure Java.
- StockfishOpponent.java : ProcessBuilder moteur Stockfish natif déjà distribué; un second processus pour évaluer tous coups légaux selon MultiPV; indépendante de l'analyse graphique StockfishBridge; mémoire Android à surveiller.
- BoardArrows.java + BoardArrowsTest.java : annotations transitoires flèches max12, Canvas Path et toucher ACTION_DOWN/UP converti sur grille avec jauge gauche.
- MainActivity : trialPositions/trialMoves sont distincts positions/moves; displayed() consulte pile d'essais si activée; 3 palettes de pièces sont rendues au Canvas avec fond de silhouette contrasté et contours.
- PiperSpeechEngine : générateur Sherpa ONNX et AudioTrack persistant en un worker, une phrase complète à la fois, mise en attente des suivantes. Délai 100..2000 ms réglable.
- Workflow CI : nouveaux tests StockfishPrecisionTest et BoardArrowsTest avant NDK/Gradle, publication APK 0.4.0 après succès.

## État livraison 0.4.0 : GitHub Actions #13 success

## GD-009 — Livraison build v0.4.0 vérifiée 20/10/2026
- GitHub Actions run #13 : https://github.com/greenpower2669/GambitDrame/actions/runs/39385921496 => completed success, source 4069713b1d96aaaea77566650d88da8b8d22eebe.
- APK directe NON ZIP : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-13/GambitDrame-v0.4.0-debug-arm64.apk, taille 176 645 865 octets, SHA256 c9d35ce9d65a8004ed3c0224f44c5d55a20afaa3416bd472e2feeaa65932f838, release gd-test-13, asset uploaded.
- Tests logs : CHESS_TESTS_OK 18 ; HUMAN_POLICY_TESTS_OK 130 choices e4=61 d4=39 ; REVIEW_AND_GAUGE_TESTS_OK 21 ; CHESS_NARRATION_TESTS_OK 15 ; STOCKFISH_PRECISION_TESTS_OK 10 ; BOARD_ARROWS_TESTS_OK 4 ; BUILD SUCCESSFUL in 1m18s.
- Contenus natifs vérifiés ZIP APK : libstockfish.so ARM64 113 750 704 octets + GPL3, Piper fr_FR-upmc-medium.onnx 76 682 529 octets + Sherpa licences.
- ATTENTION : CI n'a pas testé l'exécution réelle sur Samsung, ni la sélection all-MultiPV à profondeur7 sur téléphone, ni audio Piper avec première phrase / enchaînement, ni glissés/couleurs en contexte réel. Ne pas prétendre « testé téléphone ».

## GD-010 — Extension v0.4.1 (20/10/2026)
- `StockfishOpponent.select(...,deep,depth)` : défaut depth-1 *par coup légal* avec moteur UCI réutilisé, conversion en score du joueur initial (`StockfishPrecision.parentScoreFromChild`); mode MultiPV profond conservé et réglable.
- `MainActivity` : migration préréglages Stockfish 80/5, bois demi-pièces (style 3) / relief perspective (style 4), Switch MultiPV profond, dialogue de plateau plein écran, flèches sans interception de ScrollView et aperçu semi-transparent.
- `WoodPieces.java` : silhouettes Canvas en bois contrasté, sans cercle de fond; anciennes palettes accessibles.
- `app/build.gradle` : 0.4.1 code 8 ; workflow publie APK directe non ZIP sur prérelease.
- CI v0.4.1 run #18 réussi (version avant tests de conversion et prévisualisation). Attendre statut dernier run #22 avant validation finale. Pas de test Samsung par CI.

## GD-010 — Validation CI finale, 20/10/2026
- GitHub Actions #22 (39394368345) sur d8aac2e3d21d47b3ac35340f34cf9d9c94b76b63 : completed success ; BUILD SUCCESSFUL in 58s, règles 18 tests, politique humaine 130, précision/orientation Stockfish 15, flèches 4.
- APK directe .apk (pas ZIP), v0.4.1 code 8, dans gd-test-22 : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-22/GambitDrame-v0.4.1-debug-arm64.apk ; taille 176659809 octets.
- Livrée pour test réel Samsung SM-A576B Android 16. CI ne vérifie pas la qualité de l'affichage 3D, les gestes physiques ni le temps de réponse de Stockfish sur ce téléphone. Pas de merge main.

## GD-011 — Diagnostic de précision et GET ERR (20/10/2026)
- Nouveau `EngineDiagnostics.java`: journal embarqué local persistant sous SharedPreferences `gambit-engine-diagnostics` (65 k caractères récents), tag Logcat `GambitEngine`.
- `StockfishOpponent.java`: log des scores CP de toutes les FEN enfants et classement relatif de tous les coups (même en MultiPV), cible gaussienne, coup choisi, raisons valeur 100 %, durée et erreurs UCI.
- `MainActivity.java`: affichage des valeurs du curseur appliquées, GET ERR en haut (voir / COPIER TOUT / PARTAGER), réglages/choix de coups/erreurs/positions consignés.
- `StockfishPrecisionTest.java`: régression 'cible 80 % peut choisir 100 %' et égalités 100 %. Version 0.4.2 code9; CI finale et validation appareil requises.

## GD-011 — Livrable v0.4.2 vérifié en CI, 20/10/2026
- GitHub Actions #27 `39415650189` sur commit `4b5b68dc23f4c6c2938ec77c4db4509b8ea5906c`: completed success, BUILD SUCCESSFUL (1m20s). Tests précision 18, règles 18, revue/jauge 21, narrations 15, flèches 4.
- Prérelease `gd-test-27` : APK directe `GambitDrame-v0.4.2-debug-arm64.apk` (176 673 417 octets), lien https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-27/GambitDrame-v0.4.2-debug-arm64.apk ; pas de ZIP à décompresser.
- GET ERR : journal Stockfish local copiable et partageable, scores CP et rangs de tous les candidats, curseurs/modes, score réellement sélectionné, cas `BEST_FORCED_BY_NEAREST_SCORE` et `ALL_SCORES_EQUAL`, traces de l'analyse gauche en profondeur 11 vs choix profondeur 1, erreurs moteur.
- Tests manuels Samsung Android 16 restent requis pour vérifier le moteur UCI et l'ergonomie réelle. Rien fusionné sur main.


## GD-012 — Architecture garde-fou (20/10/2026)
- `StockfishBridge.java` : processus UCI unique, 3 phases idle / jauge depth11 / sélection gardée depth12 MultiPV=nombre de coups légaux; même hash partagé, synchronisation FEN, annulation et timeout; publication résultat via latch au worker bot.
- `TacticalGuard.java` + `TacticalGuardTest.java`: pure Java, perte CP maximum quadratique selon précision, interdiction relative des sacrifices catastrophiques pour 80%, tolérance élevée bas réglage; cas test de la dame.
- `StockfishOpponent.Result` : données score blanc prévu choisi/meilleur, perte CP, seuil et intervention; moteur profondeur1/mode approfondi originaux toujours disponibles.
- `MainActivity.java` : checkbox persistée, affichage double jauge dans Canvas à gauche, diagnostic GET ERR, moteur bot choisit pont partagé si garde activée et mode profond désactivé.
- `app/build.gradle` 0.4.3 code 10, `.github/workflows/android-debug.yml` teste TacticalGuard en Java et produit APK direct non zip. CI et test matériel à vérifier.

## GD-012 — Livraison CI confirmée
- GitHub Actions #37, https://github.com/greenpower2669/GambitDrame/actions/runs/39426144750 : success, commit `faa696ba9e86c52808087e8f3f04a640b4a36c59`.
- Tests confirmés dans le log : `TACTICAL_GUARD_TESTS_OK 8`, `STOCKFISH_PRECISION_TESTS_OK 18`, `CHESS_TESTS_OK 18`, `BOARD_ARROWS_TESTS_OK 4`; `BUILD SUCCESSFUL in 1m 19s`.
- Prérelease `gd-test-37`, APK **directe non ZIP** `GambitDrame-v0.4.3-debug-arm64.apk`, 176684097 octets : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-37/GambitDrame-v0.4.3-debug-arm64.apk.
- ⚠️ Validation Android 16 physique à faire : UCI pause/reprise, latence MultiPV12, score et bande prédictive, voix, journal GET ERR. CI ≠ preuve de performance réelle sur Samsung. Pas de fusion main.


## GD-013 — Entrée rapide (20/10/2026)
- Études de partie : `GameStudyEngine.java` (analyse offline prof9), `StudyMetrics.java` (pertes centipions et sigma descriptive), `OpeningTheory.java` (petit livre non exhaustif), `StudyTrendView.java` (graphique), UI/journal `MainActivity.java`.
- Contrat : ne pas confondre précision indicative, sigma réel, sigma du curseur, conformité théorique et taux de réussite ; détail dans `brain.md` / `todo.md`.
- Livraison v0.4.4 et validation Android16 à confirmer ; ne pas fusionner main.

- GD-013 livrée en prérelease `gd-test-48` APK 0.4.4 ; validation Samsung et calibration métriques toujours à faire. Détails `brain.md` / `todo.md`.


## GD-015 — Carte des dépendances candidates v0.4.6
- `PiperSpeechEngine.kt` : `speakTracked` pousse une `Utterance(message, Consumer<Boolean>)` en file série; fin réelle `play()` quand AudioTrack playbackHeadPosition a dépassé `playedFrames`; callback false si échec/stop. Sécurité warm-up et conservation des messages.
- `AccessibleAudio.java` : `sayTracked`, séquence de cancellation, secours TTS avec `UtteranceProgressListener` et ids de requête ; simple `say` inchangé.
- `MainActivity.java` : préf `gd015Adaptive` et sliders sensibilité/récupération/stabilité/profondeur ; routine `botMove` calcule en worker, ne joue qu'après `releaseSpeechGate` ; historiques `highlightedSquares` repris en relecture et peints dans `ChessView.onDraw`; aide `showMoveAdvice` explicite. Réduction de la narration automatique.
- `AdaptiveOpponent.java` : modèle Java pur par position, avantage racine `gaugeRootCp`, température de choix à perte CP plafonnée, profondeur adaptative, mémoire du plan; `AdaptiveOpponentTest.java` couvre équilibre et perte grave, non calibration Elo.
- `StockfishBridge.java` : état UCI/Hash partagé, mémorisation root cp et `AdaptiveOpponent.State`, paramètres d'adaptation, profondeur 12/13/14 bornée, diagnostic ADAPTIVE et recommandations MultiPV par FEN via `suggestions()`.
- `OpeningTheory.java` : `next(played,position)` donne uniquement les suites légales réellement connues du petit livre, avec étiquette d'ouverture; `OpeningCoach.java` produit descriptions positionnelles prudentes; `OpeningCoachTest.java` vérifie Gambit de dame accepté/refusé et provenance.
- `.github/workflows/android-debug.yml` : compile tests Java AdaptiveOpponentTest et OpeningCoachTest puis Gradle Android avec Piper/Stockfish; prérelease APK directe v0.4.6 quand CI passe.

## GD-015 — Résultat final de CI 20/10/2026
- GitHub Actions run #70 : https://github.com/greenpower2669/GambitDrame/actions/runs/39497491157 ; `success`, `BUILD SUCCESSFUL in 1m 17s`, code testé 294c4e0f60e6374cc12db7ca7c8c8b6f3557f0a4.
- Tests confirmés : ADAPTIVE_OPPONENT_TESTS_OK 504, OPENING_COACH_TESTS_OK 10, BOARD_ARROWS_TESTS_OK 46, CHESS_TESTS_OK 18, HUMAN_POLICY_TESTS_OK 130, STOCKFISH_PRECISION_TESTS_OK 18, TACTICAL_GUARD_TESTS_OK 8, STUDY_METRICS_TESTS_OK 9.
- APK ARM64 directe, sans ZIP : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-70/GambitDrame-v0.4.6-debug-arm64.apk ; taille 176749305 octets, MIME application/vnd.android.package-archive ; Release https://github.com/greenpower2669/GambitDrame/releases/tag/gd-test-70.
- Statut : livré à valider sur Samsung SM-A576B Android16. Pas de test physique, pas de mesure sur parties complètes, pas de calibration Elo ni de certification AudioTrack sur téléphone. Ancien mode conservé, main inchangé.


## GD-016 — Flux récup niveau / seuil (21/10/2026)
- `MainActivity.java` lit/écrit `gd016RecoveryGoalCp` via curseur [-500..0] par pas 25 cp, étiquette du niveau nominal stable. Passe nom 30 / sigma 10, seuil -100 indépendamment à `StockfishBridge.selectGuarded(..., recoveryGoalCp)`.
- `StockfishBridge.GuardRequest.recoveryGoalCp` transporte le but positionnel; `gaugeRootCp` prend score UCI vu du camp Stockfish à la profondeur11. Il détermine si la récupération vise la barre, et la profondeur MultiPV 12 (normal) / 13 ou 14 si déficit; log `GUARD_SEARCH`, `ADAPTIVE` séparé; `gd016Recovering`, `gd016RecoveryGoalLast` maintiennent la progression à la reprise.
- `AdaptiveOpponent.State.recovering` et `.recoveryGoalCp` encapsulent la cible, hystérésis d'entrée et sortie seulement à `rootCp>=goalCp`. `AdaptiveOpponent.choose(...,rootCp,goalCp)` garde nominal, ajuste la température et le plafond de perte de manière bornée, donne bonus modeste au progrès vers la barre, maintient un léger bonus de continuité du plan. `AdaptiveOpponentTest` vérifie objectifs, distribution 30 vs 90 et absence de hausse nominale.
- `StockfishOpponent.Result` fournit une description adaptée sans confondre qualité relative du coup (100%) et difficulté demandée; ancien constructeur conservé pour comparaison.
- `app/build.gradle` v0.4.7 code14 ; GitHub Actions prérelease APK directe. Résultats CI à confirmer, Android16 non testé physiquement.

## GD-016 — Vérification CI finale et APK 0.4.7 (21/10/2026)
- GitHub Actions run #79 https://github.com/greenpower2669/GambitDrame/actions/runs/39669050938 : **completed success**, BUILD SUCCESSFUL (1m17s) ; commit code `525f333139f957be6b3e8c18b9bb9defd977f856`. Couverture : ADAPTIVE_OPPONENT_TESTS_OK **1113**, CHESS_TESTS_OK 18, STOCKFISH_PRECISION_TESTS_OK 18, TACTICAL_GUARD_TESTS_OK 8, BOARD_ARROWS_TESTS_OK 46, STUDY_METRICS_TESTS_OK 9, OPENING_COACH_TESTS_OK 10.
- Prérelease gd-test-79 https://github.com/greenpower2669/GambitDrame/releases/tag/gd-test-79 ; APK directe non ZIP, MIME application/vnd.android.package-archive, **176754465 octets** : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-79/GambitDrame-v0.4.7-debug-arm64.apk.
- Jauge turquoise signale l'objectif de récupération (score du côté Noir dans le mode actuel), sans modifier la valeur nominale 30/40/90. Test CI de logique oui ; **test de jeu réel Samsung non réalisé**. Conserver ancien mode et toutes les données ; main non fusionnée.


## GD-017 — Dépendances et index (21/10/2026)
- `MainActivity.java` : `gaugeTensorMode`, `gaugeRedCp`, `gaugeGreenCp`, `gaugeLegend`, `ChessView.drawGauge` et `ChessView.onTouchEvent` (3 voies verticales N/R/V, clamp de la position inverse EvaluationScale, manipulation du doigt, affichage mise à jour temps réel) ; anciens réglages déplacés vers `advancedControls` GONE puis bouton déroulant, sans les supprimer. Stockfish sélectionne maintenant `selectGuarded(..., tensorMode, redCp, greenCp)` par défaut.
- `GaugeTensorPolicy.java`: fonctions `tension(rootCp,grey,redCp,greenCp)`, `choose(...)`, rang basé sur tri CP descendant (pire 0, meilleur 100), interpolation smoothstep (seuils CP indépendants de la liste), bruit sigma, plafond CP selon la tension, suivi du plan / variantes PV, explication log et alternatives.
- `StockfishBridge.java`: `GuardRequest` possède tenseurs et PV complètes des lignes UCI profondeur finale ; choisit `GaugeTensorPolicy` si nouveau mode, sinon garde l'ancien `AdaptiveOpponent` ou `TacticalGuard`; mémorise plan/continuation, journal TENSOR_DECISION / PLAN_CHANGE / PLAN_KEEP.
- `GaugeTensorPolicyTest.java`: validations de 50% au milieu du classement, progression rouge/vert, 30 contre 90, contrôle des pertes CP, maintien de mémoire stratégique et explications journalisées. Workflow Android compile la classe + tests et publie APK directe prérelease v0.4.8 (code 15).

## GD-017 — Livraison vérifiée GitHub Actions (21/10/2026)
- CI **#88** https://github.com/greenpower2669/GambitDrame/actions/runs/39681424964 : completed success, BUILD SUCCESSFUL 1m22s. Commit code `aa1a6e403657aa88ed70f9bb367fcfea0a4526c2`.
- Tests CI explicitement confirmés : `GAUGE_TENSOR_TESTS_OK 17` (50% = milieu du classement, rouge/vert progressive, maintien de plan/filtre), `ADAPTIVE_OPPONENT_TESTS_OK 1113`, `CHESS_TESTS_OK 18`, `STOCKFISH_PRECISION_TESTS_OK 18`, `BOARD_ARROWS_TESTS_OK 46`.
- Pré-release `gd-test-88` https://github.com/greenpower2669/GambitDrame/releases/tag/gd-test-88 ; APK directe `GambitDrame-v0.4.8-debug-arm64.apk`, 176771129 octets, MIME application/vnd.android.package-archive (sans ZIP) https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-88/GambitDrame-v0.4.8-debug-arm64.apk.
- Statut : **livré pour tests sur Samsung SM-A576B Android16**. Compilation et tests Java ne prouvent PAS qualité visuelle, tactile, variation d'Elo ou absence de retard MultiPV en usage réel. Branch proposal préservée, pas de merge main.
