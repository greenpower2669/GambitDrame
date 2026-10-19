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
