package com.gambitdrame.app.domain

import kotlin.random.Random

class QueenGambitTheory {

    /*
     * Curated local repertoire for training. trainingWeight is deliberately
     * NOT labelled as a real-world frequency yet. Later it can be refreshed
     * from several datasets without changing the training engine.
     */
    private val lines = listOf(
        TrainingLine(
            name = "Gambit Dame refusé · orthodoxe",
            trainingWeight = 34,
            minComplexity = TrainingComplexity.MEDIUM,
            moves = listOf(
                m("d2d4", "d4", "Tu prends de l'espace au centre et tu prépares c4."),
                m("d7d5", "…d5", "Les Noirs répondent au centre."),
                m("c2c4", "c4", "Le coup caractéristique du Gambit Dame : tu mets d5 sous pression."),
                m("e7e6", "…e6", "Le gambit est refusé. Les Noirs soutiennent d5."),
                m("b1c3", "Cc3", "Développement et pression supplémentaire sur d5."),
                m("g8f6", "…Cf6", "Les Noirs développent en contrôlant e4."),
                m("c1g5", "Fg5", "Le fou cloue le cavalier et augmente la pression positionnelle."),
                m("f8e7", "…Fe7", "Les Noirs préparent le roque et cassent le clou."),
                m("e2e3", "e3", "Tu consolides le centre et libères ton fou f1."),
                m("e8g8", "…O-O", "Le roi noir se met à l'abri."),
                m("g1f3", "Cf3", "Développement naturel et préparation du roque."),
                m("b8d7", "…Cbd7", "Les Noirs terminent leur développement central."),
                m("a1c1", "Tc1", "La tour vient soutenir la colonne c."),
                m("c7c6", "…c6", "Les Noirs solidifient d5."),
                m("f1d3", "Fd3", "Le fou se développe vers une diagonale active.")
            )
        ),
        TrainingLine(
            name = "Gambit Dame refusé · variante d'échange",
            trainingWeight = 24,
            minComplexity = TrainingComplexity.MEDIUM,
            moves = listOf(
                m("d2d4", "d4", "Tu prends le centre."),
                m("d7d5", "…d5", "Les Noirs occupent aussi le centre."),
                m("c2c4", "c4", "Tu mets d5 sous pression."),
                m("e7e6", "…e6", "Le Gambit Dame est refusé."),
                m("b1c3", "Cc3", "Tu développes et renforces la pression."),
                m("g8f6", "…Cf6", "Développement naturel des Noirs."),
                m("c4d5", "cxd5", "Tu échanges la tension centrale."),
                m("e6d5", "…exd5", "Une structure typique du Gambit Dame apparaît."),
                m("c1g5", "Fg5", "Développement avec pression sur f6."),
                m("f8e7", "…Fe7", "Les Noirs préparent le roque."),
                m("e2e3", "e3", "Tu consolides et libères ton fou."),
                m("e8g8", "…O-O", "Le roi noir se met à l'abri."),
                m("f1d3", "Fd3", "Développement actif vers h7."),
                m("b8d7", "…Cbd7", "Les Noirs continuent leur développement."),
                m("d1c2", "Dc2", "La dame soutient l'attaque potentielle sur h7.")
            )
        ),
        TrainingLine(
            name = "Gambit Dame accepté",
            trainingWeight = 20,
            minComplexity = TrainingComplexity.MEDIUM,
            moves = listOf(
                m("d2d4", "d4", "Tu prends de l'espace au centre."),
                m("d7d5", "…d5", "Les Noirs occupent le centre."),
                m("c2c4", "c4", "Tu offres temporairement le pion c."),
                m("d5c4", "…dxc4", "Les Noirs acceptent le gambit."),
                m("g1f3", "Cf3", "Tu développes sans te précipiter pour récupérer le pion."),
                m("g8f6", "…Cf6", "Les Noirs développent."),
                m("e2e3", "e3", "Tu prépares Fxc4 et consolides d4."),
                m("e7e6", "…e6", "Les Noirs consolident leur position."),
                m("f1c4", "Fxc4", "Tu récupères le pion en développant une pièce."),
                m("c7c5", "…c5", "Les Noirs attaquent immédiatement le centre."),
                m("e1g1", "O-O", "Tu mets ton roi à l'abri."),
                m("a7a6", "…a6", "Les Noirs préparent souvent ...b5."),
                m("d1e2", "De2", "La dame soutient e4 et la pression centrale.")
            )
        ),
        TrainingLine(
            name = "Défense slave",
            trainingWeight = 18,
            minComplexity = TrainingComplexity.MEDIUM,
            moves = listOf(
                m("d2d4", "d4", "Tu prends le centre."),
                m("d7d5", "…d5", "Les Noirs répondent au centre."),
                m("c2c4", "c4", "Position de Gambit Dame."),
                m("c7c6", "…c6", "La Slave soutient d5 sans enfermer le fou c8."),
                m("g1f3", "Cf3", "Développement souple."),
                m("g8f6", "…Cf6", "Les Noirs développent et renforcent le centre."),
                m("b1c3", "Cc3", "Tu augmentes la pression centrale."),
                m("d5c4", "…dxc4", "Les Noirs prennent c4 après avoir consolidé."),
                m("a2a4", "a4", "Tu limites ...b5 et facilites la récupération du pion."),
                m("c8f5", "…Ff5", "Le fou sort avant que ...e6 ne ferme sa diagonale."),
                m("e2e3", "e3", "Tu prépares Fxc4."),
                m("e7e6", "…e6", "Les Noirs consolident."),
                m("f1c4", "Fxc4", "Tu récupères le pion avec développement.")
            )
        ),
        TrainingLine(
            name = "Semi-Slave · structure méran",
            trainingWeight = 14,
            minComplexity = TrainingComplexity.ADVANCED,
            moves = listOf(
                m("d2d4", "d4", "Tu prends le centre."),
                m("d7d5", "…d5", "Les Noirs répondent au centre."),
                m("c2c4", "c4", "Tu mets d5 sous pression."),
                m("e7e6", "…e6", "Le Gambit Dame est refusé."),
                m("b1c3", "Cc3", "Développement et pression sur d5."),
                m("g8f6", "…Cf6", "Les Noirs développent."),
                m("g1f3", "Cf3", "Tu développes sans définir encore ton fou c1."),
                m("c7c6", "…c6", "La structure semi-slave est installée."),
                m("e2e3", "e3", "Tu consolides le centre."),
                m("b8d7", "…Cbd7", "Les Noirs préparent ...dxc4 et ...b5."),
                m("f1d3", "Fd3", "Le fou vise h7 et soutient e4."),
                m("d5c4", "…dxc4", "Les Noirs prennent c4 au bon moment."),
                m("f1c4", "Fxc4", "Tu récupères le pion en développant.")
            )
        ),
        TrainingLine(
            name = "Gambit Dame refusé · défense Tarrasch",
            trainingWeight = 12,
            minComplexity = TrainingComplexity.ADVANCED,
            moves = listOf(
                m("d2d4", "d4", "Tu prends le centre."),
                m("d7d5", "…d5", "Les Noirs répondent au centre."),
                m("c2c4", "c4", "Tu mets d5 sous pression."),
                m("e7e6", "…e6", "Le gambit est refusé."),
                m("b1c3", "Cc3", "Développement naturel."),
                m("c7c5", "…c5", "La Tarrasch attaque immédiatement d4 et accepte parfois un pion isolé."),
                m("c4d5", "cxd5", "Tu clarifies la tension centrale."),
                m("e6d5", "…exd5", "Les Noirs reprennent et peuvent obtenir un pion dame isolé."),
                m("g1f3", "Cf3", "Tu développes en attaquant le centre."),
                m("b8c6", "…Cc6", "Les Noirs mettent de la pression sur d4."),
                m("g2g3", "g3", "Tu prépares le fianchetto du fou."),
                m("g8f6", "…Cf6", "Les Noirs poursuivent leur développement."),
                m("f1g2", "Fg2", "Le fou vise le centre et l'aile dame.")
            )
        ),
        TrainingLine(
            name = "Défense Chigorin",
            trainingWeight = 10,
            minComplexity = TrainingComplexity.ADVANCED,
            moves = listOf(
                m("d2d4", "d4", "Tu prends le centre."),
                m("d7d5", "…d5", "Les Noirs répondent au centre."),
                m("c2c4", "c4", "Tu mets d5 sous pression."),
                m("b8c6", "…Cc6", "La Chigorin développe une pièce devant le pion c et cherche une activité immédiate."),
                m("g1f3", "Cf3", "Tu développes et contrôles e5."),
                m("c8g4", "…Fg4", "Les Noirs mettent une pression directe sur f3 et d4."),
                m("c4d5", "cxd5", "Tu clarifies le centre."),
                m("g4f3", "…Fxf3", "Les Noirs abîment potentiellement ta structure de pions."),
                m("g2f3", "gxf3", "Tu reprends le fou et acceptes une structure atypique."),
                m("d8d5", "…Dxd5", "La dame noire récupère le pion central."),
                m("e2e3", "e3", "Tu consolides et poursuis le développement.")
            )
        ),
        TrainingLine(
            name = "Contre-gambit Albin",
            trainingWeight = 8,
            minComplexity = TrainingComplexity.COMPLEX,
            moves = listOf(
                m("d2d4", "d4", "Tu prends le centre."),
                m("d7d5", "…d5", "Les Noirs répondent au centre."),
                m("c2c4", "c4", "Tu entres dans le Gambit Dame."),
                m("e7e5", "…e5", "L'Albin contre-gambit sacrifie immédiatement un pion pour l'initiative."),
                m("d4e5", "dxe5", "Tu acceptes le pion et obliges les Noirs à justifier leur initiative."),
                m("d5d4", "…d4", "Le pion avancé gêne fortement le développement blanc."),
                m("g1f3", "Cf3", "Tu attaques d4 et développes."),
                m("b8c6", "…Cc6", "Les Noirs augmentent la pression sur e5 et d4."),
                m("a2a3", "a3", "Tu prépares souvent b4 et évites certaines idées de ...Fb4+."),
                m("c8e6", "…Fe6", "Les Noirs développent avec tempo sur le centre."),
                m("b1d2", "Cbd2", "Tu développes en attaquant d4.")
            )
        ),
        TrainingLine(
            name = "Gambit Dame refusé · Cambridge Springs",
            trainingWeight = 9,
            minComplexity = TrainingComplexity.COMPLEX,
            moves = listOf(
                m("d2d4", "d4", "Tu prends le centre."),
                m("d7d5", "…d5", "Les Noirs répondent au centre."),
                m("c2c4", "c4", "Tu mets d5 sous pression."),
                m("e7e6", "…e6", "Le gambit est refusé."),
                m("b1c3", "Cc3", "Développement naturel."),
                m("g8f6", "…Cf6", "Les Noirs développent."),
                m("c1g5", "Fg5", "Le fou cloue le cavalier."),
                m("b8d7", "…Cbd7", "Les Noirs préparent une structure très théorique."),
                m("e2e3", "e3", "Tu consolides."),
                m("c7c6", "…c6", "Les Noirs soutiennent d5."),
                m("g1f3", "Cf3", "Tu continues ton développement."),
                m("d8a5", "…Da5", "Le coup caractéristique de Cambridge Springs met une pression tactique sur c3 et a2."),
                m("b1d2", "Cd2", "Tu réduis plusieurs idées tactiques de la dame noire.")
            )
        ),
        TrainingLine(
            name = "Gambit Dame refusé · Ragozin",
            trainingWeight = 9,
            minComplexity = TrainingComplexity.COMPLEX,
            moves = listOf(
                m("d2d4", "d4", "Tu prends le centre."),
                m("d7d5", "…d5", "Les Noirs répondent au centre."),
                m("c2c4", "c4", "Tu mets d5 sous pression."),
                m("e7e6", "…e6", "Le gambit est refusé."),
                m("b1c3", "Cc3", "Développement naturel."),
                m("g8f6", "…Cf6", "Les Noirs développent."),
                m("g1f3", "Cf3", "Tu développes sans jouer Fg5 immédiatement."),
                m("f8b4", "…Fb4", "La Ragozin cloue le cavalier c3 et augmente la pression centrale."),
                m("c4d5", "cxd5", "Tu clarifies la tension centrale."),
                m("e6d5", "…exd5", "Les Noirs reprennent avec le pion e."),
                m("c1g5", "Fg5", "Tu développes et mets f6 sous pression."),
                m("h7h6", "…h6", "Les Noirs interrogent ton fou."),
                m("g5h4", "Fh4", "Tu conserves le clou."),
                m("g7g5", "…g5", "Les Noirs gagnent de l'espace mais prennent des risques autour du roi."),
                m("h4g3", "Fg3", "Le fou reste actif sur la diagonale.")
            )
        )
    )

    fun candidates(
        history: List<String>,
        complexity: TrainingComplexity
    ): List<TheoryCandidate> {
        val matchingLines = matchingLines(history, complexity)
        if (matchingLines.isEmpty()) return emptyList()

        return matchingLines
            .filter { history.size < it.moves.size }
            .groupBy { it.moves[history.size].uci }
            .map { (_, group) ->
                TheoryCandidate(
                    move = group.first().moves[history.size],
                    weight = group.sumOf { it.trainingWeight }
                )
            }
            .sortedByDescending { it.weight }
    }

    fun findCandidate(
        history: List<String>,
        uci: String,
        complexity: TrainingComplexity
    ): TheoryCandidate? = candidates(history, complexity).firstOrNull { it.move.uci == uci }

    fun chooseReply(
        history: List<String>,
        complexity: TrainingComplexity
    ): TheoryCandidate? {
        val options = candidates(history, complexity)
        if (options.isEmpty()) return null
        if (options.size == 1) return options.first()

        val total = options.sumOf { it.weight.coerceAtLeast(1) }
        var draw = Random.nextInt(total)
        for (candidate in options) {
            draw -= candidate.weight.coerceAtLeast(1)
            if (draw < 0) return candidate
        }
        return options.last()
    }

    fun branchLabel(history: List<String>, complexity: TrainingComplexity): String {
        val names = matchingLines(history, complexity).map { it.name }.distinct()
        return when {
            names.isEmpty() -> "Hors arbre local"
            names.size == 1 -> names.first()
            else -> "Tronc commun · ${names.size} variantes possibles"
        }
    }

    fun expectedMoveSummary(history: List<String>, complexity: TrainingComplexity): String {
        val options = candidates(history, complexity)
        if (options.isEmpty()) return "Aucun coup du module local n'est défini ici."
        return options.joinToString(" ou ") { "${it.move.san} (${it.move.uci})" }
    }

    fun availableLineCount(complexity: TrainingComplexity): Int =
        lines.count { it.minComplexity.level <= complexity.level }

    private fun matchingLines(
        history: List<String>,
        complexity: TrainingComplexity
    ): List<TrainingLine> = lines.filter { line ->
        line.minComplexity.level <= complexity.level &&
            history.size <= line.moves.size &&
            line.moves.take(history.size).map { it.uci } == history
    }

    private fun m(uci: String, san: String, explanation: String) =
        MoveLesson(uci = uci, san = san, explanation = explanation)
}
