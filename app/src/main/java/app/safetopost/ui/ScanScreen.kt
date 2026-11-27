package app.safetopost.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.safetopost.ScanPhase
import app.safetopost.ScanState
import app.safetopost.ScanViewModel
import app.safetopost.UiAction
import app.safetopost.model.CaptionFinding
import app.safetopost.model.CaptionKind
import app.safetopost.model.FaceRegion
import app.safetopost.model.LeakCategory
import app.safetopost.model.MetadataEntry
import app.safetopost.model.PhotoMetadata
import app.safetopost.model.Risk
import app.safetopost.model.RiskFactor
import app.safetopost.model.RiskLevel
import app.safetopost.model.RiskScore
import app.safetopost.photo.LoadedPhoto
import app.safetopost.risk.RiskScorer
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    palette: AppPalette,
    onPaletteSelected: (AppPalette) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val batch by viewModel.batch.collectAsState()
    var showThemes by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::load)
    }
    val pickPhoto: () -> Unit = {
        picker.launch("image/*")
    }
    val batchPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) viewModel.loadBatch(uris)
    }

    LaunchedEffect(state.message?.id) {
        val message = state.message ?: return@LaunchedEffect
        val exported = state.lastExport
        val actionLabel = when (message.action) {
            UiAction.SHARE -> if (exported != null) "Share" else null
            UiAction.UNDO_CAPTION -> "Undo"
            null -> null
        }
        val result = snackbar.showSnackbar(
            message = message.text,
            actionLabel = actionLabel,
            withDismissAction = true,
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            when (message.action) {
                UiAction.SHARE -> if (exported != null) shareExport(context, exported)
                UiAction.UNDO_CAPTION -> viewModel.undoCaptionClean()
                null -> Unit
            }
        }
        viewModel.clearMessage()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Header(
                canAdd = state.photo == null || state.phase == ScanPhase.READY,
                onAdd = pickPhoto,
                onBatch = { batchPicker.launch("image/*") },
                palette = palette,
                onThemeClick = { showThemes = true }
            )
        },
        bottomBar = {
            if (batch.total == 0 && state.photo != null) {
                ActionBar(
                    phase = state.phase,
                    onExport = viewModel::export,
                    onPick = pickPhoto
                )
            }
        }
    ) { padding ->
        if (batch.total > 0 || batch.items.isNotEmpty()) {
            BatchScreen(
                modifier = Modifier.padding(padding),
                batch = batch,
                onToggle = viewModel::toggleBatchItem,
                onCleanOne = viewModel::cleanBatchItem,
                onToggleBlur = viewModel::setBatchBlurFaces,
                onClean = viewModel::cleanBatch,
                onClose = viewModel::clearBatch
            )
        } else if (state.photo == null) {
            EmptyState(
                modifier = Modifier.padding(padding),
                onPick = pickPhoto,
                caption = state.caption
            )
        } else {
            ScanContent(
                modifier = Modifier.padding(padding),
                state = state,
                onToggleFace = viewModel::toggleFace,
                onCaptionChange = viewModel::updateCaption,
                onCleanCaption = viewModel::cleanCaption,
                onPick = pickPhoto
            )
        }
    }

    if (showThemes) {
        ThemeSheet(
            selected = palette,
            onSelect = {
                showThemes = false
                onPaletteSelected(it)
            },
            onDismiss = { showThemes = false }
        )
    }
}

/* ---------------------------------------------------------------- header */

@Composable
private fun Header(
    canAdd: Boolean,
    onAdd: () -> Unit,
    onBatch: () -> Unit,
    palette: AppPalette,
    onThemeClick: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Safe to Post",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "A LAST LOOK BEFORE YOU POST",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onBatch,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        "Batch",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                ThemeButton(palette = palette, onClick = onThemeClick)
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = onAdd,
                    enabled = canAdd,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Choose another photo")
                }
            }
        }
    }
}

@Composable
private fun ThemeButton(palette: AppPalette, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = palette.accent,
        border = BorderStroke(2.dp, palette.ink.copy(alpha = 0.18f)),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "Aa",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSheet(
    selected: AppPalette,
    onSelect: (AppPalette) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Theme", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Pick a palette. The new colour sweeps across the screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            AppPalette.entries.forEach { entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onSelect(entry) }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .background(entry.accent, CircleShape)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            entry.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Background, accent and ink",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (entry == selected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------ empty state */

@Composable
private fun EmptyState(modifier: Modifier, onPick: () -> Unit, caption: String = "") {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            "Three things can follow a photo you post.",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "The file carries metadata, the frame carries faces, and the caption carries details. Check all three on this phone before you share.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                StepRow("1", "Choose a photo", "From the gallery, or share it here from another app.")
                StepRow("2", "See what it reveals", "GPS, capture time, device, faces and caption details.")
                StepRow("3", "Clean it and export", "One tap writes a copy with no metadata and the faces you picked blurred.")
            }
        }
        Button(
            onClick = onPick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Choose a photo", style = MaterialTheme.typography.labelLarge)
        }
        if (caption.isNotBlank()) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "FROM THE KEYBOARD",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Choose the photo you were about to post; the caption stays with it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(
            "No account. No upload. Nothing leaves the device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StepRow(number: String, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/* --------------------------------------------------------------- content */

@Composable
private fun ScanContent(
    modifier: Modifier,
    state: ScanState,
    onToggleFace: (Int) -> Unit,
    onCaptionChange: (String) -> Unit,
    onCleanCaption: () -> Unit,
    onPick: () -> Unit
) {
    val photo = state.photo ?: return
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { VerdictCard(state) }
        item { PhotoCard(photo, state.faces) }
        item {
            FacesCard(
                faces = state.faces,
                onToggle = onToggleFace
            )
        }
        item { MetadataCard(state.metadata) }
        item {
            CaptionCard(
                caption = state.caption,
                findings = state.findings,
                onChange = onCaptionChange,
                onClean = onCleanCaption
            )
        }
        item {
            Text(
                "Analysis stays on this device. Export writes a new file; the original is untouched.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }
    }
}

/* ------------------------------------------------------------- verdict */

@Composable
private fun VerdictCard(state: ScanState) {
    val score = state.score
    var expanded by rememberSaveable(state.photo?.name) { mutableStateOf(false) }
    val level = score.level
    val headline = when (level) {
        RiskLevel.HIGH -> "This one gives away a lot."
        RiskLevel.MEDIUM -> "A few details are visible."
        RiskLevel.LOW -> "Looks fairly clean."
    }
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScoreRing(score)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        when (level) {
                            RiskLevel.HIGH -> "HIGH RISK"
                            RiskLevel.MEDIUM -> "MEDIUM RISK"
                            RiskLevel.LOW -> "LOW RISK"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = riskColor(level)
                    )
                    Text(headline, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        summary(state.metadata, state.findings),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (score.factors.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Text("WHAT COUNTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val shown = if (expanded) score.factors else score.factors.take(3)
                shown.forEach { FactorRow(it) }
                if (score.factors.size > 3) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            if (expanded) "Show less" else "Show all ${score.factors.size} signals",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreRing(score: RiskScore) {
    val color = riskColor(score.level)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * (score.total.coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${score.total}",
                fontSize = 30.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text("/ 100", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FactorRow(factor: RiskFactor) {
    val dot = when {
        factor.weight >= 25 -> HighRisk
        factor.weight >= 10 -> MediumRisk
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(dot, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            factor.explanation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "+${factor.weight}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun summary(metadata: PhotoMetadata, findings: List<CaptionFinding>): String {
    val metaBits = mutableListOf<String>()
    if (metadata.hasGps) metaBits += "location"
    if (metadata.captureTime != null) metaBits += "capture time"
    if (metadata.entries.any { it.category == LeakCategory.DEVICE }) metaBits += "device details"
    if (metadata.entries.any { it.category == LeakCategory.IDENTITY }) metaBits += "identifying fields"

    val captionBits = findings.map { kindLabel(it.kind) }.distinct()
    val metaPart = if (metaBits.isEmpty()) "No file metadata found" else "The file reveals " + metaBits.joinToString(", ")
    val captionPart = if (captionBits.isEmpty()) "the caption is clean" else "the caption names a " + captionBits.joinToString(" and ")
    return "$metaPart; $captionPart."
}

/* --------------------------------------------------------------- photo */

@Composable
private fun PhotoCard(photo: LoadedPhoto, faces: List<FaceRegion>) {
    val bitmap = photo.bitmap
    val accent = MaterialTheme.colorScheme.primary
    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val height = (maxWidth / ratio).coerceAtMost(360.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Selected photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Canvas(Modifier.fillMaxSize()) {
                    val scale = min(size.width / bitmap.width, size.height / bitmap.height)
                    val dx = (size.width - bitmap.width * scale) / 2f
                    val dy = (size.height - bitmap.height * scale) / 2f
                    faces.forEach { face ->
                        val color = if (face.selected) accent else Color.White.copy(alpha = 0.9f)
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
}

/* ---------------------------------------------------------------- faces */

@Composable
private fun FacesCard(faces: List<FaceRegion>, onToggle: (Int) -> Unit) {
    val selected = faces.count { it.selected }
    SectionCard(
        title = "People",
        meta = if (faces.isEmpty()) null else "$selected of ${faces.size} to blur"
    ) {
        if (faces.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "No faces detected. The export still removes metadata.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(faces) { face ->
                    FilterChip(
                        selected = face.selected,
                        onClick = { onToggle(face.id) },
                        label = { Text("Face ${face.id + 1}") },
                        leadingIcon = if (face.selected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { faces.filterNot { it.selected }.forEach { onToggle(it.id) } }) {
                    Text("Select all", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = { faces.filter { it.selected }.forEach { onToggle(it.id) } }) {
                    Text("Clear", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/* ------------------------------------------------------------- metadata */

@Composable
private fun MetadataCard(metadata: PhotoMetadata) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    SectionCard(
        title = "What the file reveals",
        meta = if (metadata.entries.isEmpty()) null else "${metadata.entries.size} fields"
    ) {
        if (metadata.entries.isEmpty()) {
            Text(
                "No EXIF or JPEG metadata found in this file.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val order = listOf(
                LeakCategory.LOCATION,
                LeakCategory.IDENTITY,
                LeakCategory.TIME,
                LeakCategory.DEVICE,
                LeakCategory.SOCIAL,
                LeakCategory.OTHER
            )
            val sorted = metadata.entries.sortedBy { order.indexOf(it.category) }
            val visible = if (expanded) sorted else sorted.take(6)
            var lastCategory: LeakCategory? = null
            visible.forEach { entry ->
                if (entry.category != lastCategory) {
                    lastCategory = entry.category
                    Text(
                        entry.category.name.lowercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                MetadataRow(entry)
            }
            if (metadata.entries.size > 6) {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        if (expanded) "Show fewer fields" else "Show all ${metadata.entries.size} fields",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(entry: MetadataEntry) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(riskColor(entry.risk), CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            entry.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.weight(1f)
        )
        Text(
            entry.value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

/* -------------------------------------------------------------- caption */

@Composable
private fun CaptionCard(
    caption: String,
    findings: List<CaptionFinding>,
    onChange: (String) -> Unit,
    onClean: () -> Unit
) {
    SectionCard(title = "Caption check", meta = if (caption.isBlank()) null else "${caption.length} chars") {
        OutlinedTextField(
            value = caption,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Paste or type the caption you plan to post\u2026", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            minLines = 3,
            maxLines = 6,
            shape = MaterialTheme.shapes.medium,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
            )
        )
        if (findings.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(findings.distinctBy { it.kind }) { finding ->
                    Pill("${kindLabel(finding.kind)} +${RiskScorer.captionWeight(finding.kind)}")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onClean,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "Clean caption",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "replaces details with [phone] / [address]",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val annotated = buildAnnotatedString {
                append(caption)
                findings.forEach { finding ->
                    val start = finding.range.first.coerceIn(0, caption.length)
                    val end = (finding.range.last + 1).coerceIn(start, caption.length)
                    addStyle(SpanStyle(background = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), color = MaterialTheme.colorScheme.onSurface), start, end)
                }
            }
            Text(annotated, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        } else if (caption.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = LowRisk, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "No personal details found in this caption.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LowRisk
                )
            }
        } else {
            Text(
                "Phone numbers, e-mail, addresses, dates and school or work names are checked as you type.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun kindLabel(kind: CaptionKind): String = when (kind) {
    CaptionKind.PHONE -> "phone"
    CaptionKind.EMAIL -> "e-mail"
    CaptionKind.ADDRESS -> "address"
    CaptionKind.DATE -> "date"
    CaptionKind.SCHOOL_OR_WORK -> "school/work"
    CaptionKind.HANDLE -> "handle"
    CaptionKind.LINK -> "link"
}

@Composable
private fun Pill(text: String, color: Color = Color.Unspecified) {
    val pillColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    Surface(
        shape = RoundedCornerShape(50),
        color = pillColor.copy(alpha = 0.12f)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = pillColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/* --------------------------------------------------------------- bottom */

@Composable
private fun ActionBar(phase: ScanPhase, onExport: () -> Unit, onPick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
        Row(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onExport,
                enabled = phase == ScanPhase.READY,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (phase == ScanPhase.EXPORTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Clean & export", style = MaterialTheme.typography.labelLarge)
                }
            }
            OutlinedButton(
                onClick = onPick,
                modifier = Modifier
                    .height(52.dp)
                    .weight(0.42f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text("New", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    meta: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                if (meta != null) {
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

private fun shareExport(context: Context, uri: Uri) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share cleaned photo"))
}
