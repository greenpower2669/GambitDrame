# GambitDrame

Prototype Android d'entraînement au **Gambit Dame**.

## Objectif V0.1

- jouer les Blancs sur un échiquier tactile ;
- rester dans un petit arbre théorique local et auditable ;
- faire répondre automatiquement les Noirs avec plusieurs branches ;
- afficher un pourcentage de proximité avec la théorie ;
- signaler immédiatement une sortie de ligne par un son discret ;
- expliquer le coup en gros texte et le lire à voix haute via le TTS Android ;
- conserver un premier historique local de réussite ;
- produire automatiquement un APK de test avec GitHub Actions.

## Lignes actuellement incluses

Le jeu de données V0.1 est volontairement petit :

- Gambit Dame refusé, ligne orthodoxe ;
- Gambit Dame refusé, variante d'échange ;
- Gambit Dame accepté ;
- Défense slave contre le Gambit Dame.

Les `trainingWeight` servent uniquement à varier les réponses pendant le prototype. Ils ne représentent **pas encore** les fréquences réelles de Lichess ou Chess.com.

## Architecture

Le moteur pédagogique (`domain`) est séparé de l'interface Compose (`ui`). Cette séparation permettra ensuite d'ajouter :

- une base théorique plus riche ;
- des imports PGN ;
- des statistiques externes mises en cache ;
- des profils de niveau ;
- un historique détaillé par branche et profondeur ;
- une passerelle vers ChatGPT pour approfondir un coup ;
- éventuellement Stockfish comme outil d'analyse complémentaire, sans remplacer la logique pédagogique.

## APK

Le workflow **Build Android APK** compile `app-debug.apk` et le publie comme artefact GitHub Actions.

Branche de développement initiale : `dev/v0.1-prototype`.
