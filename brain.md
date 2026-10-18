# GambitDrame — état fonctionnel et contrat (19/10/2026)
Objectif : application d'échecs accessible Android jouable, à terme entraîneur contre des joueurs humains crédibles.
L'adversaire humain doit échantillonner des coups humains réels, avec continuité de style, et ne doit jamais être assimilé aux meilleurs coups de Stockfish. Les tranches sont des classements Lichess par cadence, non FIDE ; cohorte 400/500 non vérifiée.

## Code proposé sur branche de travail
- `ChessGame.java` : état FEN, coups légaux, attaque/échecs, roque, prise en passant, quatre promotions, rois protégés, mat/pat, détection simplifiée 50 coups et triple répétition au niveau activité, matériel insuffisant.
- `MainActivity.java` : plateau tactile, saisie UCI alternative, historique, annulation, reprise et sauvegarde locale, import/copie FEN, chargement de l'ouverture Gambit de dame, bot de démonstration aléatoire **NON calibré**.
- Profils Lichess 400–900 affichés mais désactivés ; Stockfish non intégré ; pas d'import PGN, ni navigation des variantes.
- Aucune mesure de force ni installation matérielle revendiquée. Ces fonctionnalités doivent passer les CI et essais Android avant statut opérationnel.

## Principes à conserver
- Accessibilité : fort contraste, plateau responsive, taille raisonnable, entrée de coups au clavier en alternative, icône chevalier vectorielle et marges système Android 16 ; accessibilité TalkBack virtuelle des 64 cases restant à développer.
- Hors ligne par défaut ; pas d'INTERNET dans le manifeste.
- Licence MIT du code original ; ne pas embarquer Stockfish avant clarification licence/source de distribution.
- Mémoires du projet à conserver distinctes des mémoires Djipie et FabCopilot.
