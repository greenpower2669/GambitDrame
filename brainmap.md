# GambitDrame — architecture et flux (19/10/2026)

## Entrées
- `settings.gradle`, `build.gradle`, `app/build.gradle` : Android Java AGP 8.7.3, compile SDK 35, applicationId fr.gambitdrame, version 0.2.0.
- `app/src/main/AndroidManifest.xml` : activité exportée, thème natif, aucune permission réseau.

## Fonctionnement actuel du prototype proposé
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
   - Champ UCI, nouveau, annuler, FEN en presse-papiers ; Bot simple aléatoire sur coups légaux (prototype explicitement non humain/non calibré).
   - Bouton Gambit de dame : charge 1.d4 d5 2.c4, mode entraînement extensible à construire.
   - Spinner 400–900 désactivé ; texte Stockfish absent = état véridique.
3. `app/src/test/java/fr/gambitdrame/ChessGameTest.java` : tests Java autonomes : jeu initial, mat, pat, roque, en passant, promotions, pièce clouée, FEN.
4. `.github/workflows/android-debug.yml` : checkout → Java 17 → SDK courant `sdkmanager` → Gradle 8.9 → tests `javac` → assembleDebug → vérification taille APK → GitHub Release préliminaire direct .apk, sans main.

## Extension planifiée, NON implémentée
- `human/` : pipelines de collecte Lichess, provenance, filtres actifs/stables, légalité, distributions probabilistes et politiques repli.
- `analysis/` : interface moteur UCI Stockfish séparé, 1 processus, WDL et centipions non confondus, MultiPV, cache, contraintes mémoire/CPU/licence.
- `training/` : registre d'ouvertures, exercices, variantes et scores.
- UI accessible TalkBack case par case, configuration avancée des profils, affichage gauche/tablette ou repli sous plateau en portrait.
