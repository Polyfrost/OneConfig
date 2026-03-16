package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.runtime.PolyComposeRuntime
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.IconButton
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.layout.FlexibleLayout
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.Designer
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.Settings
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

enum class StudioCategory(val title: String, val icon: String) {
    Designer("Designer", "paintbrush"),
    Settings("HUD Settings", "qol");
}

enum class EditorMode(val title: String, val icon: String) {
    Design("Design", "paintbrush"),
    Stage("Stage", "move"),
}

private fun hitTestHud(hud: Hud, x: Float, y: Float): Boolean {
    val w = if (hud.staticWidth) hud.staticW else hud.renderedW.takeIf { it > 0f } ?: return false
    val h = if (hud.staticWidth) hud.staticH else hud.renderedH.takeIf { it > 0f } ?: return false
    return x >= hud.x && x <= hud.x + w && y >= hud.y && y <= hud.y + h
}

private val panelBackground = Color(17, 23, 28).copy(0.95f)
private val panelBorder = Color.White.copy(.10f)
private val panelShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HudDesignStudio() {
    var editorMode by remember { mutableStateOf(EditorMode.Design) }
    var activeCategory by remember { mutableStateOf(StudioCategory.Designer) }
    var selectedHud by remember { mutableStateOf<Hud?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var libraryVisible by remember { mutableStateOf(false) }
    var filterModId by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }

    val providers = remember { HudManager.providers().toList() }
    val modIds = remember(providers) { providers.mapNotNull { it.configId }.distinct() }
    val filteredProviders = providers.filter { hud ->
        (filterModId == null || hud.configId == filterModId) &&
            (searchText.isEmpty() || hud.title?.contains(searchText, ignoreCase = true) == true)
    }

    LaunchedEffect(editorMode) {
        Snapshot.withMutableSnapshot {
            isDragging = false
            selectedHud = null
            if (editorMode == EditorMode.Design) {
                libraryVisible = false
                filterModId = null
            }
        }
    }

    val stagePointerModifier = Modifier
        .onPointerEvent(PointerEventType.Press) { event ->
            if (event.changes.any { it.isConsumed }) return@onPointerEvent
            val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
            val hit = HudManager.activeInstances.lastOrNull { hitTestHud(it, pos.x, pos.y) }
            Snapshot.withMutableSnapshot {
                selectedHud = hit
                if (hit != null) {
                    dragOffsetX = pos.x - hit.x
                    dragOffsetY = pos.y - hit.y
                    isDragging = true
                }
            }
        }
        .onPointerEvent(PointerEventType.Move) { event ->
            if (isDragging) {
                if (event.changes.none { it.pressed }) {
                    Snapshot.withMutableSnapshot { isDragging = false; selectedHud = null }
                    return@onPointerEvent
                }
                val hud = selectedHud ?: return@onPointerEvent
                val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                Snapshot.withMutableSnapshot {
                    hud.x = pos.x - dragOffsetX
                    hud.y = pos.y - dragOffsetY
                }
            }
        }
        .onPointerEvent(PointerEventType.Release) {
            Snapshot.withMutableSnapshot { isDragging = false; selectedHud = null }
        }

    val designPointerModifier = Modifier
        .onPointerEvent(PointerEventType.Press) { event ->
            if (event.changes.any { it.isConsumed }) return@onPointerEvent
            val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
            val hit = HudManager.activeInstances.lastOrNull { hitTestHud(it, pos.x, pos.y) }
            Snapshot.withMutableSnapshot { selectedHud = hit }
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (editorMode == EditorMode.Stage) stagePointerModifier else designPointerModifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    for (hud in HudManager.activeInstances) {
                        val w = if (hud.staticWidth) hud.staticW else hud.renderedW.takeIf { it > 0f } ?: continue
                        val h = if (hud.staticWidth) hud.staticH else hud.renderedH.takeIf { it > 0f } ?: continue
                        val isSelected = hud === selectedHud
                        val strokeColor = if (isSelected) Color(0xFF4A90E2) else Color.White.copy(0.25f)
                        val strokeWidth = if (isSelected) 2f else 1f
                        val pad = 3f
                        drawRect(
                            color = strokeColor,
                            topLeft = Offset(hud.x - pad, hud.y - pad),
                            size = Size(w + pad * 2, h + pad * 2),
                            style = Stroke(width = strokeWidth)
                        )
                        if (isSelected) {
                            drawRect(
                                color = Color(0xFF4A90E2).copy(alpha = 0.08f),
                                topLeft = Offset(hud.x - pad, hud.y - pad),
                                size = Size(w + pad * 2, h + pad * 2),
                            )
                        }
                    }
                }
        )

        Row(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EditorMode.entries.forEach { mode ->
                Chip(mode.title, mode == editorMode, mode.icon) {
                    Snapshot.withMutableSnapshot {
                        editorMode = mode
                        selectedHud = null
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = editorMode == EditorMode.Design && selectedHud != null,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        ) {
            DesignStudioPanel(
                selectedHud = selectedHud,
                activeCategory = activeCategory,
                onCategoryChange = { activeCategory = it },
                onBack = { Snapshot.withMutableSnapshot { selectedHud = null } }
            )
        }

        if (editorMode == EditorMode.Stage && modIds.isNotEmpty()) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AnimatedVisibility(
                    visible = libraryVisible,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                ) {
                    HudLibraryPanel(
                        searchText = searchText,
                        onSearchChange = { searchText = it },
                        filteredProviders = filteredProviders,
                    )
                }
                ModIconColumn(
                    modIds = modIds,
                    filterModId = filterModId,
                    libraryVisible = libraryVisible,
                ) { modId ->
                    Snapshot.withMutableSnapshot {
                        if (filterModId == modId && libraryVisible) {
                            libraryVisible = false
                        } else {
                            filterModId = modId
                            libraryVisible = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesignStudioPanel(
    selectedHud: Hud?,
    activeCategory: StudioCategory,
    onCategoryChange: (StudioCategory) -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(500.dp)
            .padding(16.dp)
            .background(panelBackground, panelShape)
            .border(1.dp, panelBorder, panelShape),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton("left-arrow") { onBack() }
                    SearchBar()
                }
                Text("HUD Design Studio", color = LocalTheme.current.textColor, fontSize = 24.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudioCategory.entries.forEach {
                        Chip(it.title, it == activeCategory, it.icon) { onCategoryChange(it) }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (activeCategory) {
                        StudioCategory.Designer -> Designer(selectedHud)
                        StudioCategory.Settings -> Settings(selectedHud)
                    }
                }
            }
        }
    }
}

@Composable
private fun HudLibraryPanel(
    searchText: String,
    onSearchChange: (String) -> Unit,
    filteredProviders: List<Hud>,
) {
    Column(
        modifier = Modifier
            .size(401.dp, 481.dp)
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
            .background(panelBackground, panelShape)
            .border(1.dp, panelBorder, panelShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("HUDs", color = LocalTheme.current.textColor, fontSize = 18.sp)
            LibrarySearchBar(searchText, onSearchChange)
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
        ) {
            FlexibleLayout(
                horizontalSpacing = 16.dp,
                verticalSpacing = 16.dp
            ) {
                filteredProviders.forEach { hud ->
                    HudPreviewCard(hud)
                }
            }
        }
    }
}

@Composable
private fun ModIconColumn(
    modIds: List<String>,
    filterModId: String?,
    libraryVisible: Boolean,
    onModClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(end = 16.dp, top = 16.dp, bottom = 16.dp)
            .background(panelBackground, panelShape)
            .border(1.dp, panelBorder, panelShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        modIds.forEach { modId ->
            val config = ConfigRegistry.findById(modId)
            ModFilterIcon(
                iconName = config?.icon ?: "qol",
                selected = filterModId == modId && libraryVisible,
            ) { onModClick(modId) }
        }
    }
}

private const val PREVIEW_SCALE = 2f

@Composable
private fun HudPreviewCard(hud: Hud) {
    val previewRuntime = remember(hud) {
        PolyComposeRuntime().also { rt -> rt.setContent { hud.Content() } }
    }
    var naturalW by remember(hud) { mutableStateOf(0f) }
    var naturalH by remember(hud) { mutableStateOf(0f) }
    val density = LocalDensity.current.density
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        if (isHovered) 1.05f else 1f
    )

    LaunchedEffect(hud) {
        hud.update()
        previewRuntime.frame(2000f, 2000f)
        naturalW = previewRuntime.root.width
        naturalH = previewRuntime.root.height
    }

    if (naturalW > 0f && naturalH > 0f) {
        val w = (naturalW * PREVIEW_SCALE / density).dp
        val h = (naturalH * PREVIEW_SCALE / density).dp
        Canvas(
            modifier = Modifier
                .size(w, h)
                .scale(scale)
                .onClick(interactionSource) {
                    try {
                        val instance = hud.make()
                        val (dx, dy) = hud.defaultPosition()
                        instance.x = if (dx > 0f) dx else 200f
                        instance.y = if (dy > 0f) dy else 200f
                        if (instance !in HudManager.activeInstances) {
                            HudManager.activeInstances.add(instance)
                            instance.setup()
                        }
                    } catch (_: Throwable) {}
                }
        ) {
            drawIntoCanvas { canvas ->
                val root = previewRuntime.root
                canvas.nativeCanvas.save()
                canvas.nativeCanvas.scale(PREVIEW_SCALE, PREVIEW_SCALE)
                root.render(RenderContext(canvas.nativeCanvas))
                canvas.nativeCanvas.restore()
            }
        }
    }
}

@Composable
private fun ModFilterIcon(iconName: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val iconColor by animateColorAsState(
        if (selected) Color.White.copy(1f)
        else if (isHovered) Color.White.copy(0.8f)
        else Color.White.copy(0.7f)
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .onClick(interactionSource) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(iconName, modifier = Modifier.size(32.dp), color = iconColor)
    }
}

@Composable
fun SearchBar() {
    var searchText by remember { mutableStateOf("") }
    val interactionSource = rememberInteractionSource()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        if (isFocused) Color.White.copy(.20f) else Color.White.copy(.10f)
    )
    val iconColor by animateColorAsState(
        if (isFocused) Color(223, 234, 255).copy(0.70f) else Color(223, 234, 255).copy(0.50f)
    )

    BasicTextField(
        searchText,
        { searchText = it },
        interactionSource = interactionSource,
        textStyle = TextStyle(
            color = iconColor, fontSize = 12.sp,
            fontFamily = LocalTheme.current.typography.family
        ),
        cursorBrush = SolidColor(iconColor),
    ) { innerTextField ->
        Row(
            modifier = Modifier.size(181.dp, 32.dp)
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .background(Color(35, 45, 50).copy(0.95f), RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon("search", color = iconColor, modifier = Modifier.padding(start = 8.dp).size(12.dp))
            Box {
                if (!isFocused && searchText.isEmpty())
                    Text("Search ...", color = iconColor, fontSize = 12.sp)
                innerTextField()
            }
        }
    }
}

@Composable
private fun LibrarySearchBar(value: String, onValueChange: (String) -> Unit) {
    val interactionSource = rememberInteractionSource()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        if (isFocused) Color.White.copy(.20f) else Color.White.copy(.10f)
    )
    val iconColor by animateColorAsState(
        if (isFocused) Color(223, 234, 255).copy(0.70f) else Color(223, 234, 255).copy(0.50f)
    )

    BasicTextField(
        value,
        onValueChange,
        interactionSource = interactionSource,
        textStyle = TextStyle(
            color = iconColor, fontSize = 12.sp,
            fontFamily = LocalTheme.current.typography.family
        ),
        cursorBrush = SolidColor(iconColor)
    ) { innerTextField ->
        Row(
            modifier = Modifier.width(181.dp).height(32.dp)
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .background(Color(35, 45, 50).copy(0.95f), RoundedCornerShape(8.dp)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon("search", color = iconColor, modifier = Modifier.padding(start = 8.dp).size(12.dp))
            Box {
                if (!isFocused && value.isEmpty())
                    Text("Search HUDs...", color = iconColor, fontSize = 12.sp)
                innerTextField()
            }
        }
    }
}
