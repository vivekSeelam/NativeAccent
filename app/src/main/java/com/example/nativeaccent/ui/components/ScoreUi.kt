package com.example.nativeaccent.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nativeaccent.assessment.DisplayWord
import com.example.nativeaccent.assessment.PhonemeResult
import com.example.nativeaccent.assessment.PronunciationResult
import com.example.nativeaccent.assessment.ScoreBand
import com.example.nativeaccent.ui.theme.InkStroke
import com.example.nativeaccent.ui.theme.ScoreFair
import com.example.nativeaccent.ui.theme.ScoreGood
import com.example.nativeaccent.ui.theme.ScorePoor
import com.example.nativeaccent.ui.theme.TextMuted
import com.example.nativeaccent.ui.theme.TextPrimary
import com.example.nativeaccent.ui.theme.TextSecondary

val ScoreBand.color: Color
    get() = when (this) {
        ScoreBand.GOOD -> ScoreGood
        ScoreBand.FAIR -> ScoreFair
        ScoreBand.POOR -> ScorePoor
    }

fun scoreColor(score: Int): Color = ScoreBand.of(score).color

/** Circular progress ring for the overall score; sweeps in from zero on first show. */
@Composable
fun ScoreRing(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
    strokeWidth: Dp = 12.dp,
    label: String = "Overall",
) {
    val color = scoreColor(score)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(score) {
        progress.animateTo(score / 100f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "$label score $score out of 100" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = InkStroke,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString(),
                color = TextPrimary,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

/** Accuracy / Fluency / Completeness, each coloured by its own band. */
@Composable
fun SubScoreRow(result: PronunciationResult, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        SubScore("Accuracy", result.accuracyScore)
        SubScore("Fluency", result.fluencyScore)
        SubScore("Completeness", result.completenessScore)
    }
}

@Composable
private fun SubScore(label: String, score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = scoreColor(score),
            fontWeight = FontWeight.SemiBold,
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
    }
}

/**
 * The sentence, one tappable word at a time, each coloured by its own accuracy.
 * Skipped words are struck through; the focused word gets a tinted backdrop.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScoredSentence(
    words: List<DisplayWord>,
    result: PronunciationResult?,
    focusedIndex: Int?,
    onWordClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        words.forEach { display ->
            val index = display.resultIndex
            val scored = if (index != null) result?.words?.getOrNull(index) else null
            val color = scored?.let { scoreColor(it.accuracyScore) } ?: TextPrimary
            val focused = index != null && index == focusedIndex
            val shape = RoundedCornerShape(10.dp)

            var wordModifier = Modifier.clip(shape)
            if (focused) wordModifier = wordModifier.background(color.copy(alpha = 0.16f)).border(1.dp, color, shape)
            if (index != null && scored != null) {
                wordModifier = wordModifier
                    .clickable(role = Role.Button) { onWordClick(index) }
                    .semantics {
                        contentDescription = if (scored.wasSkipped) "${display.text}, skipped"
                        else "${display.text}, ${scored.accuracyScore} out of 100"
                    }
            }

            Text(
                text = display.text,
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                textDecoration = if (scored?.wasSkipped == true) TextDecoration.LineThrough else null,
                modifier = wordModifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** A word's sounds as small chips, e.g. K · AH · M · IH · T · IY, each coloured by its score. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhonemeChips(phonemes: List<PhonemeResult>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        phonemes.forEach { phoneme ->
            val color = scoreColor(phoneme.accuracyScore)
            val shape = RoundedCornerShape(12.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(shape)
                    .background(color.copy(alpha = 0.14f))
                    .border(1.dp, color.copy(alpha = 0.7f), shape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .semantics {
                        contentDescription = "sound ${phoneme.phoneme}, ${phoneme.accuracyScore} out of 100"
                    },
            ) {
                Text(
                    text = phoneme.phoneme.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                )
                Text(
                    text = phoneme.accuracyScore.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}
