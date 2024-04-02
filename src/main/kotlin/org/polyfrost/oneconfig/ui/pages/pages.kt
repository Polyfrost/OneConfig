package org.polyfrost.oneconfig.ui.pages

import org.polyfrost.oneconfig.api.config.ConfigVisualizer
import org.polyfrost.oneconfig.api.config.Tree
import org.polyfrost.oneconfig.internal.News
import org.polyfrost.oneconfig.internal.Notification
import org.polyfrost.oneconfig.ui.OneConfigUI
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.component.*
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Group
import org.polyfrost.polyui.component.impl.Image
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.Fade
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.image
import org.polyfrost.polyui.utils.radii
import org.polyfrost.polyui.utils.translated

private val heart = PolyImage("plus.svg")
private val defaultModImage = "chatting.svg".image()
private val modBoxTopRad = radii(8f, 8f, 0f, 0f)
private val modBoxBotRad = radii(0f, 0f, 8f, 8f)
private val modBoxAlign = Align(cross = Align.Cross.Start, mode = Align.Mode.Vertical, padding = Vec2.ZERO)
private val imageAlign = Align(main = Align.Main.Center, padding = Vec2.ZERO)
private val barAlign = Align(padding = Vec2(14f, 6f), main = Align.Main.SpaceBetween)

fun ModsPage(trees: Collection<Tree>): Drawable {
    // todo add categories
    return Group(
        size = Vec2(1130f, 0f),
        visibleSize = Vec2(1130f, 635f),
        alignment = Align(cross = Align.Cross.Start, padding = Vec2(18f, 18f)),
        children = trees.map {
            Group(
                alignment = modBoxAlign,
                children = arrayOf(
                    Block(
                        radii = modBoxTopRad,
                        alignment = imageAlign,
                        size = Vec2(256f, 104f),
                        children = arrayOf(Image(defaultModImage)),
                    ).withStates(),
                    Block(
                        radii = modBoxBotRad,
                        alignment = barAlign,
                        size = Vec2(256f, 36f),
                        children =
                        arrayOf(
                            Text("Chatting", font = PolyUI.defaultFonts.medium, fontSize = 14f),
                            Image(heart),
                        ),
                    ).setPalette { brand.fg },
                ),
            ).events {
                Event.Mouse.Clicked(0) then { _ ->
                    OneConfigUI.openPage(ConfigVisualizer.get(it), (this[1][0] as Text).text)
                }
            }.namedId("ModCard")
        }.toTypedArray(),
    ).namedId("ModsPage")
}

fun ThemesPage(): Drawable {
    return Group()
}

fun FeedbackPage(): Drawable {
    return Group(
        Image(PolyImage("polyfrost.png")).onInit { image.size.min(298f, 50f) },
        Text("oneconfig.feedback.title", fontSize = 24f, font = PolyUI.defaultFonts.medium),
        Text("oneconfig.feedback.credits", fontSize = 14f),
        Text("oneconfig.feedback.bugreport", fontSize = 24f, font = PolyUI.defaultFonts.medium),
        Text("oneconfig.feedback.joindiscord", fontSize = 14f),
        size = Vec2(1130f, 0f),
        visibleSize = Vec2(1130f, 635f),
        alignment = Align(cross = Align.Cross.Start, mode = Align.Mode.Vertical, padding = Vec2(18f, 18f)),
    )
}

fun ProfilesPage(): Drawable {
    return Group()
}

fun ChangelogPage(news: Collection<News>): Drawable {
    return Group(
        size = Vec2(1130f, 0f),
        visibleSize = Vec2(1130f, 635f),
        alignment = Align(cross = Align.Cross.Center, padding = Vec2(60f, 20f)),
        children = news.map {
            Group(
                if (it.image != null) Image(it.image).onInit { image.size.max(325f, 111f) } else null,
                Group(
                    Text(it.title, font = PolyUI.defaultFonts.medium, fontSize = 16f),
                    Text(it.summary, visibleSize = Vec2(612f, 166f)),
                    Group(
                        Text(it.dateString),
                        Text("oneconfig.readmore").withStates().events {
                            Event.Mouse.Clicked(0) then { _ ->
                                val page =
                                    Group(
                                        if (it.image != null) Image(it.image).onInit { image.size.max(325f, 111f) } else null,
                                        Group(
                                            Text(it.title, font = PolyUI.defaultFonts.medium, fontSize = 24f),
                                            Text("oneconfig.writtenby".translated(it.author)),
                                            Text(it.dateString),
                                        ),
                                        Text(it.content, fontSize = 14f, visibleSize = Vec2(1100f, 0f)),
                                        alignment = Align(cross = Align.Cross.Start),
                                        size = Vec2(1130f, 0f),
                                        visibleSize = Vec2(1130f, 635f),
                                    )
                                OneConfigUI.openPage(page, it.title)
                                // todo switch
                            }
                        },
                        size = Vec2(612f, 12f),
                        alignment = Align(main = Align.Main.SpaceBetween),
                    ),
                    alignment = Align(mode = Align.Mode.Vertical),
                ),
            )
        }.toTypedArray(),
    )
}

fun NotificationsPopup(polyUI: PolyUI, notifications: List<Notification>) {
    val it = Block(
        focusable = true,
        visibleSize = Vec2(368f, 500f),
        size = Vec2(300f, 0f),
        children = arrayOf(
            Text("oneconfig.notifications", font = PolyUI.defaultFonts.medium, fontSize = 16f),
            Group(
                children = notifications.map {
                    Group(
                        Group(
                            Image(it.icon).onInit { image.size.resize(24f, 24f) },
                            Group(
                                Group(
                                    Text(it.title, font = PolyUI.defaultFonts.medium, fontSize = 14f),
                                    Text(it.timeString).setPalette { text.secondary },
                                ),
                                Text(it.description).setPalette { text.secondary },
                                alignment = Align(mode = Align.Mode.Vertical),
                            ),
                        ),
                        *it.extras,
                    )
                }.toTypedArray(),
            ),
            Group(
                Group(
                    Image("close.svg".image()),
                    Text("oneconfig.clearall"),
                ),
                Image("cog.svg".image()),
            ),
        ),
    ).events {
        Event.Focused.Lost then { _ ->
            Fade(this, 0f, false, Animations.EaseInOutQuad.create(0.2.seconds)) {
                parent!!.removeChild(this)
            }.add()
        }
    }
    it.setup(polyUI)
    it.x = polyUI.mouseX - it.width / 2f
    it.y = polyUI.mouseY + 10f
    it.alpha = 0f
    Fade(it, 1f, false, Animations.EaseInOutQuad.create(0.1.seconds)).add()
    polyUI.master.addChild(it)
    polyUI.focus(it)
}