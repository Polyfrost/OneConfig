package org.polyfrost.oneconfig.internal.ui.api

import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.platform.v1.ModInfo

object ThirdPartyModCategories {
    // Seeded from Polyfrost/DataStorage OneClient packwiz bundles.
    private val hypixel = setOf(
        "catharsis",
        "customscoreboard",
        "firmament",
        "hotshirtlessmen",
        "modern-warp-menu",
        "modernwarpmenu",
        "rei",
        "rei-search-bar-calculations",
        "roughlyenoughitems",
        "skyblock-custom-scoreboard",
        "skyblock-profile-viewer",
        "skyblocker",
        "skyblocker-liap",
        "skyblockpv",
        "skycubed",
        "skyhanni",
        "skyocean",
    )

    private val combat = setOf(
        "effecttimerplus",
        "shaketweaks",
        "viewmodel",
    )

    private val performance = setOf(
        "asyncparticles",
        "badoptimizations",
        "c2me",
        "c2me-fabric",
        "debugify",
        "dynamic-fps",
        "dynamic_fps",
        "entityculling",
        "fastquit",
        "fastserverpings",
        "ferrite-core",
        "ferritecore",
        "immediatelyfast",
        "lithium",
        "modernfix",
        "modernfix-mvus",
        "noxesium",
        "rrls",
        "scalablelux",
        "sodium",
        "sodium-extra",
    )

    private val visuals = setOf(
        "animatica",
        "animatium",
        "better-block-entities",
        "better-elytra-render",
        "better-mipmaps",
        "better-selection",
        "betterblockentities",
        "blur",
        "continuity",
        "cull-fewer-leaves",
        "entity-model-features",
        "entity_model_features",
        "entity_texture_features",
        "entitytexturefeatures",
        "euphoria-patches",
        "iris",
        "lambdynamiclights",
        "lambdynlights",
        "modelfix",
        "optipainting",
        "particle_core",
        "polytone",
        "presence-footsteps",
        "presencefootsteps",
        "sciophobia",
        "skinlayers3d",
        "skyboxify",
        "smooth-skies",
        "smoothskies",
        "wavey-capes",
        "waveycapes",
    )

    private val hud = setOf(
        "appleskin",
        "betterstats",
        "bettermounthud",
        "centered-crosshair",
        "clean-debug",
        "detailab",
        "detailabreconst",
        "inventoryhud",
        "inventoryhudplus",
        "scoreboardtweaks",
        "status-effect-bars",
        "tabtweaks",
        "titletweaks",
        "ukus-armor-hud",
    )

    private val controls = setOf(
        "amecs",
        "amecs-reborn",
        "commandkeys",
        "controlify",
        "controlling",
        "crosshairtweaks",
        "fix-keyboard-on-linux",
        "gamma-utils",
        "gammautils",
        "scrolltweaks",
        "toggle-toggle-sprint",
        "zoomify",
    )

    private val chat = setOf(
        "autogg",
        "chat-heads",
        "chat-impressive-animation",
        "chat_heads",
        "chatimpressiveanimation",
        "hypixelautotip",
        "no-chat-reports",
        "nochatreports",
        "nochatrestrictions",
        "simple-voice-chat",
        "symbol-chat",
        "voicechat",
    )

    private val utility = setOf(
        "bettercommandblockui",
        "crash_assistant",
        "crashpatch",
        "e4mc",
        "firmamentpacketfix",
        "forge-config-api-port",
        "forgeconfigapiport",
        "ias",
        "in-game-account-switcher",
        "islandutils",
        "ixeris",
        "jade",
        "lazy-language-loader",
        "logcleaner",
        "mixintrace",
        "modmenu",
        "nbt-autocomplete",
        "nbt_ac",
        "one-click-join",
        "oneconfigbootstrap",
        "packmanager",
        "quick-pack",
        "quickjoin",
        "reeses-sodium-options",
        "resourcify",
        "respackopts",
        "screenshotclipboard",
        "screenshotcompression",
        "simple-modpack-update-checker",
        "spark",
        "spu",
        "stackdeobfuscator",
        "viafabricplus",
        "yacl",
        "yet_another_config_lib_v3",
        "yosbr",
    )

    private val qol = setOf(
        "better-screens",
        "better-stats",
        "betternarratorerror",
        "betternightvision",
        "betterscreens",
        "black-bar-concealer",
        "blackbarconcealer",
        "compacting",
        "confirmdisconnect",
        "confirmresetkeys",
        "countdown",
        "cubes-without-borders",
        "cwb",
        "damagetint",
        "dark-loading-screen",
        "darkgraph",
        "droppeditemtweaks",
        "entity_sound_features",
        "esf",
        "forcecloseworldloadingscreen",
        "hold_that_chunk_2",
        "idontwannascrollagain",
        "mountopacity",
        "nametagtweaks",
        "notsoessential",
        "numericalenchantments",
        "optigui",
        "overlaytweaks",
        "paginatedadvancements",
        "polytime",
        "polyweather",
        "rendertweaks",
        "reward-claim",
        "rewardclaim",
        "screencopy",
        "secureskins",
        "shulkerboxtooltip",
        "simple-block-overlay",
        "simpleblockoverlay",
        "simplenickhider",
        "slice",
        "smoothscrollingrefurbished",
        "sound-controller",
        "sound-physics-remastered",
        "sound_physics_perfected",
        "sound_physics_remastered",
        "soundcontroller",
        "text_tunnels",
        "tiptapshow",
        "tntcountdown",
        "tooltipscroll",
    )

    private val byId: Map<String, Config.Category> =
        hypixel.associateWith { Config.Category.HYPIXEL } +
            combat.associateWith { Config.Category.COMBAT } +
            performance.associateWith { Config.Category.PERFORMANCE } +
            visuals.associateWith { Config.Category.VISUALS } +
            hud.associateWith { Config.Category.HUD } +
            controls.associateWith { Config.Category.CONTROLS } +
            chat.associateWith { Config.Category.CHAT } +
            utility.associateWith { Config.Category.UTILITY } +
            qol.associateWith { Config.Category.QOL }

    fun categoryFor(id: String?, modInfo: ModInfo?): Config.Category? =
        sequenceOf(id, id?.removeSuffix(".json"), modInfo?.id, modInfo?.id?.removeSuffix(".json"))
            .filterNotNull()
            .map { it.lowercase() }
            .firstNotNullOfOrNull(byId::get)
}
