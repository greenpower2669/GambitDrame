# GambitDrame

Entraînement aux ouvertures d'échecs centré sur les idées, les branches et les coups théoriques.

## Intégration Lichess Opening Explorer

La branche `feature/lichess-opening-explorer` ajoute une couche d'enrichissement dynamique des positions à partir de l'Opening Explorer Lichess.

### Principe

GambitDrame garde sa pédagogie et ses branches écrites à la main. Lichess sert à enrichir chaque position avec :

- nombre de parties observées ;
- fréquence de chaque continuation ;
- résultats blancs / nulles / noirs ;
- Elo moyen du coup quand disponible ;
- parties récentes et parties de référence ;
- nom ECO/ouverture renvoyé par Lichess.

Les données Lichess ne remplacent jamais les explications pédagogiques. Elles donnent du poids statistique aux variantes et permettent de faire apparaître progressivement de nouvelles branches.

## Synchronisation complète d'un arbre d'étude

Le point d'entrée principal est `syncStudyTree()`.

```js
import {
  LichessStudyClient,
  syncStudyTree,
} from './src/lichess-study-sync.mjs';

const play = [
  'd2d4', 'd7d5',
  'c2c4', 'e7e6',
  'b1c3', 'g8f6',
  'c1g5', 'f8e7',
  'e2e3', 'e8g8',
  'g1f3', 'b8d7',
];

const client = new LichessStudyClient();

const tree = await syncStudyTree(play, {
  level: 'advanced',
  existingTree: previousTree,
  client,
});
```

Le résultat contient une table `positions` indexée par la suite UCI. Chaque position contient son snapshot Lichess, ses coups connus et la liste des coups réellement enseignés pour le niveau demandé.

Cette représentation évite de reconstruire un énorme arbre imbriqué à chaque mise à jour : seules les positions visitées sont rafraîchies.

## Niveaux d'apprentissage

`src/learning-policy.mjs` fournit trois profils adaptatifs :

- **Débutant** : très peu de continuations, uniquement les lignes fréquentes ;
- **Intermédiaire** : plusieurs alternatives courantes ;
- **Avancé** : branches fines jusqu'à environ 1 % de fréquence si le volume de parties est suffisant.

Un coup écrit à la main avec une explication pédagogique est toujours conservé, même s'il est rare dans la base Lichess.

Les profils peuvent être surchargés à l'appel de `syncStudyTree()` sans modifier la base pédagogique.

## Règle de comptage importante

Une réponse de l'Opening Explorer est déjà un agrégat complet de la position. Lors d'un nouveau rafraîchissement, GambitDrame **remplace le snapshot statistique précédent** au lieu d'additionner les valeurs.

Exemple : 12 000 parties puis 12 100 parties donnent 12 100, et non 24 100.

Les références de parties individuelles sont dédupliquées par `game.id`.

## Cache et respect de Lichess

`LichessStudyClient` :

- met les positions en cache ;
- fusionne les demandes simultanées vers la même position ;
- sérialise tous les appels réseau pour n'avoir qu'une requête Lichess à la fois ;
- déclenche une pause d'au moins une minute après une réponse HTTP `429` ;
- permet de forcer un rafraîchissement avec `force: true`.

L'arbre est également borné avec `maxDepth` et `maxPositions` pour empêcher un niveau Avancé de lancer accidentellement des milliers de requêtes.

## Proxy serveur

Le navigateur appelle par défaut :

```text
GET /api/lichess-explorer
```

Le proxy `server/lichess-explorer-proxy.mjs` ajoute l'authentification Lichess côté serveur.

Définir :

```text
LICHESS_TOKEN=...
```

Le jeton ne doit jamais être placé dans le JavaScript envoyé au navigateur.

Les appels même origine sont autorisés sans CORS. Si l'interface et l'API sont sur deux domaines différents, définir explicitement :

```text
GAMBITDRAME_ALLOWED_ORIGIN=https://app.example
```

Plusieurs origines peuvent être séparées par des virgules.

Le proxy limite aussi les paramètres transmis à Lichess, borne le nombre de coups/parties retournés et sérialise les appels en amont par instance serveur.

## Exemple position seule

Pour enrichir une seule branche sans développer l'arbre :

```js
import {
  fetchLichessPosition,
  mergeLichessIntoBranch,
} from './src/lichess-explorer.mjs';

const snapshot = await fetchLichessPosition(play);
const enrichedBranch = mergeLichessIntoBranch(currentBranch, snapshot);
```

## Tests

```bash
npm test
```

GitHub Actions exécute automatiquement la suite Node sur les branches `feature/**` et les pull requests vers `main`.
