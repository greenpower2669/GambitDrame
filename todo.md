# GambitDrame — tâches ouvertes, 19/10/2026

## Réalisé et vérifié sur branche `proposal/stockfish-multipv-human-training-v1`
- [x] Lecture FabCopilot + cinq mémoires + deux spécifications, audit Java/Gradle/workflow, identification des deux échecs initiaux.
- [x] Correction SDK : binaire sdkmanager par chemin absolu, suppression de la dépendance obsolète `android-actions/setup-android`.
- [x] `ChessGame.java` moteur Java sans Android pour coups légaux, règles spéciales, mat/pat, nulles simplifiées.
- [x] `MainActivity.java` plateau tactile, entrée UCI, annuler, historique, persistence locale, import et export FEN, lancement Gambit de dame.
- [x] Bot de démonstration aléatoire avec coups légaux, explicitement non calibré et distingué des profils humains réels.
- [x] Icône cavalier vectorielle + marges système Android 16.
- [x] 18 assertions Java dont perft(2)=400 et perft(3)=8902.
- [x] CI run 4 `success`, `BUILD SUCCESSFUL`, APK directe v0.2.1 sur https://github.com/greenpower2669/GambitDrame/releases/tag/gd-test-5 ; SHA256 dans debughistorical.
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

## Intervention v0.3.0 — corrections demandées par Fab
- [x] Ajouter algorithme de coups humains probabiliste distinct de Stockfish + Lichess ouverture (<1000 sans garantie de cohorte) + repli local + cible 400–900/style.
- [x] Ajouter pont Stockfish 18 UCI MultiPV3, WDL et centipions + source GPL3 et compilation NDK ARM64 (non vérifié jusqu'au CI).
- [x] Interface : sélecteurs réellement activés, analyse adaptée à l'orientation, logs de provenance humain, Internet optionnel.
- [x] Tests JVM de la politique et de la légalité intégrés au workflow.
- [x] CI #6 réussie : 18 tests de règles + 130 tirages de politique humains (e4=61 d4=39), Stockfish 18 ARM64 avec GPLv3 présent dans APK, Release directe `gd-test-6`, SHA256 consigné dans debughistorical.
- [ ] Installer sur SM-A576B Android 16 et confirmer qu'un vrai processus UCI Stockfish répond `uciok`, `readyok` et `info ... multipv ... wdl` ; vérifier chauffe et consommation.
- [ ] Cohortes Lichess vérifiées >= 1000 parties/joueur, niveaux 400 et 500, statistiques par cadence et cotes historiques ; niveaux cibles actuels NON calibrés.
- [ ] WDL sous forme de jauge graphique, vraie navigation MultiPV et analyse des coups précédents ; ce lot donne uniquement un panneau texte et 3 variantes.
- [ ] Vérification juridique de l'assemblage MIT+GPL/obligation de fournir sources et notes app.

## Statut réel du transfert 0.3.0
- CI et .apk **produites et vérifiées** par GitHub, sans ZIP pour Fab.
- À valider sur Samsung : ouverture de l'APK 79,8 Mo, première analyse Stockfish multiPV/WDL, téléchargement Lichess (si connecté), coups du bot et reprise.
- Les préréglages 400–900 demeurent expérimentaux, non étalonnés. Pas de fusion `main`.

## Mission GD-007 — jauge et télécommande de relecture
- [x] Coder colonne verticale noir haut/blanc bas de 44dp à gauche intégrée au canvas du plateau ; interpolation centipions/mat, indicateur accessible.
- [x] Coder boutons début/précédent/play-pause/suivant/fin, lecture auto ~900ms par coup, sortie relecture explicite, sans modifier l'historique sauvegardé.
- [x] Sécuriser bot contre navigation, intégrer affichage FEN historique et synchronisation d'analyse UCI avec tag FEN + barrière isready.
- [x] Ajouter tests JVM `ReviewTest` (curseur, évaluation) et vérifications dans le workflow.
- [x] Build 0.3.1 / workflow #7 réussi, 18+130+21 tests, Stockfish ARM64+GPL vérifiés dans l'APK et .apk directe `gd-test-7` publiée; lien et SHA256 dans debughistorical.
- [ ] Vérifier sur Samsung SM-A576B : jauge noire/blanche visible à gauche même en portrait, largeur, tactile case a..h, navigation, autoplay, reprise, et scores qui ne se mélangent pas en recul/avance rapides.

## Mission GD-008 accessibilité sonore et Piper
- [x] Traduction coups/positions en phrases françaises testables, annonce échec, prise, promotion, roque, case touchée, coup illégal.
- [x] Sons sélection/coup/prise/échec/erreur/relecture, switch son/voix indépendants et persistants.
- [x] Boutons Décrire la position/Lire l'échiquier/Coups possibles/Répéter/Écouter Stockfish, contrôle du débit et des profils voix Jessica/Pierre.
- [x] Code d'intégration Piper Sherpa VITS UPMC local (+ secours Android TTS) et CI prépare les modèles par SHA256.
- [x] CI #12 réussi et APK `0.3.2` directe publiée `gd-test-12`; ONNX Piper, Sherpa ARM64 et Stockfish dans APK, licences incluses, 15 tests de narration + autres tests réussis ; lien/empreinte dans debughistorical.
- [ ] Tester sur Samsung Android 16 sid 0 Jessica et sid 1 Pierre, audioTrack startup/warmup, latence, priorité audio, talkback et annonce de pions/gauche droite + mode voix désactivé.
- [ ] Améliorer l'accessibilité TalkBack des 64 cases (canvas actuel a une seule description globale) et gérer le contrôle de jeu par voix si validé plus tard.

- [ ] Urgent : vérifier sur Samsung SM-A576B Jessica sid0 / Pierre sid1, voix parlante par défaut, bascule ON/OFF, vitesse, boutons d'écoute, relecture, premier warmup muet.
- [ ] Mesurer espace mémoire de l'APK 176,6 Mo + assets extraits, vérifier TalkBack ne double pas la parole, CPU Piper/Stockfish ne chauffe pas excessivement.
- [ ] Retours utilisateur sur prononciation des colonnes A-H (étiquettes phonétiques) et éventuel ajout commandes vocales d'échecs, sans confusion sur les niveaux Elo 400–900.

## GD-009 livrable 0.4.0
- [x] Voix Piper warmup muet réel, file phrases non interruptives, délai de fin configurable et fallback Android seulement si échec Piper.
- [x] Stockfish full MultiPV et précision relative gaussienne cible 0..100 sigma 0..30 en mode de jeu distinct de Lichess; tests mathématiques.
- [x] Essais fictifs indépendants de partie, annuler/quitter, analyser chaque étape.
- [x] Flèches Canvas par glissé, toggle et effacement; tests des segments.
- [x] Pièces stylisées ivoire/ébène, minimal et haut contraste avec choix enregistré.
- [ ] Passer tests GitHub Actions et obtenir APK directe 0.4.0 avec Stockfish/Piper; ajouter preuve et hash en mémoire.
- [ ] Validation Samsung : début/fin véritables phrases, latence, moteur Stockfish recherche tous coups, mémoire, styles, flèches, cases et mode essais.
