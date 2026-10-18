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
