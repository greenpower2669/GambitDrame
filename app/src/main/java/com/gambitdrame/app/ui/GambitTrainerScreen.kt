package com.gambitdrame.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.gambitdrame.app.data.AiProviderStore
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
    val aiProviderStore = remember { AiProviderStore(context.applicationContext) }
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 72) }
    val moveProgress = remember { Animatable(1f) }

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
    var lineCompleted by remember { mutableStateOf(false) }

    var lastMoveFrom by remember { mutableStateOf<String?>(null) }
    var lastMoveTo by remember { mutableStateOf<String?>(null) }
    var moveAnimationToken by remember { mutableIntStateOf(0) }

    var selectedAi by remember { mutableStateOf(aiProviderStore.selected) }
    var showAiSettings by remember { mutableStateOf(false) }
    var settingsChoiceId by remember { mutableStateOf(selectedAi.id) }
    var customAiName by remember { mutableStateOf(if (selectedAi.custom) selectedAi.name else "Mon IA") }
    var customAiUrl by remember { mutableStateOf(if (selectedAi.custom) selectedAi.url else "") }

    val complexity = when (complexitySlider.roundToInt().coerceIn(0, 2)) {
        0 -> TrainingComplexity.MEDIUM
        1 -> TrainingComplexity.ADVANCED
        else -> TrainingComplexity.COMPLEX
    }
    val whiteToMove = !lineCompleted && history.size % 2 == 0
    val sessionScore = if (sessionAttempts == 0) 1f else sessionCorrect.toFloat() / sessionAttempts
    val globalScore = if (totalAttempts == 0) 1f else totalCorrect.toFloat() / totalAttempts

    fun startMoveAnimation(uci: String) {
        if (uci.length >= 4) {
            lastMoveFrom = uci.substring(0, 2)
            lastMoveTo = uci.substring(2, 4)
            moveAnimationToken += 1
        }
    }

    fun resetLine(message: String = "Nouvelle ligne. À toi : cherche les coups du Gambit Dame.") {
        board = ChessBoard.initial()
        history = emptyList()
        selectedSquare = null
        sessionAttempts = 0
        sessionCorrect = 0
        explanationVisible = false
        lineCompleted = false
        lastMoveFrom = null
        lastMoveTo = null
        lastExplanation = message
    }

    fun recordAttempt(correct: Boolean) {
        sessionAttempts += 1
        if (correct) sessionCorrect += 1
        progressStore.record(correct)
        totalAttempts = progressStore.totalAttempts
        totalCorrect = progressStore.totalCorrect
    }

    fun conceptualHint(): String {
        val expected = theory.expectedMoveSummary(history, complexity)
        return when {
            expected.contains("(d2d4)") -> "Prends de l'espace au centre."
            expected.contains("(c2c4)") -> "Mets la pression sur le centre."
            expected.contains("(b1c3)") -> "Développe en renforçant la pression."
            expected.contains("(g1f3)") -> "Développe une pièce vers le centre."
            expected.contains("(c1g5)") -> "Cherche une pression sur un défenseur important."
            expected.contains("(c4d5)") -> "La tension centrale peut être clarifiée."
            expected.contains("(e2e3)") -> "Consolide avant de poursuivre le développement."
            expected.contains("(f1c4)") || expected.contains("(d3c4)") ->
                "Le pion offert peut maintenant être récupéré en développant."
            expected.contains("(a2a4)") -> "Empêche l'expansion noire à l'aile dame."
            expected.contains("(e1g1)") -> "Pense à la sécurité de ton roi."
            expected.contains("(d1e2)") || expected.contains("(d1c2)") ->
                "Coordonne ta dame avec le centre."
            expected.contains("(g2g3)") -> "Prépare un développement en fianchetto."
            expected.contains("(f1g2)") -> "Active ton fou sur une grande diagonale."
            expected.contains("(d4e5)") -> "Décide comment répondre au défi central."
            expected.contains("(a2a3)") -> "Prépare ton jeu à l'aile dame."
            expected.contains("(b1d2)") || expected.contains("(f3d2)") ->
                "Réorganise un cavalier pour réduire la pression tactique."
            expected.contains("(g5h4)") || expected.contains("(h4g3)") ->
                "Conserve ton fou actif sans céder la pression."
            else -> "Cherche le développement, le centre et la sécurité du roi."
        }
    }

    fun playUserMove(uci: String) {
        val candidate = theory.findCandidate(history, uci, complexity)
        if (candidate != null) {
            startMoveAnimation(uci)
            board = board.applyMove(uci)
            val nextHistory = history + uci
            history = nextHistory
            recordAttempt(correct = true)
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 90)
            lastExplanation = "${candidate.move.san}. ${candidate.move.explanation}"
            explanationVisible = false

            if (theory.candidates(nextHistory, complexity).isEmpty()) {
                lineCompleted = true
                lastExplanation += " Variante terminée pour cette ligne d'entraînement."
            }
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

    fun openSelectedAiWithContext() {
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
        clipboard.setPrimaryClip(
            ClipData.newPlainText("GambitDrame · contexte ${selectedAi.name}", prompt)
        )

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selectedAi.url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onSuccess {
                Toast.makeText(
                    context,
                    "Contexte copié. Dans ${selectedAi.name}, colle-le dans la zone de message.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .onFailure {
                Toast.makeText(
                    context,
                    "Contexte copié. Impossible d'ouvrir ${selectedAi.name} automatiquement.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    LaunchedEffect(moveAnimationToken) {
        if (moveAnimationToken > 0) {
            moveProgress.snapTo(0f)
            moveProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 280)
            )
        }
    }

    LaunchedEffect(complexity) {
        if (history.isNotEmpty()) {
            resetLine("Niveau ${complexity.label}. Nouvelle variante tirée au prochain départ.")
        }
    }

    LaunchedEffect(history, complexity, lineCompleted) {
        if (!lineCompleted && history.isNotEmpty() && history.size % 2 == 1) {
            delay(420)
            val reply = theory.chooseReply(history, complexity)
            if (reply != null) {
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                startMoveAnimation(reply.move.uci)
                board = board.applyMove(reply.move.uci)
                val nextHistory = history + reply.move.uci
                history = nextHistory
                lastExplanation = "Les Noirs jouent ${reply.move.san}. ${reply.move.explanation}"
                explanationVisible = false

                if (theory.candidates(nextHistory, complexity).isEmpty()) {
                    lineCompleted = true
                    lastExplanation += " Variante terminée pour cette ligne d'entraînement."
                }
            } else {
                lineCompleted = true
                lastExplanation = "La variante locale s'arrête ici. Tu peux lancer une nouvelle branche."
            }
        }
    }

    LaunchedEffect(whiteToMove, history.size, lineCompleted) {
        if (whiteToMove && history.isNotEmpty() && !lineCompleted) {
            delay(330)
            tone.startTone(ToneGenerator.TONE_PROP_PROMPT, 90)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GambitDrame",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Entraînement · Gambit Dame",
                    fontSize = 19.sp
                )
            }
            TextButton(
                onClick = {
                    settingsChoiceId = selectedAi.id
                    customAiName = if (selectedAi.custom) selectedAi.name else "Mon IA"
                    customAiUrl = if (selectedAi.custom) selectedAi.url else ""
                    showAiSettings = true
                },
                modifier = Modifier.semantics {
                    contentDescription = "Réglages de l'intelligence artificielle externe"
                }
            ) {
                Text("⚙", fontSize = 28.sp)
            }
        }

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

        Spacer(Modifier.height(6.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    lineCompleted -> Color(0xFFDDF4E4)
                    whiteToMove -> Color(0xFFFFF1B8)
                    else -> Color(0xFFDCE8F7)
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        lineCompleted -> "✓ VARIANTE TERMINÉE"
                        whiteToMove -> "● À VOUS DE JOUER"
                        else -> "● LES NOIRS JOUENT…"
                    },
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        lineCompleted -> "Cette ligne locale s'arrête ici. Rejouer lance une autre branche."
                        whiteToMove -> "Indice : ${conceptualHint()}"
                        else -> "Regarde le déplacement : la réponse noire peut changer selon la variante."
                    },
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        ChessBoardView(
            board = board,
            selectedSquare = selectedSquare,
            inputEnabled = whiteToMove,
            lastMoveFrom = lastMoveFrom,
            lastMoveTo = lastMoveTo,
            moveProgress = moveProgress.value,
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
                Text(if (lineCompleted) "↻ Autre ligne" else "↻ Rejouer", fontSize = 17.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { openSelectedAiWithContext() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("↗ Approfondir avec ${selectedAi.name}", fontSize = 17.sp)
        }
        Text(
            text = "Le contexte est copié : la page externe peut s'ouvrir vide, il suffit de coller.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )

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
            text = "V0.2.2 : déplacement animé, sons de tour, gros indicateur de joueur et indice conceptuel sans donner directement le coup.",
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (showAiSettings) {
        AlertDialog(
            onDismissRequest = { showAiSettings = false },
            title = { Text("IA pour approfondir") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Choisis le service ouvert par le bouton d'approfondissement. Le contexte reste copié dans le presse-papiers."
                    )
                    Spacer(Modifier.height(8.dp))

                    aiProviderStore.builtIns.forEach { provider ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { settingsChoiceId = provider.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settingsChoiceId == provider.id,
                                onClick = { settingsChoiceId = provider.id }
                            )
                            Text(provider.name, fontSize = 17.sp)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingsChoiceId = "custom" }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settingsChoiceId == "custom",
                            onClick = { settingsChoiceId = "custom" }
                        )
                        Text("Autre IA / personnalisée", fontSize = 17.sp)
                    }

                    if (settingsChoiceId == "custom") {
                        OutlinedTextField(
                            value = customAiName,
                            onValueChange = { customAiName = it },
                            label = { Text("Nom de mon IA") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customAiUrl,
                            onValueChange = { customAiUrl = it },
                            label = { Text("Adresse web") },
                            placeholder = { Text("exemple.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (settingsChoiceId == "custom") {
                            aiProviderStore.selectCustom(customAiName, customAiUrl)
                        } else {
                            aiProviderStore.selectBuiltIn(settingsChoiceId)
                        }
                        selectedAi = aiProviderStore.selected
                        showAiSettings = false
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiSettings = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun ChessBoardView(
    board: ChessBoard,
    selectedSquare: String?,
    inputEnabled: Boolean,
    lastMoveFrom: String?,
    lastMoveTo: String?,
    moveProgress: Float,
    onSquareClick: (String) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(2.dp, Color.Black)
    ) {
        val density = LocalDensity.current
        val cellPx = with(density) { (maxWidth / 8).toPx() }

        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0 until 8) {
                Row(modifier = Modifier.weight(1f)) {
                    for (col in 0 until 8) {
                        val index = row * 8 + col
                        val square = ChessBoard.squareName(index)
                        val piece = board.squares[index]
                        val isLight = (row + col) % 2 == 0
                        val selected = square == selectedSquare
                        val isLastMove = square == lastMoveFrom || square == lastMoveTo
                        val isAnimatedDestination = square == lastMoveTo && lastMoveFrom != null
                        val squareColor = if (isLight) Color(0xFFECEFF1) else Color(0xFF607D8B)

                        val fromIndex = if (isAnimatedDestination && lastMoveFrom != null) {
                            ChessBoard.indexOf(lastMoveFrom)
                        } else {
                            index
                        }
                        val fromRow = fromIndex / 8
                        val fromCol = fromIndex % 8
                        val translationX = (fromCol - col) * cellPx * (1f - moveProgress)
                        val translationY = (fromRow - row) * cellPx * (1f - moveProgress)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .zIndex(if (isAnimatedDestination && moveProgress < 1f) 2f else 0f)
                                .background(squareColor)
                                .then(
                                    when {
                                        selected -> Modifier.border(4.dp, Color(0xFFFFC107))
                                        isLastMove -> Modifier.border(3.dp, Color(0xFF42A5F5))
                                        else -> Modifier
                                    }
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
                                        .graphicsLayer {
                                            if (isAnimatedDestination) {
                                                this.translationX = translationX
                                                this.translationY = translationY
                                                val scale = 0.92f + (0.08f * moveProgress)
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                        }
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
}
