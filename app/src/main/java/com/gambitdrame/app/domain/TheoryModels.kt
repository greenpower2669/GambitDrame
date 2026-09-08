package com.gambitdrame.app.domain

enum class TrainingComplexity(
    val level: Int,
    val label: String,
    val description: String
) {
    MEDIUM(
        level = 1,
        label = "Moyen",
        description = "Les grandes réponses classiques du Gambit Dame."
    ),
    ADVANCED(
        level = 2,
        label = "Avancé",
        description = "Ajoute davantage de bifurcations et de défenses positionnelles."
    ),
    COMPLEX(
        level = 3,
        label = "Complexe",
        description = "Ajoute les réponses plus rares, tranchantes ou théoriques."
    );

    companion object {
        fun fromSlider(value: Float): TrainingComplexity = when (value.toInt()) {
            0 -> MEDIUM
            1 -> ADVANCED
            else -> COMPLEX
        }
    }
}

data class MoveLesson(
    val uci: String,
    val san: String,
    val explanation: String
)

data class TrainingLine(
    val name: String,
    val trainingWeight: Int,
    val minComplexity: TrainingComplexity = TrainingComplexity.MEDIUM,
    val moves: List<MoveLesson>
)

data class TheoryCandidate(
    val move: MoveLesson,
    val weight: Int
)

data class ChessBoard(
    val squares: List<Char?>
) {
    fun pieceAt(square: String): Char? = squares[indexOf(square)]

    fun applyMove(uci: String): ChessBoard {
        if (uci.length < 4) return this

        val from = uci.substring(0, 2)
        val to = uci.substring(2, 4)
        val fromIndex = indexOf(from)
        val toIndex = indexOf(to)
        val movingPiece = squares.getOrNull(fromIndex) ?: return this
        val next = squares.toMutableList()

        next[fromIndex] = null
        next[toIndex] = movingPiece

        when (uci) {
            "e1g1" -> moveRook(next, "h1", "f1")
            "e1c1" -> moveRook(next, "a1", "d1")
            "e8g8" -> moveRook(next, "h8", "f8")
            "e8c8" -> moveRook(next, "a8", "d8")
        }

        return copy(squares = next)
    }

    private fun moveRook(board: MutableList<Char?>, from: String, to: String) {
        val fromIndex = indexOf(from)
        val toIndex = indexOf(to)
        board[toIndex] = board[fromIndex]
        board[fromIndex] = null
    }

    companion object {
        fun initial(): ChessBoard = fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR")

        fun fromFen(piecePlacement: String): ChessBoard {
            val result = MutableList<Char?>(64) { null }
            var index = 0

            piecePlacement.forEach { token ->
                when {
                    token == '/' -> Unit
                    token.isDigit() -> index += token.digitToInt()
                    else -> {
                        if (index in result.indices) result[index] = token
                        index++
                    }
                }
            }
            return ChessBoard(result)
        }

        fun indexOf(square: String): Int {
            require(square.length == 2) { "Invalid square: $square" }
            val file = square[0] - 'a'
            val rank = square[1].digitToInt()
            val row = 8 - rank
            return row * 8 + file
        }

        fun squareName(index: Int): String {
            val row = index / 8
            val col = index % 8
            val file = ('a'.code + col).toChar()
            val rank = 8 - row
            return "$file$rank"
        }

        fun isWhite(piece: Char?): Boolean = piece?.isUpperCase() == true

        fun glyph(piece: Char?): String = when (piece) {
            'K' -> "♔"
            'Q' -> "♕"
            'R' -> "♖"
            'B' -> "♗"
            'N' -> "♘"
            'P' -> "♙"
            'k' -> "♚"
            'q' -> "♛"
            'r' -> "♜"
            'b' -> "♝"
            'n' -> "♞"
            'p' -> "♟"
            else -> ""
        }

        fun spokenName(piece: Char?): String = when (piece?.lowercaseChar()) {
            'k' -> "roi"
            'q' -> "dame"
            'r' -> "tour"
            'b' -> "fou"
            'n' -> "cavalier"
            'p' -> "pion"
            else -> "case vide"
        }
    }
}
