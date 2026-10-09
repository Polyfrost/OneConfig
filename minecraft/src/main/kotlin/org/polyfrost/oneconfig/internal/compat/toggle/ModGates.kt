package org.polyfrost.oneconfig.internal.compat.toggle

import java.util.concurrent.ConcurrentHashMap
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.backend.Backend
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.ui.v1.ModToggle
import org.polyfrost.oneconfig.api.ui.v1.ModToggles
import org.polyfrost.oneconfig.internal.ui.api.PropertyModToggle

/**
 * Gives mods without a master switch of their own one on their mod card
 *
 * A gated mod is switched off in one or both of two ways. The mixins in `mixin.compat.toggle` sit
 * in the mod's own classes and check [ModToggles.isEnabled], and a [ConfigMask] swaps the options
 * the mod reads from its mixins for vanilla values. Mods that do ship a switch under another name
 * get it wired to the card instead
 */
internal object ModGates {
    private val LOGGER = LogManager.getLogger("OneConfig/ModToggles")

    /**
     * @param id the loader id of the mod which is also what its mixins pass to [ModToggles.isEnabled]
     * @param cards further mod card ids showing the same switch such as a renamed config file
     * @param mask the options to overwrite while the mod is off
     * @param saveKey the name the mod's config library saves the masked config under
     * @param onToggle run after the switch changed for mods that cache what they render
     * @param unless a class only an unrelated mod sharing the id has, which is then left alone
     */
    private class Gate(
        val id: String,
        val cards: List<String> = emptyList(),
        val mask: ConfigMask? = null,
        val saveKey: String? = null,
        val onToggle: ((Boolean) -> Unit)? = null,
        val unless: String? = null,
    )

    private fun tweaks(name: String) = "dev.microcontrollers.$name.config"

    private val gates = listOf(
        Gate(
            "overlaytweaks",
            mask = ConfigMask(
                "${tweaks("overlaytweaks")}.OverlayTweaksConfig", listOf("overlaytweaks.json"),
                mapOf(
                    "keepHand" to false, "removeWaterOverlay" to false, "removeFireOverlay" to false,
                    "removeSubmergedFov" to false, "removeItemTooltip" to false,
                    "handInvisibilityOpacity" to 0f, "fireOverlayHeight" to 0f, "customShieldHeight" to 0f,
                    "elderGuardianOpacity" to 55f, "customFireOverlayOpacity" to 89.9f,
                    "elderGuardianScale" to 1f, "freezingOpacity" to 100f, "netherPortalOpacity" to 100f,
                    "equipableOpacity" to 100f, "spyglassOpacity" to 100f, "suffocationOverlayBrightness" to 10f,
                    "itemCooldownColor" to 0x7FFFFFFF, "spyglassColor" to 0xFF000000.toInt(),
                    "removeWaterFov" to false, "classicDebugStyle" to false, "potionGlint" to false,
                    "disableHandViewSway" to false, "removeRecipeBookShift" to false, "clickOutOfContainers" to false,
                    "customVignetteDarkness" to false, "colorShieldCooldown" to false, "pumpkinOpacity" to 100f,
                    "customShieldOpacity" to 100f, "containerOpacity" to 81.56863f, "containerTextureOpacity" to 100f,
                    "heartDisplayType" to "DEFAULT", "subtitleColor" to 0xCC000000.toInt(),
                    "closedCaptionBackgroundColor" to 0xCC000000.toInt(),
                ),
            ),
        ),
        Gate(
            "rendertweaks", cards = listOf("rendertweaksv2"),
            mask = ConfigMask(
                "${tweaks("rendertweaks")}.RenderTweaksConfig", listOf("rendertweaksv2.json"),
                mapOf(
                    "disableLightning" to false, "disableFishingLine" to false,
                    "disableWorldBorderStationary" to false, "disableWorldBorderShrinking" to false,
                    "disableWorldBorderGrowing" to false, "fishingLineWidthMultiplier" to 1f,
                    "lightningColor" to 0x4C72727F, "fishingLineColor" to 0xFF000000.toInt(),
                    "worldBorderStationaryColor" to 0xFF20A0FF.toInt(),
                    "worldBorderShrinkingColor" to 0xFFFF3030.toInt(),
                    "worldBorderGrowingColor" to 0xFF40FF80.toInt(),
                    "lightningChroma" to false, "fishingLineChroma" to false, "worldBorderStationaryChroma" to false,
                    "worldBorderShrinkingChroma" to false, "worldBorderGrowingChroma" to false,
                    "lightningAlpha" to 76f, "fishingLineAlpha" to 255f, "worldBorderStationaryAlpha" to 255f,
                    "worldBorderShrinkingAlpha" to 255f, "worldBorderGrowingAlpha" to 255f,
                ),
            ),
        ),
        Gate(
            "droppeditemtweaks", cards = listOf("droppeditemtweaksv2"),
            mask = ConfigMask(
                "${tweaks("droppeditemtweaks")}.DroppedItemTweaksConfig", listOf("droppeditemtweaksv2.json"),
                mapOf("staticItems" to false, "dropStackCount" to 0, "itemScale" to 1f, "uhcOverlay" to 0f),
            ),
        ),
        Gate(
            "shaketweaks",
            mask = ConfigMask(
                "${tweaks("shaketweaks")}.ShakeTweaksConfig", listOf("shaketweaks.json"),
                listOf(
                    "disableScreenBobbing", "disableHandBobbing", "disableHorizontalBobbing", "disableMapBobbing",
                    "disableHandDamage", "disableScreenDamage", "disableHandViewSway",
                ).associateWith { false },
            ),
        ),
        Gate(
            "scrolltweaks",
            mask = ConfigMask(
                "${tweaks("scrolltweaks")}.ScrollTweaksConfig", listOf("scrolltweaks.json"),
                mapOf("disableScroll" to false, "reverseScroll" to false, "preventOverflowScroll" to false),
            ),
        ),
        Gate(
            "crosshairtweaks",
            mask = ConfigMask(
                "${tweaks("crosshairtweaks")}.CrosshairTweaksConfig", listOf("crosshairtweaks.json"),
                listOf(
                    "disableCrosshair", "hideCrosshairInContainers", "showCrosshairInPerspective",
                    "hideCrosshairInPerspective", "showCrosshairInSpectator", "removeCrosshairBlendingFirstPerson",
                    "removeCrosshairBlendingThirdPerson", "disableAttackIndicator",
                    "removeAttackIndicatorBlendingFirstPerson", "removeAttackIndicatorBlendingThirdPerson",
                    "debugCrosshairAttackIndicator", "removeCrosshairBlending", "removeAttackIndicatorBlending",
                    "useNormalCrosshair", "useDebugCrosshair", "fixDebugCooldown",
                ).associateWith { false } + listOf(
                    "crosshairOpacity", "attackIndicatorOpacity",
                    "crosshairOpacityFirstPerson", "crosshairOpacityThirdPerson",
                    "attackIndicatorOpacityFirstPerson", "attackIndicatorOpacityThirdPerson",
                ).associateWith { 100f } + ("debugCrosshairSize" to 10),
            ),
        ),
        Gate(
            "betterscreens",
            mask = ConfigMask(
                "${tweaks("betterscreens")}.BetterScreensConfig", listOf("betterscreens.json"),
                mapOf(
                    "preventClosingScreens" to false, "dontResetCursor" to false, "replaceOpenToLan" to false,
                    "removeRecipeBookShift" to false, "removeStatusEffectsShift" to false, "showServerPreview" to false,
                    "clickOutOfContainers" to false, "containerBackgroundOpacity" to 81.56863f,
                    "containerTextureOpacity" to 100f, "containerLabelColor" to 0xFF404040.toInt(),
                    "playerInventoryLabelColor" to 0xFF404040.toInt(), "containerScale" to -1,
                ),
            ),
        ),
        Gate(
            "mountopacity",
            mask = ConfigMask(
                "${tweaks("mountopacity")}.MountOpacityConfig", listOf("mountopacity.json"),
                listOf(
                    "horseOpacity", "pigOpacity", "llamaOpacity", "striderOpacity", "camelOpacity",
                    "nautilusOpacity", "happyGhastOpacity", "defaultOpacity",
                ).associateWith { 100f } + mapOf("onlyInFirstPerson" to false, "useLookDirection" to false),
            ),
        ),
        Gate(
            "betternightvision",
            mask = ConfigMask(
                "${tweaks("betternightvision")}.BetterNightVisionConfig", listOf("betternightvision.json"),
                mapOf("disableNightVision" to false, "cleanerNightVision" to false),
            ),
        ),
        Gate(
            "sciophobia",
            mask = ConfigMask(
                "${tweaks("sciophobia")}.SciophobiaConfig", listOf("sciophobia.json"),
                mapOf(
                    "removeShadows" to false, "forceShadows" to false, "alternateShadows" to false,
                    "fullShadow" to false,
                ),
            ),
        ),
        Gate(
            "simpleblockoverlay",
            mask = ConfigMask(
                "${tweaks("simpleblockoverlay")}.SimpleBlockOverlayConfig", listOf("simpleblockoverlay.json"),
                mapOf(
                    "useVanillaOverlay" to true, "alwaysRenderOutline" to false, "hideBarrierBlocks" to false,
                    "hideTallGrass" to false, "renderInAdventure" to false, "renderInSpectator" to false,
                    "renderInHiddenHud" to false,
                    "disableMod" to true, "customRenderer" to false, "disableOutline" to false,
                    "disableOverlay" to true, "chroma" to false, "disableDepth" to false,
                    "solidColor" to 0x66000000,
                ),
            ),
        ),
        Gate(
            "vignettetweaks",
            mask = ConfigMask(
                "org.codeberg.chromatic.vignettetweaks.config.VignetteConfig", listOf("vignettetweaks.json"),
                mapOf("type" to 1, "strengthMultiplier" to 1f, "minimumStrength" to 0f, "maximumStrength" to 100f),
            ),
        ),
        Gate(
            "chattweaks",
            mask = ConfigMask(
                "org.polyfrost.chattweaks.config.ChatTweaksConfig", listOf("chattweaks.json"),
                listOf(
                    "removeBlankMessages", "dontClearChatHistory", "saveChatHistory", "sessionMarkers",
                    "compactChat", "consecutiveCompactChat", "dontCompactScreenshots", "timestamps",
                    "onlyNewTimestamps", "secondsOnTimestamps", "shiftChat", "bypassCommandLimit",
                    "fuzzyAutocomplete", "safeChatClicks", "safeChatClicksHistory", "imagePreview",
                ).associateWith { false } + ("increaseChatHistoryLimit" to 100),
            ),
        ),
        Gate(
            "blur", saveKey = "blur",
            // LegacyGuiBlur on 1.8.9 shares the id and has an enabled option of its own
            unless = "eu.midnightdust.blur.BlurConfig",
            mask = ConfigMask(
                "eu.midnightdust.blur.config.BlurConfig",
                values = mapOf("useGradient" to false, "blurContainers" to false, "showScreenID" to false),
            ),
        ),
        Gate(
            "effecttimerplus", saveKey = "effecttimerplus",
            mask = ConfigMask(
                "dev.terminalmc.effecttimerplus.config.Config",
                values = mapOf("scale" to 1.0, "potencyEnabled" to false, "timerEnabled" to false),
            ),
        ),
        Gate(
            "shulkerboxtooltip",
            // left as they are the mod would still hide the vanilla contents tooltip
            mask = ConfigMask(
                "com.misterpemodder.shulkerboxtooltip.ShulkerBoxTooltip",
                values = mapOf("config.tooltip.type" to "VANILLA", "config.tooltip.hideShulkerBoxLore" to false),
            ),
        ),
        Gate("lambdabettergrass", onToggle = { rebuildChunks() }),
        Gate("gammautils", onToggle = {
            callStatic("io.github.sjouwer.gammautils.statuseffect.StatusEffectManager", "updateAllEffects")
        }),
        Gate("controlify", onToggle = { enabled ->
            val controlify = callStatic("dev.isxander.controlify.Controlify", "instance")
            if (enabled && controlify != null) {
                controlify.javaClass.getMethod("applyControllerSelection", Boolean::class.javaPrimitiveType)
                    .invoke(controlify, true)
            }
        }),
        Gate("tooltipscroll", onToggle = { callStatic("com.provismet.tooltipscroll.ScrollTracker", "reset") }),
        Gate(
            "custom-block-highlight",
            // the 1.8.9 build hides the vanilla outline from a mixin of its own that reads this option
            mask = ConfigMask(
                "tektonikal.customblockhighlight.config.CBHOneConfig", listOf("custom-block-highlight.json"),
                mapOf("globalModToggle" to false, "vanillaOutline" to true),
            ),
        ),
        Gate("wwaypoints"),
        Gate("appleskin"),
        Gate("detailabreconst"),
        Gate("status-effect-bars"),
        Gate("waveycapes"),
        Gate("zoomify"),
        Gate("overflowparticles"),
        Gate("chatting"),
        Gate("redaction"),
        Gate("fovchanger"),
        Gate("chatblock"),
        Gate("hymod"),
        Gate(
            "legacyskyblock",
            // what builds before 2.0 do without checking that the player is on SkyBlock
            mask = ConfigMask(
                "tomeko.legacyskyblock.config.LegacySkyblockConfig", listOf("legacyskyblock.json"),
                listOf(
                    "hideGuildMOTDEnabled", "healthVignetteEnabled", "healthVignetteWorkOutsideSkyblock",
                    "toggleSprintEnabled", "hideAutoTipMessagesEnabled", "autoTipEnabled",
                    "autoCopyScreenshotEnabled", "modifyScreenshotMessageAddName", "modifyScreenshotMessageAddCopy",
                    "modifyScreenshotMessageAddOpen", "modifyScreenshotMessageAddOpenFolder",
                    "modifyScreenshotMessageAddDelete",
                ).associateWith { false } + ("customChatMessagesToHide" to emptyList<String>()),
            ),
            onToggle = { enabled -> if (!enabled) forgetLegacySkyblockLocation() },
        ),
        Gate("skyblockpv"),
        Gate("skyblock-item-list"),
        Gate("bobby"),
        Gate("presencefootsteps"),
        Gate("jade"),
        Gate("iconographic"),
        Gate("customscoreboard"),
        Gate("viewmodel"),
        Gate("flashback"),
        Gate("skyblocker"),
        Gate("freelook"),
        Gate(
            "screenshotmessageenhancer",
            // 2.x replaces the vanilla screenshot message whatever its options say, so this only strips the extras
            mask = ConfigMask(
                "tomeko.screenshotmessageenhancer.config.ScreenshotMessageEnhancerConfig",
                listOf("screenshotmessageenhancer.json"),
                listOf(
                    "modifyScreenshotMessageEnabled", "showCopyButton", "showOpenButton", "showOpenFolderButton",
                    "showDeleteButton", "showUploadButton", "autoCopyScreenshot", "compressScreenshots",
                ).associateWith { false } + ("showName" to true),
            ),
        ),
        Gate(
            "overflowanimations",
            // only the 1.8.9 build needs this, later ones have a switch that is wired up instead
            mask = ConfigMask(
                "org.polyfrost.overflowanimations.config.OverflowAnimationsConfig", listOf("overflowanimations.json"),
                mapOf(
                    "capeMovement" to "V1_12", "sneakBobbing" to "VANILLA", "voidFog" to "OFF",
                    "damageTintStyle" to "VANILLA",
                ) + listOf(
                    "sneakAnimation", "sneakEyeHeight", "backwardsWalking", "headRotationInterpolation", "damageTilt",
                    "slimArmPosition", "itemGlint", "armorGlint", "potionGlint", "fishingRodVersion", "dropSwing",
                    "usingTextureInGUI", "equipAnimationVersion", "cameraVersion", "thirdPersonCrosshair",
                    "inventoryEffects", "debugCrosshairStyle", "tabListStyle", "viewBobbingTilt", "useEquipAnimation",
                    "blockMiningProgress",
                ).associateWith { "VANILLA|V1_8" } + listOf(
                    "longUnsneak", "itemPositions", "itemPositionsInThirdPerson", "itemUsageSwinging",
                    "usageSwingingParticles", "fakeMissPenaltySwing", "fakeMissPenaltyParticles", "blockHitWhileMining",
                    "modernPotionColors", "fishingRodLineFov", "fixItemUsageVisualInGUI", "damageTintArmor",
                    "thirdPersonSwordBlockingPosition", "capeSneakPosition", "offsetHurtTiltTime", "disableHurtCamera",
                    "dinnerboneMode", "dinnerboneModeEntities", "wavyArms", "itemDropsFaceCamera",
                    "itemDropsFaceCameraRotationFix", "itemDrops2D", "itemFramed2D", "thinBlockPositions",
                    "thinFishingRodLineThickness", "itemPickupPosition", "mobHeadIcons", "eggSnowballParticles",
                    "customSwingSpeed", "ignoreHasteSpeed", "ignoreMiningFatigueSpeed", "alwaysUsageSwing",
                    "disableSwingTranslate", "disableSwingPivot", "itemDrops2DColors", "legacyProjectiles",
                    "xpOrbPosition", "fireballModel", "disableItemPickupAnimation", "disableHandSway",
                    "smartSwingScaling", "disableDropSwingInContainers", "resetMiningOnUse", "disableAdventureSwing",
                    "disableAdventureUsageSwinging", "disableAdventureUsageParticles", "lunarBlockHitPosition",
                    "lunarItemPositions", "coloredPotionBottles", "customRodLine", "scaleConsumeWithItem",
                    "disableHeartFlash", "centerScrollableListWidgets", "disableDebugHudBackground",
                    "debugHudTextShadow", "disconnectServerToTitleScreen", "legacyDebugScreen", "planarSkyFog",
                    "glintAffectsArmorTint", "damageTintItems", "damageTintCape", "maxGlintProperties", "flameOffset",
                    "persistentBlockOutline", "fastGrass", "disableRandomBlockRotations",
                ).associateWith { false } + listOf(
                    "itemSwingSpeed", "hasteSwingSpeed", "miningFatigueSwingSpeed", "itemPickupOffset",
                    "fishingRodLineThickness", "consumeScale", "consumeIntensity", "consumeSpeed", "blockingScale",
                    "droppedScale", "projectileScale", "fireballScale",
                ).associateWith { 0f } + listOf(
                    "itemOffset", "itemRotation", "rodLinePosition", "swingPosition", "consumePosition",
                    "consumeRotation", "blockingPosition", "blockingRotation", "droppedPosition", "droppedRotation",
                    "projectilePosition", "projectileRotation", "fireballPosition", "fireballRotation",
                ).flatMap { name -> listOf("X", "Y", "Z").map { "$name$it" to 0f } } + mapOf(
                    "itemScaleX" to 1f, "itemScaleY" to 1f, "itemScaleZ" to 1f, "reequipSpeed" to 0.4f,
                ),
            ),
        ),
        Gate("betterhurtcam", cards = listOf("betterhurtcam.toml")),
        Gate("hybedwars"),
        Gate("hybridge"),
        Gate("hychatter"),
        Gate("hyinfo"),
        Gate("hylobby"),
        // its config file kept the name of the mod it replaces
        Gate("polyzoom", cards = listOf("zoomify")),
    )

    private val byId = gates.associateBy { it.id }

    private val ownSwitches: Map<String, () -> ModToggle?> = mapOf(
        "simplenickhider" to { optionToggle("${tweaks("simplenickhider")}.SimpleNickHiderConfig", "enable") },
        "numericalenchantments" to {
            optionToggle("${tweaks("numericalenchantments")}.NumericalEnchantmentsConfig", "enabled")
        },
        "confirmdisconnect" to {
            optionToggle("${tweaks("confirmdisconnect")}.ConfirmDisconnectConfig", "confirmEnabled")
        },
        "voicechat" to { VoiceChatToggle.takeIf { it.available } },
        "animatium" to {
            SelfSwitchingMod("org.visuals.legacy.animatium.Animatium", "btw.mixces.animatium.AnimatiumClient")
                .takeIf { it.available }
        },
        "overflowanimations" to {
            SelfSwitchingMod("org.polyfrost.overflowanimations.OverflowAnimations").takeIf { it.available }
        },
    )

    @JvmStatic
    fun register() {
        val loaded = ModInfo.loadedMods.mapTo(HashSet()) { it.id }

        for ((id, create) in ownSwitches) {
            if (id !in loaded) continue
            try {
                create()?.let { ModToggles.register(id, it) }
            } catch (t: Throwable) {
                LOGGER.error("Failed to wire up the switch of {}", id, t)
            }
        }

        val present = gates.filter { it.id in loaded && (it.unless == null || !classExists(it.unless)) }
        for (gate in present) {
            ModToggles.manage(gate.id)
            val toggle = ModToggles.toggleFor(gate.id) ?: continue
            gate.cards.forEach { ModToggles.register(it, toggle) }
            if (!ModToggles.isEnabled(gate.id)) {
                gate.mask?.apply()
                // resources and chunks were built before the mask went on
                runCatching { gate.onToggle?.invoke(false) }
            }
        }

        ModToggles.addListener { id, enabled ->
            val gate = byId[id] ?: return@addListener
            if (enabled) gate.mask?.remove() else gate.mask?.apply()
            try {
                gate.onToggle?.invoke(enabled)
            } catch (t: Throwable) {
                LOGGER.error("Failed to refresh {} after it was switched {}", id, if (enabled) "on" else "off", t)
            }
        }

        ConfigManager.addProfileChangeListener(object : ConfigManager.ProfileChangeListener {
            override fun onProfileSaving(profile: String) {
                present.forEach { it.mask?.remove() }
            }

            override fun onProfileChanged(newProfile: String) {
                present.forEach { if (!ModToggles.isEnabled(it.id)) it.mask?.apply() }
            }
        })
    }

    @JvmStatic
    fun blocksYaclSave(handler: Any): Boolean = try {
        val type = handler.javaClass.getMethod("configClass").invoke(handler) as? Class<*>
        type != null && gates.any { it.mask?.masks(type.name) == true }
    } catch (_: Throwable) {
        false
    }

    @JvmStatic
    fun blocksSave(saveKey: String?): Boolean =
        gates.any { it.saveKey == saveKey && it.mask?.applied == true }

    /** Puts the mask back after [configClassName] was reloaded from disk underneath it */
    @JvmStatic
    fun remask(configClassName: String) {
        gates.forEach { gate -> gate.mask?.takeIf { it.masks(configClassName) }?.reapply() }
    }

    private val enumConstants = ConcurrentHashMap<String, Any>()

    @JvmStatic
    fun enumConstant(className: String, name: String): Any? = enumConstants["$className.$name"] ?: try {
        Class.forName(className, true, ModGates::class.java.classLoader).enumConstants
            ?.firstOrNull { (it as Enum<*>).name == name }
            ?.also { enumConstants["$className.$name"] = it }
    } catch (_: Throwable) {
        null
    }

    private val zoomifyState by lazy {
        try {
            val type = Class.forName("dev.isxander.zoomify.Zoomify", true, ModGates::class.java.classLoader)
            listOf("zooming" to false, "secondaryZooming" to false, "previousZoomDivisor" to 1.0).map { (name, value) ->
                type.getDeclaredField(name).also { it.isAccessible = true } to value
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    @JvmStatic
    fun resetZoomify() {
        try {
            zoomifyState.forEach { (field, value) -> field.set(null, value) }
        } catch (_: Throwable) {
        }
    }

    private val legacySkyblockLocation by lazy {
        runCatching {
            val type = Class.forName("tomeko.legacyskyblock.utils.HypixelPackets", true, ModGates::class.java.classLoader)
            listOf("onHypixel", "inSkyblock", "inDungeons").map(type::getField)
        }.getOrDefault(emptyList())
    }

    @JvmStatic
    fun forgetLegacySkyblockLocation() {
        try {
            legacySkyblockLocation.forEach { it.setBoolean(null, false) }
        } catch (_: Throwable) {
        }
    }

    private val freelook by lazy {
        runCatching {
            val type = Class.forName(
                "org.codeberg.chromatic.freelook.handler.FreelookHandler", true, ModGates::class.java.classLoader,
            )
            type.getDeclaredField("freelookToggled").also { it.isAccessible = true } to type.getMethod("stop")
        }.getOrNull()
    }

    /** Hands the camera back when Freelook is switched off mid-look */
    @JvmStatic
    fun stopFreelook(handler: Any) {
        try {
            val (looking, stop) = freelook ?: return
            if (looking.getBoolean(handler)) stop.invoke(handler)
        } catch (_: Throwable) {
        }
    }

    private fun classExists(name: String): Boolean = runCatching {
        Class.forName(name, false, ModGates::class.java.classLoader)
    }.isSuccess

    private val polyZoomState by lazy {
        runCatching {
            val type = Class.forName("org.polyfrost.polyzoom.PolyZoom", true, ModGates::class.java.classLoader)
            listOf("zooming" to false, "secondaryZooming" to false, "scrollSteps" to 0).map { (name, value) ->
                type.getDeclaredField(name).also { it.isAccessible = true } to value
            }
        }.getOrDefault(emptyList())
    }

    @JvmStatic
    fun releasePolyZoom(polyZoom: Any) {
        try {
            polyZoomState.forEach { (field, value) -> field.set(polyZoom, value) }
        } catch (_: Throwable) {
        }
    }

    private val tipperEnabled by lazy {
        runCatching {
            val type = Class.forName("org.codeberg.awruff.tipper.config.TipperConfig", true, ModGates::class.java.classLoader)
            type.getField("INSTANCE").get(null) to type.getMethod("getEnabled")
        }.getOrNull()
    }

    @JvmStatic
    fun tipperDisabled(): Boolean = try {
        tipperEnabled?.let { (config, enabled) -> enabled.invoke(config) == false } ?: false
    } catch (_: Throwable) {
        false
    }

    @JvmStatic
    fun dropController(controlify: Any) {
        try {
            val current = controlify.javaClass.getMethod("getCurrentController").invoke(controlify) as? java.util.Optional<*>
            if (current?.isPresent != true) return
            controlify.javaClass.methods
                .first { it.name == "setCurrentController" && it.parameterCount == 2 }
                .invoke(controlify, null, true)
        } catch (_: Throwable) {
        }
    }

    @JvmStatic
    fun waveyCapesVanillaLayer(): Any? = try {
        val (instance, capeLayer) = waveyCapes ?: return null
        instance.get(null)?.let(capeLayer::invoke)
    } catch (_: Throwable) {
        null
    }

    private val waveyCapes by lazy {
        runCatching {
            val base = Class.forName("dev.tr7zw.waveycapes.WaveyCapesBase", true, ModGates::class.java.classLoader)
            base.getField("INSTANCE") to base.getMethod("getCapeLayer")
        }.getOrNull()
    }

    private fun callStatic(className: String, method: String): Any? =
        Class.forName(className, true, ModGates::class.java.classLoader).getMethod(method).invoke(null)

    private fun rebuildChunks() {
        //? if >= 26.2 {
        net.minecraft.client.Minecraft.getInstance().levelExtractor.allChanged()
        //?} elif > 1.8.9 {
        /*net.minecraft.client.Minecraft.getInstance().levelRenderer.allChanged()
        *///?}
    }

    private fun optionToggle(className: String, option: String): ModToggle? {
        val id = className.substringBeforeLast(".config.").substringAfterLast('.')
        val tree = ConfigManager.active().trees().firstOrNull {
            it.id == "$id.json" && it.getMetadata<Any>(Backend.UI_ONLY_METADATA) != true
        }
        @Suppress("UNCHECKED_CAST")
        (tree?.getProp(option) as? Property<Boolean>)?.let { return PropertyModToggle(it) }

        val type = Class.forName(className, true, ModGates::class.java.classLoader)
        val handler = type.getField("CONFIG").get(null)
        val field = type.getField(option)
        val instance = handler.javaClass.getMethod("instance")
        val save = handler.javaClass.getMethod("save")
        return object : ModToggle {
            override fun isEnabled() = field.getBoolean(instance.invoke(handler))

            override fun setEnabled(enabled: Boolean) {
                field.setBoolean(instance.invoke(handler), enabled)
                save.invoke(handler)
            }
        }
    }

    private object VoiceChatToggle : ModToggle {
        private val manager by lazy {
            runCatching {
                Class.forName("de.maxhenkel.voicechat.voice.client.ClientManager", true, ModGates::class.java.classLoader)
                    .getMethod("getPlayerStateManager")
            }.getOrNull()
        }

        val available get() = manager != null

        private fun state() = manager?.invoke(null)

        private val onboarding by lazy {
            runCatching {
                Class.forName(
                    "de.maxhenkel.voicechat.gui.onboarding.OnboardingManager", true, ModGates::class.java.classLoader,
                ).getMethod("isOnboarding")
            }.getOrNull()
        }

        override fun needsSetup(): Boolean = try {
            onboarding?.invoke(null) == true
        } catch (_: Throwable) {
            false
        }

        override fun isEnabled(): Boolean = try {
            val state = state()
            state == null || state.javaClass.getMethod("isDisabled").invoke(state) != true
        } catch (_: Throwable) {
            true
        }

        override fun setEnabled(enabled: Boolean) {
            try {
                val state = state() ?: return
                state.javaClass.getMethod("setDisabled", Boolean::class.javaPrimitiveType).invoke(state, !enabled)
            } catch (t: Throwable) {
                LOGGER.error("Failed to switch Simple Voice Chat", t)
            }
        }
    }

    /** A mod with static `isEnabled` and `setEnabled` of its own, the way Animatium and its ports have */
    private class SelfSwitchingMod(typeName: String, legacyTypeName: String? = null) : ModToggle {
        private fun find(name: String?) = runCatching {
            Class.forName(name, true, ModGates::class.java.classLoader)
        }.getOrNull()

        private val type by lazy { find(typeName)?.takeIf { runCatching { it.getMethod("isEnabled") }.isSuccess } }

        private val legacy by lazy { find(legacyTypeName) }

        val available get() = type != null || legacy != null

        override fun isEnabled(): Boolean = try {
            (type ?: legacy)?.getMethod("isEnabled")?.invoke(null) != false
        } catch (_: Throwable) {
            true
        }

        override fun setEnabled(enabled: Boolean) {
            try {
                val type = type
                if (type != null) {
                    type.getMethod("setEnabled", Boolean::class.javaPrimitiveType).invoke(null, enabled)
                } else {
                    val legacy = legacy ?: return
                    legacy.getField("ENABLED").setBoolean(null, enabled)
                    legacy.getMethod("saveEnabledState").invoke(null)
                }
            } catch (t: Throwable) {
                LOGGER.error("Failed to switch {}", (type ?: legacy)?.simpleName, t)
                return
            }
            val reload = runCatching { type?.getMethod("reload") }.getOrNull()
            if (reload != null) runCatching { reload.invoke(null) } else runCatching { rebuildChunks() }
        }
    }
}
