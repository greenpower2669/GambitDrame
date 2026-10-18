# GambitDrame — suivi technique, 19/10/2026

## Produits dans la proposition de code de cette intervention (à valider par CI)
- [x] Audit des cinq mémoires, du code initial, du workflow et du premier échec SDK (run 39173917141).
- [x] Nouveau moteur Java de règles complètes de base ; interfaces tactiles, clavier, sauvegarde, reprise, annulation.
- [x] Tests reproductibles Java inclus dans CI.
- [x] Correction candidate du workflow `sdkmanager tools` absent ; direct APK dans pré-release sur branche.
- [x] Gambit de dame : position d'ouverture chargeable.
- [ ] Contrôler troisième CI après correctif du chemin SDK (run 2 : échec `sdkmanager: command not found`) ; vérifier tests `javac` puis APK Release. Aucune APK pour l'instant.

## À développer, distinct des ordres Fab GD-001 à GD-005
- [ ] Tests avancés de légalité : perft (profondeurs 1–4), nulles FIDE, historique/PGN, import/export FEN ; tests Android instrumentation et TalkBack.
- [ ] Séparer ViewModel et écran, registres d'ouvertures, variantes, leçons/erreurs.
- [ ] Pipeline Lichess respectant activité, non-provisoire, 1000 parties/cadence, 100 dernières parties, classement historique ; vérifier volume 400 et 500 (aucun profil réel créé).
- [ ] Modèle humain empirique, échantillonnage hiérarchique, repli calibré, mesures parties complètes et incertitude.
- [ ] Stockfish UCI pour analyse uniquement (GPLv3, distribution des sources), jauge, WDL, MultiPV, paramètres de reproduction et bench CPU.
- [ ] Vérifier accessibilité et confort sur Samsung SM-A576B Android 16, icône dédiée, nom de version, APK testé.
- [ ] Ne pas merger main ni présenter profils Elo ou Stockfish comme implémentés.

## Prochaine vérification
Consulter le workflow déclenché par ce commit. S'il échoue, inspecter logs par étape et corriger l'anomalie observée selon FAB-DEBUG-001. S'il réussit, vérifier GitHub Release + asset `.apk` direct avant transmission.

## Extension v0.2.1 (19/10/2026)
- [x] Ajout de l'import FEN (base locale persistante), des marges système Android 16 et d'une icône vectorielle.
- [x] Décompte de positions perft initial profondeur 2 et 3 ajouté aux tests Java.
- [ ] Vérifier le build 0.2.1, la pré-release et l'APK direct ; installation, navigation et rendu des glyphes sur le Samsung restent à valider.
