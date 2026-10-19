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
