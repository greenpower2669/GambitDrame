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
