# GambitDrame

Application Android d'entraînement au **Gambit Dame**, pensée pour apprendre les idées et les variantes plutôt que simplement jouer contre un moteur.

## État actuel — V0.2

- échiquier tactile, joueur côté Blancs ;
- réponses noires automatiques et pondérées ;
- curseur de complexité **Moyen / Avancé / Complexe** ;
- l'arbre de variantes s'élargit avec la complexité choisie ;
- pourcentage de proximité avec la théorie ;
- signal sonore immédiat quand le coup sort de l'arbre travaillé ;
- explications en gros texte et lecture vocale Android ;
- historique local de réussite ;
- bouton **Approfondir dans ChatGPT** sans clé API : l'application prépare et copie automatiquement le contexte de la position, puis ouvre ChatGPT ;
- génération automatique d'un APK de test avec GitHub Actions.

## Variantes locales actuellement incluses

### Moyen

- Gambit Dame refusé — ligne orthodoxe ;
- Gambit Dame refusé — variante d'échange ;
- Gambit Dame accepté ;
- Défense slave.

### Avancé

Le niveau Avancé ajoute notamment :

- Semi-Slave / structure méran ;
- Défense Tarrasch ;
- Défense Chigorin.

### Complexe

Le niveau Complexe ajoute notamment :

- contre-gambit Albin ;
- Cambridge Springs ;
- Ragozin.

Les `trainingWeight` servent pour l'instant uniquement à varier les réponses des Noirs. Ils ne sont **pas présentés comme des fréquences réelles**. Une prochaine couche pourra les alimenter depuis plusieurs sources statistiques mises en cache.

## Philosophie du moteur pédagogique

Le but n'est pas de dire « cette position gagne à +0,7 » mais de répondre à une autre question : **est-ce que le joueur apprend réellement son ouverture et sait reconnaître les idées quand l'adversaire change de variante ?**

Le moteur pédagogique (`domain`) est séparé de l'interface Compose (`ui`) afin de pouvoir ensuite ajouter sans réécrire l'application :

- davantage de branches théoriques ;
- imports PGN ;
- plusieurs sources statistiques ;
- suivi de progression par branche et profondeur ;
- entraînement ciblé sur les points faibles ;
- profils de joueurs publics ;
- Stockfish comme outil d'analyse complémentaire, sans remplacer l'arbre pédagogique.

## ChatGPT sans coût API obligatoire

Le bouton ChatGPT ne consomme pas d'API OpenAI : il fabrique un contexte contenant le niveau, la branche, les coups joués, le dernier retour pédagogique et les coups théoriques disponibles. Ce texte est placé dans le presse-papiers puis ChatGPT est ouvert. L'utilisateur n'a plus qu'à coller le contexte pour continuer la discussion avec son compte habituel.

## APK

Le workflow **Build Android APK** compile `app-debug.apk` et le publie comme artefact GitHub Actions.

Branche de développement actuelle : `dev/v0.1-prototype`.
