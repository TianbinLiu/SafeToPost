package app.safetopost.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.safetopost.ScanPhase
import app.safetopost.ScanState
import app.safetopost.ScanViewModel
import app.safetopost.model.CaptionFinding
import app.safetopost.model.FaceRegion
import app.safetopost.model.PhotoMetadata
import app.safetopost.model.RiskLevel
import app.safetopost.model.RiskScore
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(viewModel: ScanViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::load)
    }

    LaunchedEffect(state.message?.id) {
        state.message?.let {
            snackbar.showSnackbar(it.text)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text("Safe to Post") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.phase == ScanPhase.SCANNING || state.phase == ScanPhase.EXPORTING) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (state.photo == null) {
                EmptyState(onPick = { picker.launch("image/*") })
            } else {
                ScoreHeader(state.score)
                PhotoPreview(state.photo!!.bitmap, state.faces)
                FaceChips(state.faces, viewModel::toggleFace)
                MetadataSection(state.metadata)
                CaptionEditor(
                    caption = state.caption,
                    findings = state.findings,
                    onCaptionChange = viewModel::updateCaption
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = viewModel::export,
                        enabled = state.phase == ScanPhase.READY,
                        modifier = Modifier.weight(1f)
                    ) { Text("Clean & export") }
                    OutlinedButton(
                        onClick = { picker.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) { Text("Pick another") }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmptyState(onPick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Check a photo before you post it.", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Hidden metadata, faces, and caption risk. Everything stays on the phone.",
            color = Muted,
            fontSize = 14.sp
        )
        Button(onClick = onPick) { Text("Choose a photo") }
        Text("Android 10 or newer", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun ScoreHeader(score: RiskScore) {
    val levelColor = when (score.level) {
        RiskLevel.LOW -> Color(0xFF2E7D5B)
        RiskLevel.MEDIUM -> Color(0xFFB07A1E)
        RiskLevel.HIGH -> Accent
    }
    Card(colors = CardDefaults.cardColors(containerColor = Sand)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("PRIVACY RISK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Muted)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${score.total}", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = levelColor)
                Text(" / 100", fontSize = 16.sp, color = Muted, modifier = Modifier.padding(bottom = 8.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    score.level.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = levelColor,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            if (score.factors.isEmpty()) {
                Text("No metadata or caption findings.", fontSize = 12.sp, color = Muted)
            } else {
                score.factors.take(8).forEach { factor ->
                    Text(
                        "\u2022 ${factor.explanation}   +${factor.weight}",
                        fontSize = 12.sp,
                        color = Ink.copy(alpha = 0.75f)
                    )
                }
                if (score.factors.size > 8) {
                    Text("+ ${score.factors.size - 8} more factor(s)", fontSize = 11.sp, color = Muted)
                }
            }
        }
    }
}

@Composable
private fun PhotoPreview(bitmap: android.graphics.Bitmap, faces: List<FaceRegion>) {
    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
        val height = maxWidth / ratio
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .padding(0.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            Canvas(Modifier.fillMaxSize()) {
                val scale = min(size.width / bitmap.width, size.height / bitmap.height)
                val dx = (size.width - bitmap.width * scale) / 2f
                val dy = (size.height - bitmap.height * scale) / 2f
                faces.forEach { face ->
                    val color = if (face.selected) Accent else Color.White.copy(alpha = 0.85f)
                    drawRect(
                        color = color,
                        topLeft = Offset(dx + face.rect.left * scale, dy + face.rect.top * scale),
                        size = Size(face.rect.width * scale, face.rect.height * scale),
                        style = Stroke(width = 3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FaceChips(faces: List<FaceRegion>, onToggle: (Int) -> Unit) {
    if (faces.isEmpty()) {
        Text("No faces detected.", fontSize = 13.sp, color = Muted)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Faces: tap to blur", fontSize = 12.sp, color = Muted)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(faces) { face ->
                FilterChip(
                    selected = face.selected,
                    onClick = { onToggle(face.id) },
                    label = { Text("Face ${face.id + 1}") }
                )
            }
        }
    }
}

@Composable
private fun MetadataSection(metadata: PhotoMetadata) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Hidden metadata", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (metadata.entries.isEmpty()) {
                Text("No EXIF or JPEG metadata found.", fontSize = 13.sp, color = Muted)
            } else {
                val known = metadata.entries.take(40)
                val grouped = known.groupBy { it.category }
                for ((category, entries) in grouped) {
                    Text(
                        category.name.lowercase().replace('_', ' '),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Accent
                    )
                    entries.forEach { entry ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(entry.label, fontSize = 13.sp, color = Ink.copy(alpha = 0.7f))
                            Text(
                                entry.value,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Ink
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptionEditor(caption: String, findings: List<CaptionFinding>, onCaptionChange: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Caption check", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            OutlinedTextField(
                value = caption,
                onValueChange = onCaptionChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste the caption you plan to post\u2026") },
                minLines = 3,
                maxLines = 6
            )
            if (findings.isNotEmpty()) {
                val annotated = buildAnnotatedString {
                    append(caption)
                    findings.forEach { finding ->
                        val start = finding.range.first.coerceIn(0, caption.length)
                        val end = (finding.range.last + 1).coerceIn(start, caption.length)
                        addStyle(
                            SpanStyle(background = Accent.copy(alpha = 0.22f), color = Ink),
                            start,
                            end
                        )
                    }
                }
                Text(annotated, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                Text(
                    findings.joinToString("  \u00b7  ") {
                        it.kind.name.lowercase().replace('_', ' ')
                    },
                    fontSize = 11.sp,
                    color = Accent
                )
            } else if (caption.isNotBlank()) {
                Text("No personal details found in the caption.", fontSize = 12.sp, color = Muted)
            }
        }
    }
}
