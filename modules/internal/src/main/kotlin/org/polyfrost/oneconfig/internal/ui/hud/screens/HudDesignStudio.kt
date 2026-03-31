package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.VerticalScrollbar
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
import androidx.compose.foundation.rememberScrollbarAdapter
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.runtime.PolyComposeRuntime
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud
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
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.themes.Accent

enum class StudioCategory(val title: String, val icon: String) {
    Designer("Designer", "paintbrush"),
    Settings("HUD Settings", "qol");
}

private fun hitTestHud(hud: Hud, screenX: Float, screenY: Float): Boolean {
    val s = Platform.screen().screenToMcScale()
    val mcX = screenX * s
    val mcY = screenY * s
    val scale = hud.effectiveScale
    val rawW = if (hud.staticWidth) hud.staticW else hud.renderedW.takeIf { it > 0f } ?: hud.staticW.takeIf { it > 0f } ?: return false
    val rawH = if (hud.staticWidth) hud.staticH else hud.renderedH.takeIf { it > 0f } ?: hud.staticH.takeIf { it > 0f } ?: return false
    val w = rawW * scale
    val h = rawH * scale
    return mcX >= hud.x && mcX <= hud.x + w && mcY >= hud.y && mcY <= hud.y + h
}

private fun hitTestGear(hud: Hud, screenX: Float, screenY: Float, mcToScreen: Float, iconPx: Float): Boolean {
    val scale = hud.effectiveScale
    val w = (if (hud.staticWidth) hud.staticW else hud.renderedW.takeIf { it > 0f } ?: return false) * scale
    val h = (if (hud.staticWidth) hud.staticH else hud.renderedH.takeIf { it > 0f } ?: return false) * scale
    val sx = hud.x * mcToScreen
    val sy = hud.y * mcToScreen
    val sw = w * mcToScreen
    val sh = h * mcToScreen
    val iconX = sx + sw / 2 - iconPx / 2
    val iconY = sy + sh + 6f
    return screenX >= iconX && screenX <= iconX + iconPx && screenY >= iconY && screenY <= iconPx + iconY
}

private fun hitTestHudWithGear(hud: Hud, screenX: Float, screenY: Float, mcToScreen: Float, iconPx: Float): Boolean {
    val scale = hud.effectiveScale
    val w = (if (hud.staticWidth) hud.staticW else hud.renderedW.takeIf { it > 0f } ?: return false) * scale
    val h = (if (hud.staticWidth) hud.staticH else hud.renderedH.takeIf { it > 0f } ?: return false) * scale
    val sx = hud.x * mcToScreen
    val sy = hud.y * mcToScreen
    val sw = w * mcToScreen
    val sh = h * mcToScreen
    val totalH = sh + 6f + iconPx
    return screenX >= sx && screenX <= sx + sw && screenY >= sy && screenY <= sy + totalH
}

private val panelBackground = Color(17, 23, 28).copy(0.95f)
private val panelBorder = Color.White.copy(.10f)
private val panelShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HudDesignStudio() {
    var activeCategory by remember { mutableStateOf(StudioCategory.Designer) }
    var selectedHud by remember { mutableStateOf<Hud?>(null) }
    var hoveredHud by remember { mutableStateOf<Hud?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var draggedHud by remember { mutableStateOf<Hud?>(null) }
    var libraryVisible by remember { mutableStateOf(false) }
    var filterModId by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }
    var panelAreaWidth by remember { mutableStateOf(0f) }

    val providers = remember { HudManager.providers().filter { it !is LegacyHud }.toList() }
    val modIds = remember(providers) { providers.mapNotNull { it.configId }.distinct() }
    val filteredProviders = providers.filter { hud ->
        (filterModId == null || hud.configId == filterModId) &&
            (searchText.isEmpty() || hud.title?.contains(searchText, ignoreCase = true) == true)
    }
    val densityObj = LocalDensity.current
    val densityFloat = densityObj.density
    val gearIconPx = with(densityObj) { 24.dp.toPx() }

    // Unified pointer modifier: drag any HUD, click to select, hover to show gear
    val pointerModifier = Modifier
        .onPointerEvent(PointerEventType.Press) { event ->
            if (event.changes.any { it.isConsumed }) return@onPointerEvent
            val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
            if (panelAreaWidth > 0f && pos.x > size.width - panelAreaWidth) return@onPointerEvent
            val currentGearTarget = selectedHud ?: hoveredHud
            if (currentGearTarget != null && currentGearTarget !is LegacyHud) {
                val mcToScreen = Platform.screen().mcToScreenScale()
                if (hitTestGear(currentGearTarget, pos.x, pos.y, mcToScreen, gearIconPx)) return@onPointerEvent
            }
            val s = Platform.screen().screenToMcScale()
            val hit = HudManager.activeInstances.lastOrNull { it !is LegacyHud && hitTestHud(it, pos.x, pos.y) }
            Snapshot.withMutableSnapshot {
                if (hit != null) {
                    dragOffsetX = pos.x * s - hit.x
                    dragOffsetY = pos.y * s - hit.y
                    isDragging = true
                    draggedHud = hit
                    hoveredHud = hit
                    libraryVisible = false
                } else {
                    selectedHud = null
                }
            }
        }
        .onPointerEvent(PointerEventType.Move) { event ->
            val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
            if (isDragging) {
                if (event.changes.none { it.pressed }) {
                    Snapshot.withMutableSnapshot {
                        isDragging = false
                        draggedHud = null
                    }
                    return@onPointerEvent
                }
                val s = Platform.screen().screenToMcScale()
                val hit = draggedHud ?: return@onPointerEvent
                Snapshot.withMutableSnapshot {
                    hit.setAbsolutePosition(pos.x * s - dragOffsetX, pos.y * s - dragOffsetY)
                }
            } else {
                val hit = HudManager.activeInstances.lastOrNull { it !is LegacyHud && hitTestHud(it, pos.x, pos.y) }
                val mcToScreen = Platform.screen().mcToScreenScale()
                val overGear = hoveredHud?.let { hh ->
                    hh !is LegacyHud && hitTestGear(hh, pos.x, pos.y, mcToScreen, gearIconPx)
                } == true
                val inExpandedZone = hoveredHud?.let { hh ->
                    hh !is LegacyHud && hitTestHudWithGear(hh, pos.x, pos.y, mcToScreen, gearIconPx)
                } == true
                if (hit != null) {
                    hoveredHud = hit
                } else if (!overGear && !inExpandedZone) {
                    hoveredHud = null
                }
            }
        }
        .onPointerEvent(PointerEventType.Release) { event ->
            val wasDragging = isDragging
            val wasDraggedHud = draggedHud
            Snapshot.withMutableSnapshot {
                isDragging = false
                draggedHud = null
            }
            if (!wasDragging || wasDraggedHud == null) {
                val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                val currentGearTarget = selectedHud ?: hoveredHud
                if (currentGearTarget != null && currentGearTarget !is LegacyHud) {
                    val mcToScreen = Platform.screen().mcToScreenScale()
                    if (hitTestGear(currentGearTarget, pos.x, pos.y, mcToScreen, gearIconPx)) return@onPointerEvent
                }
                val hit = HudManager.activeInstances.lastOrNull { it !is LegacyHud && hitTestHud(it, pos.x, pos.y) }
                if (hit != null) {
                    Snapshot.withMutableSnapshot {
                        selectedHud = hit
                        libraryVisible = false
                    }
                }
            } else {
                Snapshot.withMutableSnapshot {
                    selectedHud = wasDraggedHud
                    libraryVisible = false
                }
            }
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(pointerModifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    val mcToScreen = Platform.screen().mcToScreenScale()
                    for (hud in HudManager.activeInstances) {
                        val scale = hud.effectiveScale
                        val w = (if (hud.staticWidth) hud.staticW else hud.renderedW.takeIf { it > 0f } ?: continue) * scale
                        val h = (if (hud.staticWidth) hud.staticH else hud.renderedH.takeIf { it > 0f } ?: continue) * scale
                        val sx = hud.x * mcToScreen
                        val sy = hud.y * mcToScreen
                        val sw = w * mcToScreen
                        val sh = h * mcToScreen
                        val isSelected = hud === selectedHud
                        val isHovered = hud === hoveredHud
                        val isBeingDragged = hud === draggedHud && isDragging

                        if (isBeingDragged) {
                            val glowPad = 6f
                            val cr = CornerRadius(6f, 6f)
                            drawRoundRect(
                                color = Color(0xFF4A90E2).copy(alpha = 0.12f),
                                topLeft = Offset(sx - glowPad, sy - glowPad),
                                size = Size(sw + glowPad * 2, sh + glowPad * 2),
                                cornerRadius = cr,
                            )
                            drawRoundRect(
                                color = Color(0xFF4A90E2).copy(alpha = 0.85f),
                                topLeft = Offset(sx - glowPad, sy - glowPad),
                                size = Size(sw + glowPad * 2, sh + glowPad * 2),
                                cornerRadius = cr,
                                style = Stroke(width = 2.5f)
                            )
                        } else {
                            val strokeColor = when {
                                isSelected -> Color(0xFF4A90E2)
                                isHovered -> Color.White.copy(0.5f)
                                else -> Color.White.copy(0.25f)
                            }
                            val strokeWidth = if (isSelected) 2f else 1f
                            val pad = 3f
                            drawRect(
                                color = strokeColor,
                                topLeft = Offset(sx - pad, sy - pad),
                                size = Size(sw + pad * 2, sh + pad * 2),
                                style = Stroke(width = strokeWidth)
                            )
                            if (isSelected) {
                                drawRect(
                                    color = Color(0xFF4A90E2).copy(alpha = 0.08f),
                                    topLeft = Offset(sx - pad, sy - pad),
                                    size = Size(sw + pad * 2, sh + pad * 2),
                                )
                            }
                        }
                    }
                }
        )

        val gearTarget = if (selectedHud != null) selectedHud else hoveredHud
        if (gearTarget != null && gearTarget !is LegacyHud) {
            val mcToScreen = Platform.screen().mcToScreenScale()
            val scale = gearTarget.effectiveScale
            val w = (if (gearTarget.staticWidth) gearTarget.staticW else gearTarget.renderedW) * scale
            val h = (if (gearTarget.staticWidth) gearTarget.staticH else gearTarget.renderedH) * scale
            if (w > 0f && h > 0f) {
                val sx = gearTarget.x * mcToScreen
                val sy = gearTarget.y * mcToScreen
                val sw = w * mcToScreen
                val sh = h * mcToScreen
                val iconSize = 24.dp
                val iconX = ((sx + sw / 2 - gearIconPx / 2) / densityFloat).coerceAtLeast(0f)
                val iconY = ((sy + sh + 6f) / densityFloat).coerceAtLeast(0f)
                Box(
                    modifier = Modifier
                        .padding(start = iconX.dp, top = iconY.dp)
                        .onPointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
                            if (event.changes.none { it.isConsumed }) {
                                event.changes.forEach { it.consume() }
                                Snapshot.withMutableSnapshot { selectedHud = gearTarget }
                            }
                        }
                        .onPointerEvent(PointerEventType.Move, PointerEventPass.Final) { event ->
                            event.changes.forEach { if (!it.isConsumed) it.consume() }
                        }
                ) {
                    IconButton("settings", modifier = Modifier.size(iconSize)) {
                        Snapshot.withMutableSnapshot { selectedHud = gearTarget }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedHud != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
                .onSizeChanged { panelAreaWidth = maxOf(panelAreaWidth, it.width.toFloat()) }
        ) {
            Box(
                modifier = Modifier
                    .onPointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
                        event.changes.forEach { if (!it.isConsumed) it.consume() }
                    }
                    .onPointerEvent(PointerEventType.Move, PointerEventPass.Final) { event ->
                        event.changes.forEach { if (!it.isConsumed) it.consume() }
                    }
                    .onPointerEvent(PointerEventType.Release, PointerEventPass.Final) { event ->
                        event.changes.forEach { if (!it.isConsumed) it.consume() }
                    }
            ) {
                DesignStudioPanel(
                    selectedHud = selectedHud,
                    activeCategory = activeCategory,
                    onCategoryChange = { activeCategory = it },
                    onBack = { Snapshot.withMutableSnapshot { selectedHud = null } }
                )
            }
        }

        if (modIds.isNotEmpty() && selectedHud == null) {
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
                        onDragStart = { hud, sx, sy, hudLocalOffX, hudLocalOffY ->
                            try {
                                val instance = hud.make()
                                val s = Platform.screen().screenToMcScale()
                                val effScale = instance.effectiveScale
                                val offX = hudLocalOffX * effScale
                                val offY = hudLocalOffY * effScale
                                instance.setAbsolutePosition(sx * s - offX, sy * s - offY)
                                if (instance !in HudManager.activeInstances) {
                                    HudManager.activeInstances.add(instance)
                                    instance.setup()
                                }
                                Snapshot.withMutableSnapshot {
                                    dragOffsetX = offX
                                    dragOffsetY = offY
                                    isDragging = true
                                    draggedHud = instance
                                    libraryVisible = false
                                }
                            } catch (_: Throwable) {}
                        },
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
                    IconButton("close") { onBack() }
                    SearchBar()
                }
                Text("HUD Design Studio", color = Color.White, fontSize = 24.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudioCategory.entries.forEach {
                        Chip(it.title, it == activeCategory, it.icon) { onCategoryChange(it) }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = activeCategory,
                    transitionSpec = {
                        val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (slideInHorizontally(tween(250)) { direction * it / 3 } + fadeIn(tween(250)))
                            .togetherWith(slideOutHorizontally(tween(250)) { -direction * it / 3 } + fadeOut(tween(250)))
                    },
                    modifier = Modifier.fillMaxSize()
                ) { category ->
                    when (category) {
                        StudioCategory.Designer -> {
                            val panelScrollState = rememberScrollState()
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                        .verticalScroll(panelScrollState)
                                        .padding(end = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Designer(selectedHud)
                                }
                                VerticalScrollbar(
                                    adapter = rememberScrollbarAdapter(panelScrollState),
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                                )
                            }
                        }
                        StudioCategory.Settings -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Settings(selectedHud)
                            }
                        }
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
    onDragStart: (Hud, Float, Float, Float, Float) -> Unit = { _, _, _, _, _ -> },
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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("HUDs", color = Color.White, fontSize = 18.sp)
            LibrarySearchBar(searchText, onSearchChange)
        }
        val libraryScrollState = rememberScrollState()
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .verticalScroll(libraryScrollState)
                    .padding(end = 8.dp),
            ) {
                FlexibleLayout(
                    horizontalSpacing = 10.dp,
                    verticalSpacing = 10.dp
                ) {
                    filteredProviders.forEach { hud ->
                        HudPreviewCard(hud, onDragStart)
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(libraryScrollState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
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
private fun HudPreviewCard(hud: Hud, onDragStart: (Hud, Float, Float, Float, Float) -> Unit) {
    ComposeHudPreviewCard(hud, onDragStart)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ComposeHudPreviewCard(hud: Hud, onDragStart: (Hud, Float, Float, Float, Float) -> Unit) {
    val previewRuntime = remember(hud) {
        PolyComposeRuntime().also { rt -> rt.setContent { hud.Content() } }
    }
    var naturalW by remember(hud) { mutableStateOf(0f) }
    var naturalH by remember(hud) { mutableStateOf(0f) }
    val density = LocalDensity.current.density

    // Hover state tracked via pointer Enter/Exit
    var isHovered by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(
        if (isHovered) Accent else panelBackground,
        animationSpec = tween(150)
    )

    var pressPos by remember { mutableStateOf<Offset?>(null) }
    var dragStarted by remember { mutableStateOf(false) }

    LaunchedEffect(hud) {
        hud.update()
        if (hud.staticWidth && hud.staticW > 0f && hud.staticH > 0f) {
            previewRuntime.frame(hud.staticW, hud.staticH)
            naturalW = hud.staticW
            naturalH = hud.staticH
        } else {
            previewRuntime.frame(2000f, 2000f)
            naturalW = previewRuntime.root.width
            naturalH = previewRuntime.root.height
        }
    }

    if (naturalW > 0f && naturalH > 0f) {
        val cardPadding = 12.dp
        val w = (naturalW * PREVIEW_SCALE / density).dp + cardPadding * 2
        val h = (naturalH * PREVIEW_SCALE / density).dp + cardPadding * 2
        Canvas(
            modifier = Modifier
                .size(w, h)
                .background(backgroundColor, RoundedCornerShape(8.dp))
                .border(1.dp, panelBorder, RoundedCornerShape(8.dp))
                .onPointerEvent(PointerEventType.Enter) { isHovered = true }
                .onPointerEvent(PointerEventType.Exit) {
                    isHovered = false
                    if (!dragStarted) pressPos = null
                }
                .onPointerEvent(PointerEventType.Press) { event ->
                    val pos = event.changes.firstOrNull()?.position
                    if (pos != null) {
                        pressPos = pos
                        dragStarted = false
                    }
                }
                .onPointerEvent(PointerEventType.Move) { event ->
                    val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                    val start = pressPos ?: return@onPointerEvent
                    if (!dragStarted && event.changes.any { it.pressed }) {
                        val dx = pos.x - start.x
                        val dy = pos.y - start.y
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                        if (dist > 8f) {
                            dragStarted = true
                            val paddingPx = 12f * density
                            val hudLocalX = ((start.x - paddingPx) / PREVIEW_SCALE).coerceAtLeast(0f)
                            val hudLocalY = ((start.y - paddingPx) / PREVIEW_SCALE).coerceAtLeast(0f)
                            onDragStart(hud, pos.x, pos.y, hudLocalX, hudLocalY)
                            pressPos = null
                            isHovered = false
                        }
                    }
                }
                .onPointerEvent(PointerEventType.Release) {
                    pressPos = null
                    dragStarted = false
                }
                .padding(cardPadding)
        ) {
            drawIntoCanvas { canvas ->
                val root = previewRuntime.root
                canvas.skiaCanvas.save()
                canvas.skiaCanvas.scale(PREVIEW_SCALE, PREVIEW_SCALE)
                root.render(RenderContext(canvas.skiaCanvas))
                canvas.skiaCanvas.restore()
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
            Box(modifier = Modifier.weight(1f)) {
                if (!isFocused && searchText.isEmpty())
                    Text("Search ...", color = iconColor, fontSize = 12.sp)
                innerTextField()
            }
            if (searchText.isNotEmpty()) {
                IconButton("close", modifier = Modifier.padding(end = 4.dp).size(16.dp)) {
                    searchText = ""
                }
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
            Box(modifier = Modifier.weight(1f)) {
                if (!isFocused && value.isEmpty())
                    Text("Search HUDs...", color = iconColor, fontSize = 12.sp)
                innerTextField()
            }
            if (value.isNotEmpty()) {
                IconButton("close", modifier = Modifier.padding(end = 4.dp).size(16.dp)) {
                    onValueChange("")
                }
            }
        }
    }
}
