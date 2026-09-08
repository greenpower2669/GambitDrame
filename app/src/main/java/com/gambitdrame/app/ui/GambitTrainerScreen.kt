package com.gambitdrame.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gambitdrame.app.data.ProgressStore
import com.gambitdrame.app.domain.ChessBoard
import com.gambitdrame.app.domain.QueenGambitTheory
import com.gambitdrame.app.domain.TrainingComplexity
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
    var complexitySlider by remember { mutableStateOf(1f) }
    var lastExplanation by remember {
        mutableStateOf("Commence par jouer d4. Les Noirs choisiront une réponse dans le niveau sélectionné.")
    }
    var explanationVisible by remember { mutableStateOf(false) }

    val complexity = when (complexitySlider.roundToInt().coerceIn(0, 2)) {
        0 -> TrainingComplexity.MEDIUM
        1 -> TrainingComplexity.ADVANCED
        else -> TrainingComplexity.COMPLEX
    }
    val whiteToMove = history.size % 2 == 0
    val sessionScore = if (sessionAttempts == 0) 1f else sessionCorrect.toFloat() / sessionAttempts
    val globalScore = if (totalAttempts == 0) 1f else totalCorrect.toFloat() / totalAttempts

    fun resetLine(message: String = "Nouvelle ligne. À toi : cherche les coups du Gambit Dame.") {
        board = ChessBoard.initial()
        history = emptyList()
        selectedSquare = null
        sessionAttempts = 0
        sessionCorrect = 0
        explanationVisible = false
        lastExplanation = message
    }

    fun recordAttempt(correct: Boolean) {
        sessionAttempts += 1
        if (correct) sessionCorrect += 1
        progressStore.record(correct)
        totalAttempts = progressStore.totalAttempts
        totalCorrect = progressStore.totalCorrect
    }

    fun playUserMove(uci: String) {
        val candidate = theory.findCandidate(history, uci, complexity)
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
                append("Là, tu t'éloignes de la théorie travaillée au niveau ${complexity.label}. ")
                append("Le module attend : ")
                append(theory.expectedMoveSummary(history, complexity))
                append(". Essaie un autre coup sans perdre la position.")
            }
            explanationVisible = true
        }
        selectedSquare = null
    }

    fun openChatGptWithContext() {
        val moves = if (history.isEmpty()) "Aucun coup encore joué" else history.joinToString(" ")
        val branch = theory.branchLabel(history, complexity)
        val expected = theory.expectedMoveSummary(history, complexity)
        val prompt = buildString {
            appendLine("Je m'entraîne au Gambit Dame dans l'application GambitDrame.")
            appendLine("Niveau de variantes : ${complexity.label}.")
            appendLine("Branche actuelle : $branch.")
            appendLine("Coups joués en UCI : $moves.")
            appendLine("Dernier retour pédagogique : $lastExplanation")
            appendLine("Coups théoriques proposés ici : $expected")
            appendLine()
            append("Explique-moi simplement ce qui se joue dans cette position, pourquoi ces coups sont théoriques, les idées à retenir et les erreurs typiques à éviter. Adapte l'explication à un joueur amateur qui veut mémoriser les idées plutôt que réciter des coups.")
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("GambitDrame · contexte ChatGPT", prompt))

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onSuccess {
                Toast.makeText(
                    context,
                    "Contexte copié : colle-le dans ChatGPT.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .onFailure {
                Toast.makeText(
                    context,
                    "Contexte copié. Ouvre ChatGPT puis colle-le.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    LaunchedEffect(complexity) {
        if (history.isNotEmpty()) {
            resetLine("Niveau ${complexity.label}. Nouvelle variante tirée au prochain départ.")
        }
    }

    LaunchedEffect(history, complexity) {
        if (history.isNotEmpty() && history.size % 2 == 1) {
            delay(420)
            val reply = theory.chooseReply(history, complexity)
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

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = "Variantes : ${complexity.label}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${theory.availableLineCount(complexity)} lignes locales actives · ${complexity.description}",
                    fontSize = 15.sp
                )
                Slider(
                    value = complexitySlider,
                    onValueChange = { complexitySlider = it },
                    valueRange = 0f..2f,
                    steps = 1,
                    modifier = Modifier.semantics {
                        contentDescription = "Complexité des variantes : ${complexity.label}"
                    }
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Moyen", modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Text(
                        "Avancé",
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Complexe",
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.End
                    )
                }
            }
        }

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
            text = theory.branchLabel(history, complexity),
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (whiteToMove) "À toi (Blancs)" else "Les Noirs choisissent une variante…",
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

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { openChatGptWithContext() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("↗ Approfondir dans ChatGPT", fontSize = 17.sp)
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
            text = "V0.2 : le curseur élargit l'arbre local. Les pondérations servent encore à varier l'entraînement et ne sont pas présentées comme des fréquences réelles.",
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
