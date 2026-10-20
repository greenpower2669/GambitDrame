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
- [x] CI #13 réussie et APK directe gd-test-13 publiée ; tests totaux 18+130+21+15+10+4, binaire Stockfish ARM64+Piper présents ; lien/hash consignés.
- [ ] Audit matériel SM-A576B indispensable : Stockfish MultiPV=N (N coups légaux) peut coûter temps et RAM; jouer des parties complètes, mesurer délais. Vérifier queue Piper et fin de phrases par lecture réelle. Tester essai + annotations sur l'échiquier.
- [ ] Validation Samsung : début/fin véritables phrases, latence, moteur Stockfish recherche tous coups, mémoire, styles, flèches, cases et mode essais.

## GD-010 — Essai v0.4.1 demandé par Fab le 20/10/2026
- [x] Nouveau sélectionneur Stockfish rapide : appliquer chaque coup légal sur copie, évaluer FEN fille UCI à profondeur 1, inverser le signe vers joueur initial, classer les scores 0–100, tirer selon cible sigma.
- [x] Paramètres par défaut à la migration v0.4.1 : Stockfish Noir, cible 80 %, sigma 5 ; mode MultiPV profond optionnel et profondeur réglable, profil Lichess distinct préservé.
- [x] Styles bois : silhouettes demi-pièces en coupe longitudinale au sol et mode relief/perspective, anciens thèmes préservés.
- [x] Flèches semi-transparentes, aperçu durant glisser, désactivation interception ScrollView + échiquier plein écran immersif.
- [x] Tests supplémentaires pure-Java pour la conversion du score enfant et classement meilleur/pire.
- [x] Vérifier CI #22 success et existence APK v0.4.1 directe dans gd-test-22.
- [ ] Essai visuel et tactile du vrai rendu bois/perspective et flèches plein écran sur Samsung SM-A576B Android 16 par Fab.
- [ ] Tester sorties terminales (mat/pat), promotion, coups égaux en CP, analyse sur des positions avec beaucoup de coups, latence, mémoire et absence d'interruption de la voix sur appareil.
- [ ] Réduire consommation si engine Stockfish d'analyse et sélectionneur simultanément posent problème; ne pas cacher les erreurs.
- [ ] Vérifier les obligations GPLv3 de distribution APK; ne pas fusionner main sans aval explicite.

## GD-010 — Validation CI finale, 20/10/2026
- GitHub Actions #22 (39394368345) sur d8aac2e3d21d47b3ac35340f34cf9d9c94b76b63 : completed success ; BUILD SUCCESSFUL in 58s, règles 18 tests, politique humaine 130, précision/orientation Stockfish 15, flèches 4.
- APK directe .apk (pas ZIP), v0.4.1 code 8, dans gd-test-22 : https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-22/GambitDrame-v0.4.1-debug-arm64.apk ; taille 176659809 octets.
- Livrée pour test réel Samsung SM-A576B Android 16. CI ne vérifie pas la qualité de l'affichage 3D, les gestes physiques ni le temps de réponse de Stockfish sur ce téléphone. Pas de merge main.

## GD-011 — 20/10/2026 : Stockfish 0.4.1 peut jouer à 100 % malgré curseur 80 %
- [x] Audit source : curseur sauvegardé et transmis. Ratio 0/100 relatif par position ; l'algorithme du plus proche peut sélectionner 100 même avec cible 80, notamment grands écarts ou scores tous égaux.
- [x] Ajouter diagnostic automatique par coup : mode, profondeur, valeurs cible/sigma (y compris sauvegardées), FEN, CP pour tous coups, pourcentages de classement, valeur gaussienne tirée, résultat réellement joué, raisons ALL_SCORES_EQUAL / BEST_FORCED_BY_NEAREST_SCORE.
- [x] Ajouter GET ERR près de l'échiquier : journal local persistant, lecture, copier tout, partage explicite ; stacktrace en cas d'erreur, erreurs UCI manquantes détaillées.
- [x] Tests de régression prouvant qu'une cible 80 % peut aboutir à un coup classé 100 % ; ne pas modifier le gameplay avant collecte de logs.
- [x] CI v0.4.2 code9 et asset .apk direct vérifiés (run 27). 
- [ ] Tester physiquement sur Samsung Android16 le bouton GET ERR, la journalisation de plusieurs coups et copier le résultat.
- [ ] Faire plusieurs coups noirs avec cible 0 %, 30 %, 80 % et 100 %, copier GET ERR complet et analyser les écarts CP / valeurs relatives / causes, temps de réponse et FEN.
- [ ] Après preuve, revoir mapping cible-vers-force (rang, CP, distribution, seuil de pertes positionnelles), sans prétendre Elo ou précision officielle.

## GD-011 — Livrable v0.4.2 vérifié en CI, 20/10/2026
- GitHub Actions #27 `39415650189` sur commit `4b5b68dc23f4c6c2938ec77c4db4509b8ea5906c`: completed success, BUILD SUCCESSFUL (1m20s). Tests précision 18, règles 18, revue/jauge 21, narrations 15, flèches 4.
- Prérelease `gd-test-27` : APK directe `GambitDrame-v0.4.2-debug-arm64.apk` (176 673 417 octets), lien https://github.com/greenpower2669/GambitDrame/releases/download/gd-test-27/GambitDrame-v0.4.2-debug-arm64.apk ; pas de ZIP à décompresser.
- GET ERR : journal Stockfish local copiable et partageable, scores CP et rangs de tous les candidats, curseurs/modes, score réellement sélectionné, cas `BEST_FORCED_BY_NEAREST_SCORE` et `ALL_SCORES_EQUAL`, traces de l'analyse gauche en profondeur 11 vs choix profondeur 1, erreurs moteur.
- Tests manuels Samsung Android 16 restent requis pour vérifier le moteur UCI et l'ergonomie réelle. Rien fusionné sur main.


## GD-012 — Garde-fou tactique / jauges juxtaposées, 20/10/2026
- [x] Réutiliser le processus UCI de Stockfish de la jauge : profondeur11 terminée puis recherche de tous les coups au niveau12, table hash conservée.
- [x] Implémenter limite de pertes centipions selon précision + regression pure Java sur exemple dame offerte, conserver les sacrifices évalués correctement.
- [x] Case cochable Garde-fou tactique persistée, activée initialement dans le mode rapide, désactivable ; garder mode MultiPV approfondi séparé et mode1 historique.
- [x] Superposition fine dans jauge gauche (prédiction score choisi et meilleur possible), texte explicatif, journal GET ERR pour tous les scores/override.
- [ ] Confirmer succès des tests JVM et compilation Android sur dernier commit / récupérer APK directe officielle GitHub.
- [ ] Faire tester sur Samsung SM-A576B Android16 : temps de réponse à 20–40 coups légaux MultiPV12, réponse vs force, animation jauges et bouton checkbox, légalité coups et absence de mémoire excessive, GET ERR et reprise après pause.
- [ ] Si délais >tolérance réelle ou scores insuffisants, optimiser le plan profond (budgets, cache, recherche ciblée avec preuve), sans fausse prétention de réutilisation d'un simple CP.
