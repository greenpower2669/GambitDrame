package com.gambitdrame.app.domain

import kotlin.random.Random

class QueenGambitTheory {

    /*
     * v0.1 deliberately ships with a small, auditable local repertoire.
     * trainingWeight is NOT presented as an online frequency. It is only a
     * local randomisation weight until live/open datasets are plugged in.
     */
    private val lines = listOf(
        TrainingLine(
            name = "Gambit Dame refusé · ligne orthodoxe",
            trainingWeight = 45,
            moves = listOf(
                m("d2d4", "d4", "Tu prends de l'espace au centre et tu prépares c4."),
                m("d7d5", "…d5", "Les Noirs répondent symétriquement et contestent le centre."),
                m("c2c4", "c4", "C'est le coup caractéristique du Gambit Dame : tu mets le pion d5 sous pression."),
                m("e7e6", "…e6", "Le gambit est refusé. Les Noirs soutiennent d5 et libèrent leur fou de cases noires."),
                m("b1c3", "Cc3", "Le cavalier renforce la pression sur d5 et développe une pièce avec tempo positionnel."),
                m("g8f6", "…Cf6", "Développement naturel : les Noirs contrôlent e4 et soutiennent le centre."),
                m("c1g5", "Fg5", "Le fou cloue le cavalier f6 et augmente indirectement la pression sur d5."),
                m("f8e7", "…Fe7", "Les Noirs cassent le clou et préparent le roque."),
                m("e2e3", "e3", "Tu consolides d4 et ouvres la diagonale du fou f1."),
                m("e8g8", "…O-O", "Les Noirs mettent leur roi à l'abri. La bataille devient surtout positionnelle.")
            )
        ),
        TrainingLine(
            name = "Gambit Dame refusé · variante d'échange",
            trainingWeight = 25,
            moves = listOf(
                m("d2d4", "d4", "Tu prends de l'espace au centre et tu prépares c4."),
                m("d7d5", "…d5", "Les Noirs répondent symétriquement et contestent le centre."),
                m("c2c4", "c4", "C'est le coup caractéristique du Gambit Dame : tu mets le pion d5 sous pression."),
                m("e7e6", "…e6", "Le gambit est refusé : le pion d5 reste soutenu."),
                m("b1c3", "Cc3", "Tu développes en ajoutant une pression sur d5."),
                m("g8f6", "…Cf6", "Les Noirs développent et contrôlent e4."),
                m("c4d5", "cxd5", "Tu échanges la tension centrale. Cette structure mène souvent à des plans très pédagogiques de majorité et d'attaque de minorité."),
                m("e6d5", "…exd5", "Les Noirs reprennent et une structure de pions typique du Gambit Dame apparaît."),
                m("c1g5", "Fg5", "Tu développes avec pression sur le cavalier f6.")
            )
        ),
        TrainingLine(
            name = "Gambit Dame accepté",
            trainingWeight = 18,
            moves = listOf(
                m("d2d4", "d4", "Tu prends de l'espace au centre et tu prépares c4."),
                m("d7d5", "…d5", "Les Noirs installent un pion central solide."),
                m("c2c4", "c4", "Tu offres temporairement le pion c pour détourner le pion d5 du centre."),
                m("d5c4", "…dxc4", "Le Gambit Dame est accepté. L'objectif blanc n'est pas de récupérer le pion dans la panique, mais de gagner du temps et du centre."),
                m("e2e4", "e4", "Tu construis un gros centre. C'est une façon énergique de profiter du temps dépensé par les Noirs pour prendre c4."),
                m("e7e5", "…e5", "Les Noirs contre-attaquent immédiatement le centre blanc."),
                m("g1f3", "Cf3", "Développement, contrôle de e5 et préparation rapide du roque."),
                m("e5d4", "…exd4", "Les Noirs clarifient le centre."),
                m("f1c4", "Fxc4", "Le fou récupère naturellement le pion c4 tout en se développant.")
            )
        ),
        TrainingLine(
            name = "Défense slave contre le Gambit Dame",
            trainingWeight = 12,
            moves = listOf(
                m("d2d4", "d4", "Tu prends de l'espace au centre et tu prépares c4."),
                m("d7d5", "…d5", "Les Noirs occupent le centre."),
                m("c2c4", "c4", "Tu mets d5 sous pression : position de Gambit Dame."),
                m("c7c6", "…c6", "La Slave soutient d5 avec le pion c sans enfermer immédiatement le fou c8."),
                m("g1f3", "Cf3", "Tu développes sans fermer les options de ton cavalier b1."),
                m("g8f6", "…Cf6", "Les Noirs développent et renforcent le centre."),
                m("b1c3", "Cc3", "Tu augmentes la pression centrale et poursuis le développement."),
                m("d5c4", "…dxc4", "Les Noirs relâchent la tension et prennent c4."),
                m("e2e4", "e4", "Tu profites du temps disponible pour bâtir un centre large.")
            )
        )
    )

    fun candidates(history: List<String>): List<TheoryCandidate> {
        val matchingLines = matchingLines(history)
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

    fun findCandidate(history: List<String>, uci: String): TheoryCandidate? =
        candidates(history).firstOrNull { it.move.uci == uci }

    fun chooseReply(history: List<String>): TheoryCandidate? {
        val options = candidates(history)
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

    fun branchLabel(history: List<String>): String {
        val names = matchingLines(history).map { it.name }.distinct()
        return when {
            names.isEmpty() -> "Hors arbre local"
            names.size == 1 -> names.first()
            else -> "Tronc commun · ${names.size} branches possibles"
        }
    }

    fun expectedMoveSummary(history: List<String>): String {
        val options = candidates(history)
        if (options.isEmpty()) return "Aucun coup du module local n'est défini ici."
        return options.joinToString(" ou ") { "${it.move.san} (${it.move.uci})" }
    }

    private fun matchingLines(history: List<String>): List<TrainingLine> =
        lines.filter { line ->
            history.size <= line.moves.size &&
                line.moves.take(history.size).map { it.uci } == history
        }

    private fun m(uci: String, san: String, explanation: String) =
        MoveLesson(uci = uci, san = san, explanation = explanation)
}
