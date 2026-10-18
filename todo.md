# GambitDrame — tâches ouvertes, 19/10/2026

## Réalisé et vérifié sur branche `proposal/stockfish-multipv-human-training-v1`
- [x] Lecture FabCopilot + cinq mémoires + deux spécifications, audit Java/Gradle/workflow, identification des deux échecs initiaux.
- [x] Correction SDK : binaire sdkmanager par chemin absolu, suppression de la dépendance obsolète `android-actions/setup-android`.
- [x] `ChessGame.java` moteur Java sans Android pour coups légaux, règles spéciales, mat/pat, nulles simplifiées.
- [x] `MainActivity.java` plateau tactile, entrée UCI, annuler, historique, persistence locale, import et export FEN, lancement Gambit de dame.
- [x] Bot de démonstration aléatoire avec coups légaux, explicitement non calibré et distingué des profils humains réels.
- [x] Icône cavalier vectorielle + marges système Android 16.
- [x] 18 assertions Java dont perft(2)=400 et perft(3)=8902.
- [x] CI run 4 `success`, `BUILD SUCCESSFUL`, APK directe v0.2.1 sur https://github.com/greenpower2669/GambitDrame/releases/tag/gd-test-4 ; SHA256 dans debughistorical.
- [x] Cohérence du registre et des quatre mémoires mise à jour dans le même cycle de commits.
- [ ] Test d'installation/lancement sur Samsung SM-A576B Android 16 : dépend du test réel de Fab (non revendiqué).

## Missions utilisateur encore ouvertes — ne pas effacer GD-001 à GD-005
- [ ] GD-001 : audit approfondi des règles FIDE (répétitions, compte 75 coups, bugs positions spécifiques), perft Kiwipete, annulation et sauvegarde sur appareil, PGN et navigation dans l'historique, accessibilité virtuelle TalkBack de chaque case.
- [ ] GD-002 : Stockfish UCI à part de l'adversaire, jauge CP, WDL distinctes, MultiPV, analyse des coups et reproductibilité CPU/mémoire/température, licences GPL de distribution.
- [ ] GD-003 : obtenir et filtrer cohortes Lichess par cadence, classement non provisoire stable, >=1000 parties/joueur, jusqu'à 100 dernières parties, validation cotes historiques, couverture des niveaux 400/500 (plancher officiel 400 confirmé, volume des cohortes non quantifié).
- [ ] GD-003 : implémenter distributions de coups humains (tirage hiérarchique joueur/coup), continuité de style, politique de repli non-Stockfish et tests de parties complètes avec incertitude.
- [ ] GD-004 : menus d'ouvertures extensibles, variantes, répétitions espacées, exercices, explication d'erreurs.
- [ ] GD-005 : retours d'essai physique, build AAB seulement si publication Play demandée, contrôles accessibilité et confidentialité.

## Décisions et limites de la recherche
- Archives officielles Lichess standard (CC0) : lot de septembre 2026 ~29,2 Go compressés, volume inadapté à une ingestion aveugle dans l'APK ou par GitHub Actions. Préférer corpus prétraité hors ligne, cohorte limitée et cache local. Source : https://database.lichess.org/
- Code Lichess officiel : le classement minimum est **400** (`Glicko.minRating`), avec plafond 4000 ; au plancher, les différences de niveau inférieur sont censurées. Cela ne prouve pas l'existence de cohortes éligibles avec 1000 parties. Source : https://github.com/lichess-org/lila/blob/master/modules/rating/src/main/Glicko.scala
- Export utilisateur API possible en principe, avec filtres cadence et nombre maximal de parties ; les 400/500 à 1000 parties classées n'ont PAS été quantifiés. Docs et retours : https://lichess.org/api#operation/apiGamesUser
- Stockfish en GPLv3 : licence et code source correspondant obligatoires en cas de distribution du binaire. Ne pas embarquer sans vérifier le modèle de liaison/déploiement. Source : https://github.com/official-stockfish/Stockfish
- Aucun profil Lichess, aucun Stockfish et aucun classement 400–900 opérationnels aujourd'hui.

## Suite de travail
Tester la v0.2.1 sur Samsung, puis construire le corpus humain mesuré et le moteur d'échantillonnage ; le champ « niveaux » reste désactivé tant que leur force n'est pas prouvée. Ne pas merger `main`.

## CI
Les commits purement Markdown n'ont pas besoin de recompiler Android ; workflow désormais configuré pour ignorer les changements `*.md` seuls.
