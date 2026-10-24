package org.polyfrost.compose.foundation.lazy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import org.polyfrost.compose.foundation.ScrollState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.verticalScroll
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.layout.layoutId
import org.polyfrost.compose.ui.layout.onPlaced
import org.polyfrost.compose.layout.LayoutType
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.node.requestLayout
import org.polyfrost.compose.runtime.PolyApplier

class LazyListState {
    val scroll = ScrollState()

    var firstIndex by mutableIntStateOf(0)
        private set
    var lastIndex by mutableIntStateOf(INITIAL_ITEMS - 1)
        private set

    private val heights = HashMap<Any, Float>()
    private var estimate = DEFAULT_ESTIMATE
    internal var keys: List<Any> = emptyList()
    internal var gap = 0f

    internal var composedFirst = 0
    internal var composedLast = -1

    private fun heightAt(i: Int) = heights[keys[i]] ?: estimate

    internal fun spacer(before: Boolean): Float {
        val range = if (before) 0 until composedFirst else composedLast + 1 until keys.size
        if (range.isEmpty()) return 0f
        var h = 0f
        for (i in range) h += heightAt(i) + gap
        return h - gap
    }

    internal fun placed(list: PolyNode) {
        for (c in list.children) {
            val k = (c.style.tag as? LazyItem)?.key ?: continue
            heights[k] = c.height
        }
        if (heights.isNotEmpty()) estimate = heights.values.average().toFloat()
        val n = keys.size
        if (n == 0) return
        val top = scroll.value
        val viewport = list.height
        val from = top - viewport
        val to = top + viewport * 2f
        var y = list.style.padding.top
        var first = n - 1
        var i = 0
        while (i < n) {
            val h = heightAt(i)
            if (y + h >= from) { first = i; break }
            y += h + gap
            i++
        }
        var last = first
        while (last + 1 < n && y + heightAt(last) + gap <= to) {
            y += heightAt(last) + gap
            last++
        }
        if (first != firstIndex) firstIndex = first
        if (last != lastIndex) lastIndex = last
        for (c in list.children) if (c is LazySpacerNode && c.contentHeight != spacer(c.before)) list.requestLayout()
    }

    private companion object {
        const val INITIAL_ITEMS = 24
        const val DEFAULT_ESTIMATE = 48f
    }
}

internal class LazyItem(val key: Any)

private class LazySpacerNode(var state: LazyListState, val before: Boolean) : PolyNode() {
    override fun measureContent(maxWidth: Float, maxHeight: Float) {
        contentHeight = state.spacer(before)
    }
}

@Composable
fun rememberLazyListState() = remember { LazyListState() }

interface LazyItemScope

private object LazyItemScopeInstance : LazyItemScope

class LazyListScope internal constructor() {
    internal val keys = ArrayList<Any>()
    internal val contents = ArrayList<@Composable LazyItemScope.() -> Unit>()

    fun item(key: Any? = null, content: @Composable LazyItemScope.() -> Unit) {
        keys.add(key ?: DefaultKey(keys.size))
        contents.add(content)
    }

    fun <T> items(items: List<T>, key: ((T) -> Any)? = null, itemContent: @Composable LazyItemScope.(item: T) -> Unit) {
        for (item in items) item(key?.invoke(item)) { itemContent(item) }
    }

    fun <T> itemsIndexed(items: List<T>, key: ((index: Int, item: T) -> Any)? = null, itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit) {
        for ((i, item) in items.withIndex()) item(key?.invoke(i, item)) { itemContent(i, item) }
    }

    fun items(count: Int, key: ((index: Int) -> Any)? = null, itemContent: @Composable LazyItemScope.(index: Int) -> Unit) {
        for (i in 0 until count) item(key?.invoke(i)) { itemContent(i) }
    }

    private data class DefaultKey(val index: Int)
}

@Composable
fun LazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit,
) {
    val scope = LazyListScope().apply(content)
    val count = scope.keys.size
    state.keys = scope.keys
    state.gap = verticalArrangement.spacing.value
    val first = state.firstIndex.coerceIn(0, maxOf(0, count - 1))
    val last = minOf(state.lastIndex, count - 1)
    state.composedFirst = first
    state.composedLast = last
    Column(modifier.verticalScroll(state.scroll).onPlaced(state::placed), verticalArrangement = verticalArrangement) {
        if (first > 0) LazySpacer(state, true)
        for (i in first..last) {
            val k = scope.keys[i]
            val itemContent = scope.contents[i]
            key(k) {
                Box(Modifier.fillMaxWidth().layoutId(LazyItem(k))) { LazyItemScopeInstance.itemContent() }
            }
        }
        if (last < count - 1) LazySpacer(state, false)
    }
}

@Composable
private fun LazySpacer(state: LazyListState, before: Boolean) {
    ComposeNode<LazySpacerNode, PolyApplier>(
        factory = { LazySpacerNode(state, before).also { it.style.layoutType = LayoutType.Box } },
        update = { set(state) { this.state = it } },
    )
}
