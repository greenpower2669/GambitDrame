package com.gambitdrame.app.domain

data class MoveLesson(
    val uci: String,
    val san: String,
    val explanation: String
)

data class TrainingLine(
    val name: String,
    val trainingWeight: Int,
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

        // Castling support is already here for future deeper lines.
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
