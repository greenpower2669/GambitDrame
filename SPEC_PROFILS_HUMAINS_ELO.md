# Profils humains GambitDrame — complément au cahier des charges

Statut : proposition, sans modification du jeu ni fusion.

## Profils cibles
Six préréglages initiaux : 400, 500, 600, 700, 800 et 900 (classements Lichess par cadence, non assimilés à Elo FIDE). Paramétrage continu envisageable après calibration.

## Sélection de données
Joueurs actifs, classement non provisoire et relativement stable, au moins 1000 parties classées dans la cadence étudiée, puis jusqu'aux 100 dernières parties pertinentes par joueur. Contrôler le classement à la date de chaque partie, la taille de la cohorte et les conditions d'accès aux données. Les données sous 400 peuvent être rares : signaler toute insuffisance et ne pas inventer de calibration.

## Génération humaine
Ne jamais voter pour le meilleur coup ni moyenner des coups : tirer une décision parmi les coups humains observés avec probabilités calibrées par niveau et position. Échantillonner éventuellement un joueur/profil pour préserver un style cohérent sur une partie. Éviter les pertes gratuites artificielles, sans supprimer les vraies erreurs humaines de la tranche. Pour les positions inédites, prévoir un repli calibré à tester, et non une décision arbitraire de Stockfish.

## Réglages proposés
Niveau cible, cadence, largeur de tranche de classement, stabilité et activité des joueurs, taille de cohorte, fenêtre de parties, graine aléatoire, style agressif/positionnel, diversité, fidélité statistique, prise de risque, fréquence d'erreurs humaines, sélection de couleur, profondeur de l'analyse et mode hors ligne. Les réglages doivent afficher les limites de validité et permettre de revenir aux préréglages.

## Analyse indépendante
Stockfish affiche à gauche jauge blanc/noir, centipions et WDL séparés, variantes MultiPV, retour en arrière et analyse de coups. Les recherches parallèles restent optionnelles et soumises à un test de bénéfice mesuré.

## Validation
Mesurer sur parties complètes le classement effectif, distribution des pertes en centipions, taux de gaffes, diversité, cohérence, performance et écart à la population Lichess cible ; évaluer séparément chaque niveau, avec incertitudes. Ne pas confondre qualité de l'estimation statistique et force d'un vote majoritaire.

## Mise au point sourcée — 19 octobre 2026
- Lichess fixe actuellement un plancher de classement à **400** (`Glicko.minRating = IntRating(400)`). Le palier 500 est donc techniquement représentable et le palier 400 correspond à une valeur tronquée au plancher ; un joueur effectivement plus faible peut aussi apparaître à 400. Source officielle : https://github.com/lichess-org/lila/blob/master/modules/rating/src/main/Glicko.scala
- **Non établi** : nombre de comptes distincts actifs, non provisoires, possédant 1000 parties classées dans chaque cadence et situés de manière stable à 400 ou 500. Aucun échantillon probant téléchargé à ce stade.
- Les archives Lichess standard sont disponibles en CC0 et très volumineuses (~29,2 Go compressés pour septembre 2026). Éviter leur téléchargement total dans CI ou téléphone ; construire un pipeline hors ligne filtré, avec cache et contrôle d'échantillonnage. https://database.lichess.org/
