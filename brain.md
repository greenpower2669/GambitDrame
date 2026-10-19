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
