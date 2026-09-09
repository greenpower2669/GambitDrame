# GambitDrame

Entraînement aux ouvertures d'échecs centré sur les idées, les branches et les coups théoriques.

## Intégration Lichess Opening Explorer

La branche `feature/lichess-opening-explorer` ajoute une première couche d'enrichissement dynamique des positions à partir de l'Opening Explorer Lichess.

### Principe

GambitDrame garde sa pédagogie et ses branches écrites à la main. Lichess sert uniquement à enrichir chaque position avec :

- nombre de parties observées ;
- fréquence de chaque continuation ;
- résultats blancs / nulles / noirs ;
- Elo moyen du coup quand disponible ;
- parties récentes et parties de référence ;
- nom ECO/ouverture renvoyé par Lichess.

Les données Lichess ne remplacent pas les explications pédagogiques. Elles donnent du poids statistique aux variantes et permettent de faire apparaître progressivement de nouvelles branches.

### Exemple : Gambit Dame refusé, orthodoxe

```js
import {
  fetchLichessPosition,
  mergeLichessIntoBranch,
  learningCandidates,
} from './src/lichess-explorer.mjs';

const play = [
  'd2d4', 'd7d5',
  'c2c4', 'e7e6',
  'b1c3', 'g8f6',
  'c1g5', 'f8e7',
  'e2e3', 'e8g8',
  'g1f3', 'b8d7',
];

const snapshot = await fetchLichessPosition(play);
const enrichedBranch = mergeLichessIntoBranch(currentBranch, snapshot);
const proposedMoves = learningCandidates(enrichedBranch);
```

Le client appelle par défaut `/api/lichess-explorer`. Le proxy serveur ajoute l'authentification Lichess sans exposer le jeton au navigateur.

### Configuration serveur

Définir une variable d'environnement :

```text
LICHESS_TOKEN=...
```

Puis brancher `server/lichess-explorer-proxy.mjs` sur la route HTTP `GET /api/lichess-explorer` de l'hébergement utilisé par l'application.

Le jeton ne doit jamais être placé dans le JavaScript envoyé au navigateur.

### Règle de comptage importante

Une réponse de l'Opening Explorer est déjà un agrégat complet de la position. Lors d'un nouveau rafraîchissement, GambitDrame **remplace le snapshot statistique précédent** au lieu d'additionner les valeurs, sinon le compteur de parties serait doublé à chaque synchronisation.

Les références de parties individuelles sont dédupliquées par `game.id`.

### Niveaux proposés

La première version classe automatiquement les continuations Lichess :

- `main` : fréquence >= 12 % et au moins 100 parties ;
- `candidate` : fréquence >= 3 % et au moins 30 parties ;
- `rare` : le reste.

Ces seuils sont des valeurs de départ et peuvent être adaptés aux niveaux Débutant / Intermédiaire / Avancé.

### Respect des limites API

Le proxy ne lance qu'une requête par appel. En cas de réponse HTTP `429`, GambitDrame doit respecter `Retry-After` et éviter les rafraîchissements simultanés. Un cache court côté serveur limite les appels inutiles.
