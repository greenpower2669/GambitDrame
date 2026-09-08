package com.gambitdrame.app.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.gambitdrame.app.data.ProgressStore
import com.gambitdrame.app.domain.ChessBoard
import com.gambitdrame.app.domain.QueenGambitTheory
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun GambitTrainerScreen(
    onSpeak: (String) -> Unit
) {
    val context = LocalContext.current
    val theory = remember { QueenGambitTheory() }
    val progressStore = remember { ProgressStore(context.applicationContext) }
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 72) }

    DisposableEffect(Unit) {
        onDispose { tone.release() }
    }

    var board by remember { mutableStateOf(ChessBoard.initial()) }
    var history by remember { mutableStateOf(emptyList<String>()) }
    var selectedSquare by remember { mutableStateOf<String?>(null) }
    var sessionAttempts by remember { mutableIntStateOf(0) }
    var sessionCorrect by remember { mutableIntStateOf(0) }
    var totalAttempts by remember { mutableIntStateOf(progressStore.totalAttempts) }
    var totalCorrect by remember { mutableIntStateOf(progressStore.totalCorrect) }
    var lastExplanation by remember {
        mutableStateOf("Commence par jouer d4. Le module te dira si tu restes dans l'arbre du Gambit Dame.")
    }
    var explanationVisible by remember { mutableStateOf(false) }

    val whiteToMove = history.size % 2 == 0
    val sessionScore = if (sessionAttempts == 0) 1f else sessionCorrect.toFloat() / sessionAttempts
    val globalScore = if (totalAttempts == 0) 1f else totalCorrect.toFloat() / totalAttempts

    fun resetLine() {
        board = ChessBoard.initial()
        history = emptyList()
        selectedSquare = null
        sessionAttempts = 0
        sessionCorrect = 0
        explanationVisible = false
        lastExplanation = "Nouvelle ligne. À toi : cherche les coups du Gambit Dame."
    }

    fun recordAttempt(correct: Boolean) {
        sessionAttempts += 1
        if (correct) sessionCorrect += 1
        progressStore.record(correct)
        totalAttempts = progressStore.totalAttempts
        totalCorrect = progressStore.totalCorrect
    }

    fun playUserMove(uci: String) {
        val candidate = theory.findCandidate(history, uci)
        if (candidate != null) {
            board = board.applyMove(uci)
            history = history + uci
            recordAttempt(correct = true)
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 90)
            lastExplanation = "${candidate.move.san}. ${candidate.move.explanation}"
            explanationVisible = false
        } else {
            recordAttempt(correct = false)
            tone.startTone(ToneGenerator.TONE_PROP_NACK, 160)
            lastExplanation = buildString {
                append("Là, tu t'éloignes de la théorie travaillée. ")
                append("Le module attend : ")
                append(theory.expectedMoveSummary(history))
                append(". Essaie un autre coup sans perdre la position.")
            }
            explanationVisible = true
        }
        selectedSquare = null
    }

    // The app plays Black automatically. Its branch is randomised using local
    // training weights; live frequencies will replace/complement them later.
    LaunchedEffect(history) {
        if (history.isNotEmpty() && history.size % 2 == 1) {
            delay(420)
            val reply = theory.chooseReply(history)
            if (reply != null) {
                board = board.applyMove(reply.move.uci)
                history = history + reply.move.uci
                lastExplanation = "Les Noirs jouent ${reply.move.san}. ${reply.move.explanation}"
                explanationVisible = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "GambitDrame",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Entraînement · Gambit Dame",
            fontSize = 19.sp
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "${(sessionScore * 100).roundToInt()} % théorie sur cette session",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        LinearProgressIndicator(
            progress = { sessionScore },
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
        )
        Text(
            text = if (totalAttempts == 0) {
                "Historique : pas encore de tentative enregistrée"
            } else {
                "Historique local : ${(globalScore * 100).roundToInt()} % · $totalCorrect / $totalAttempts"
            },
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 5.dp)
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = theory.branchLabel(history),
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (whiteToMove) "À toi (Blancs)" else "Les Noirs réfléchissent…",
            fontSize = 17.sp
        )

        Spacer(Modifier.height(8.dp))

        ChessBoardView(
            board = board,
            selectedSquare = selectedSquare,
            inputEnabled = whiteToMove,
            onSquareClick = { square ->
                val piece = board.pieceAt(square)
                when {
                    selectedSquare == null && ChessBoard.isWhite(piece) -> {
                        selectedSquare = square
                    }
                    selectedSquare != null && ChessBoard.isWhite(piece) -> {
                        // Tapping another white piece simply changes selection.
                        selectedSquare = square
                    }
                    selectedSquare != null -> {
                        playUserMove(selectedSquare + square)
                    }
                }
            }
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    explanationVisible = true
                    onSpeak(lastExplanation)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("!  Explication", fontSize = 17.sp)
            }
            Button(
                onClick = { resetLine() },
                modifier = Modifier.weight(1f)
            ) {
                Text("↻ Rejouer", fontSize = 17.sp)
            }
        }

        if (explanationVisible) {
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = lastExplanation,
                        fontSize = 19.sp,
                        lineHeight = 27.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onSpeak(lastExplanation) }) {
                        Text("🔊 Relire à voix haute", fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "V0.1 : les pondérations servent seulement à varier l'entraînement. Elles ne sont pas encore des fréquences Lichess/Chess.com.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ChessBoardView(
    board: ChessBoard,
    selectedSquare: String?,
    inputEnabled: Boolean,
    onSquareClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(2.dp, Color.Black)
    ) {
        for (row in 0 until 8) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0 until 8) {
                    val index = row * 8 + col
                    val square = ChessBoard.squareName(index)
                    val piece = board.squares[index]
                    val isLight = (row + col) % 2 == 0
                    val selected = square == selectedSquare
                    val squareColor = if (isLight) Color(0xFFECEFF1) else Color(0xFF607D8B)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(squareColor)
                            .then(
                                if (selected) Modifier.border(4.dp, Color(0xFFFFC107))
                                else Modifier
                            )
                            .semantics {
                                contentDescription = "$square, ${ChessBoard.spokenName(piece)}"
                            }
                            .clickable(enabled = inputEnabled) { onSquareClick(square) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (piece != null) {
                            val white = ChessBoard.isWhite(piece)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = if (white) Color.White else Color.Black,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ChessBoard.glyph(piece),
                                    fontSize = 30.sp,
                                    color = if (white) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
