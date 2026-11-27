package org.polyfrost.oneconfig.internal.compat

//? if > 1.8.9 {
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.SpriteIconButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.resources.Identifier
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen

object PauseMenuButton {
    private const val SIZE = 20
    private const val ICON = 16
    private const val GAP = 4

    private val sprite = Identifier.fromNamespaceAndPath("oneconfig", "pause_menu")

    @JvmStatic
    fun register() {
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen !is PauseScreen || !screen.showsPauseMenu()) return@register
            //? if >= 26.1 {
            val buttons = Screens.getWidgets(screen)
            //?} else
            //val buttons = Screens.getButtons(screen)
            val options = buttons.firstOrNull { it.isOptionsButton() } ?: return@register
            val title = Component.translatable("oneconfig.pause_menu.open")
            val button = SpriteIconButton.builder(title, { OneConfigUIScreen.openLastSession() }, true)
                .sprite(sprite, ICON, ICON)
                .size(SIZE, SIZE)
                .build()
            button.setPosition(options.x - SIZE - GAP, options.y)
            button.setTooltip(Tooltip.create(title))
            buttons.add(button)
        }
    }

    private fun AbstractWidget.isOptionsButton(): Boolean = (message.contents as? TranslatableContents)?.key == "menu.options"
}
//?}
